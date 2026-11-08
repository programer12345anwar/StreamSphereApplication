# StreamSphere system architecture

## Project at a glance

StreamSphere is a video-sharing application with React web clients and a
Java 17 / Spring Boot backend. The backend is split into services so identity
and catalog data, media upload operations, notification delivery, and browser
API routing have separate responsibilities.

The current repository contains a Gateway, Central service, Video service,
Notification service, and a Service Registry module. The Render Blueprint
deploys the first four; it does not currently provision the Service Registry.
PostgreSQL stores application records, Redis is used for Central's cache,
RabbitMQ carries asynchronous notification and thumbnail-processing messages,
and ImageKit stores new video media and serves it from a CDN.

## Logical topology

```mermaid
flowchart LR
    Browser[Web browser]
    Frontend[React frontend]
    Admin[React admin console]
    Gateway[API Gateway]
    Central[Central service]
    Video[Video service]
    Notify[Notification service]
    Postgres[(PostgreSQL)]
    Redis[(Redis cache)]
    Rabbit[(RabbitMQ)]
    ImageKit[ImageKit media CDN]
    SMTP[SMTP provider]

    Browser --> Frontend
    Browser --> Admin
    Frontend -->|JSON APIs and runtime config| Gateway
    Admin -->|JSON APIs and runtime config| Gateway
    Gateway --> Central
    Gateway --> Video
    Gateway --> Notify
    Central --> Postgres
    Central --> Redis
    Central -->|notification messages| Rabbit
    Rabbit --> Notify
    Central -->|thumbnail outbox request| Rabbit
    Rabbit -->|thumbnail request| Video
    Video -->|thumbnail result| Rabbit
    Rabbit -->|processing result| Central
    Notify --> SMTP
    Video -->|authorization, ownership check, metadata save| Central
    Browser -->|video bytes, signed upload| ImageKit
    Video -->|verify uploaded asset| ImageKit
    Browser -->|play media from CDN URL| ImageKit
```

## Services and responsibilities

### API Gateway

- Public backend entry point for the browser applications.
- Routes user/authentication, Central, Video, Notification, and Admin API paths
  to their respective services.
- Owns browser CORS configuration and removes the browser `Origin` header before
  forwarding requests downstream.
- Exposes `GET /api/config`, which returns the public API base URL. The clients
  load this at runtime instead of needing every API route baked into the build.
- Uses a shorter general response timeout and a longer timeout for the legacy
  multipart upload route. Current ImageKit video bytes bypass the Gateway.

### Central service

- Owns user, channel, video metadata, tags, engagement, playlists, watch
  history, reports, and in-app notification records.
- Uses PostgreSQL through Spring Data JPA; Flyway migrations define schema
  evolution.
- Issues and validates JWTs. Passwords are encoded with BCrypt.
- Provides feed, search, trending, shorts, subscription, admin, and channel
  APIs.
- Owns report submission and moderation, including the reporter, reason,
  optional description, status, and admin-facing report details.
- Persists comment likes and returns per-comment like counts and the current
  viewer's like state with comment pages.
- Uses Redis for selected cache-aside reads, with PostgreSQL remaining the
  source of truth. Cache entries are evicted on relevant video writes.
- Publishes asynchronous email notifications and durable thumbnail requests
  through RabbitMQ. Thumbnail requests originate from a PostgreSQL outbox;
  Central applies completion results idempotently and ignores stale event IDs.
- Pushes in-app notifications to WebSocket topics.

### Video service

- Creates short-lived ImageKit upload authorization and accepts upload
  completion metadata.
- Checks channel ownership through an authenticated Central service call and
  enforces a configurable 100 MiB default upload limit.
- Verifies ImageKit's file ID, expected folder/name, media type, size, and CDN
  host before asking Central to store the metadata.
- Optionally authorizes and verifies a custom JPEG, PNG, or WebP thumbnail,
  including actual decoded image format and configured dimensions/size.
- Consumes thumbnail work asynchronously, downloads video assets to bounded
  temporary files, and uses FFmpeg/FFprobe to determine duration and extract a
  suitable 1280x720 thumbnail. It preserves custom images, retries processing,
  and reports an explicit fallback result on exhausted failures.
- Does not proxy current browser-uploaded file bytes through the Video service
  or its container filesystem.
- Keeps a legacy local storage implementation for historical stream records.
  The multipart `/api/v1/video/upload` endpoint is intentionally disabled with
  HTTP 410 to prevent new uploads from silently using ephemeral local disk.

### Notification service

- Consumes notification messages from a RabbitMQ queue.
- Sends email through configured SMTP credentials.
- Uses bounded retries and a dead-letter exchange/queue to avoid infinite
  requeue loops.
- Has an in-memory, bounded duplicate-message guard. This reduces duplicate
  emails for recent redeliveries but is not a durable, cross-instance
  idempotency store.

### Other modules and infrastructure

- `service-registry` exists in the Maven reactor, but the current Render
  Blueprint uses explicit service URLs rather than deploying service discovery.
- `backend/infrastructure` contains supporting infrastructure definitions and
  examples. A folder or compose definition there does not by itself mean that
  a component is deployed or used by the application.
- The current API code and Render Blueprint use RabbitMQ, PostgreSQL, Redis,
  and ImageKit. Do not assume Kafka, MongoDB, Kubernetes, or a media worker is
  part of the deployed request path.

## Main request and data flows

### Feed and playback

1. Frontend calls `GET /api/config` and reads `apiBaseUrl`.
2. Frontend requests `GET /api/v1/central/videos?limit=...` through the Gateway.
3. Central reads the feed from PostgreSQL (or the Redis cache) and returns
   metadata such as title, channel, visibility/status, and `videoLink`.
4. Feed queries include only videos whose status is `PUBLISHED` and visibility
   is `PUBLIC`, ordered by upload time descending.
5. The browser plays the returned `videoLink`. For current uploads this is an
   HTTPS ImageKit URL, so media delivery goes directly to ImageKit's CDN, not
   through Central or the Gateway.

The metadata row and media bytes are separate resources. PostgreSQL stores the
media URL; ImageKit stores/serves the video file. A row in PostgreSQL does not
prove that the referenced file still exists.

### New video upload

1. An authenticated channel owner chooses a video in the frontend. The client
   rejects non-video MIME types and files larger than 100 MiB by default.
2. Frontend requests
   `POST /api/v1/video/upload/authorization` with the channel ID, file name, and
   size.
3. Video asks Central to verify that the authenticated account owns the
   channel, then returns a generated video ID, safe file name, folder, public
   key, and signed upload parameters. The ImageKit private key stays on the
   server.
4. Browser uploads the bytes directly to ImageKit using the short-lived signed
   parameters.
5. Browser sends the returned ImageKit file ID and video metadata to
   `POST /api/v1/video/upload/complete`.
6. If a custom thumbnail is selected, the browser separately obtains signed
   authorization from `POST /api/v1/video/upload/thumbnail/authorization`,
   uploads the image to ImageKit, and includes its file ID in completion.
7. Video checks channel ownership again, retrieves asset details, and verifies
   the expected folder/name, file size, reported MIME type, HTTPS URL, and
   configured ImageKit host. For a custom thumbnail it additionally downloads
   the image within configured bounds and validates its actual format and
   dimensions.
8. Video asks Central to create the row as `PROCESSING` and insert a
   transactional outbox event. Central publishes the event to RabbitMQ only
   after the database transaction commits.
9. Video consumes the event, probes duration and generates/optimizes a
   1280x720 JPEG when no custom image exists, then stores it in ImageKit.
   Custom images are preserved. Video publishes a result for Central to apply.
10. Central ignores duplicate or stale event results, stores thumbnail source,
    URL, status, and duration, and marks the video `PUBLISHED`. Exhausted
    processing failures are recorded as `FALLBACK`/`DEFAULT`; the clients use
    a branded local image if no working thumbnail URL is available.
11. Home/feed can return the video after publication because the feed query
    selects only `PUBLISHED` + `PUBLIC` records.

For a legacy record whose local media file was lost, the channel owner can
start a replacement upload from its Watch page. Video authorizes replacement
only for an existing video on the owner's channel; after verifying the new
ImageKit asset, Central updates that record's media URL without resetting its
views or descriptive metadata. The original source file must be available to
the owner; PostgreSQL cannot reconstruct missing bytes.

The ownership check happens both before upload and at completion. The second
check prevents an untrusted client from completing metadata for a channel it
does not own. An upload that reaches ImageKit but fails completion can leave an
orphaned ImageKit asset; cleanup/reconciliation is a future operational
improvement.

### Authentication and service calls

- Registration and login return a signed JWT and a role value.
- The frontend stores the token and sends it in the `Authorization` header for
  protected routes.
- Spring Security filters validate requests and populate the authentication
  principal. Central's channel upload-permission check compares that identity
  with the channel owner's email.
- Video forwards the original Authorization header when calling Central for
  permission and metadata operations.
- The Gateway forwards requests to Central/Video/Notification using configured
  service URLs. In Render these use internal service addresses.
- Admin endpoints additionally enforce the `ADMIN` role at the server; hiding
  admin UI elements is not an authorization control.

Review endpoint-level authorization before extending the API. Several
user-specific endpoints currently accept a `userId` query parameter; clients
must not be treated as trusted sources of identity merely because they send a
JWT.

Login and registration return the user ID for new sessions. Existing sessions
are upgraded on application bootstrap by calling the authenticated
`GET /api/central/user/me` endpoint, so user-specific frontend requests do not
continue to send an absent ID after a frontend deployment.

### Notifications

There are two distinct notification paths:

1. Email events are published by Central to RabbitMQ and consumed by
   Notification service.
2. In-app notifications are persisted by Central in PostgreSQL and pushed to
   `/topic/notifications/{userId}` over SockJS/STOMP WebSocket.

These paths are related to user-visible notifications but are not the same
queue or delivery guarantee. More detail is in
[Notification events](../events/notification-events.md).

### Reports, comments, and Shorts engagement

1. A signed-in user submits a video report through Central. Central derives
   the reporter from the authenticated JWT principal rather than accepting a
   caller-selected reporter ID, validates the report, and persists it with
   `PENDING` status.
2. Admin clients use role-protected Central endpoints to search and filter
   paginated reports, inspect video/reporter/reason details, and update a
   report to `PENDING`, `REVIEWED`, `RESOLVED`, or `REJECTED`. Legacy
   `DISMISSED` reports are exposed as `REJECTED`. The Admin UI supports
   responsive report cards/details and a mobile navigation drawer.
3. Comment likes are stored in Central's `comment_likes` table. Comment
   listings include like counts and the requesting viewer's like state;
   authenticated users can toggle their own likes.
4. The Shorts experience is a frontend presentation of Central's existing
   public Shorts feed and engagement APIs. The client uses a full-screen
   snap-scrolling feed and activates playback for the visible item; this does
   not introduce a separate Shorts service or event pipeline. The frontend
   navigation and watch experience also adapt to mobile viewport sizes.

Report submission, report moderation, and comment likes are synchronous
Central REST/database operations. They do not publish RabbitMQ events.

## Persistence and domain model

Central's PostgreSQL schema includes these principal entities:

| Entity | Purpose |
| --- | --- |
| `AppUser` | Account, unique email, encoded password, role, profile fields |
| `Channel` | Creator-owned channel, subscriber relationship, counters, videos and playlists |
| `Video` | Metadata, ImageKit URLs, thumbnail source/status, duration, processing event ID, status/visibility, view count, channel, tags, short-form flag |
| `Tag` | Reusable video tags |
| `VideoLike` / `Comment` | Video engagement and threaded comments |
| `UserWatchHistory` | Per-user watched-video and resume data |
| `PlayList` | Channel playlist and its videos |
| `VideoReport` | User-submitted moderation report, optional description, and status |
| `CommentLike` | Unique user/comment like relation with cascade cleanup |
| `InAppNotification` | Persisted in-app notification and read state |

Flyway SQL files live in
`backend/central/src/main/resources/db/migration`. In production, keep
`JPA_DDL_AUTO=validate` and use migrations for schema changes rather than
letting Hibernate mutate the schema.

## Caching

Central configures Redis-backed Spring caching for selected video feed,
trending, shorts, and by-ID reads, plus channel summaries. Cache TTLs differ by
data volatility. Relevant video writes evict affected video caches. Cache
errors are logged and treated as cache misses/store skips; they should not make
Redis the source of truth or prevent PostgreSQL-backed requests from working.

## Key design decisions and trade-offs

- **Direct-to-ImageKit upload:** avoids routing large files through the Gateway
  or keeping them on an ephemeral application disk; requires signed-upload
  configuration and completion verification.
- **PostgreSQL for metadata:** relational ownership and engagement data can be
  queried consistently; large media objects are kept outside the database.
- **Gateway + internal service URLs:** one browser-facing API origin and
  lower-latency private service-to-service calls; Gateway is an availability
  dependency for browser API traffic.
- **RabbitMQ for email work:** request-handling services need not perform SMTP
  delivery synchronously; delivery is asynchronous and can be retried.
- **Thumbnail outbox and worker:** Central's transactional outbox avoids
  committing a video row without recording its processing request. RabbitMQ
  publisher confirms, bounded processing attempts, result event IDs, and
  dead-letter queues address delivery and duplicate/stale processing cases.
- **Redis as an optional cache:** reduces repeated read load, but cache
  correctness depends on eviction/expiry and the database remains authoritative.
- **Flyway migrations:** schema changes are versioned and repeatable across
  environments.

## Production hardening opportunities

These are items to assess, not claims that they are already implemented:

- Add durable upload reconciliation/cleanup for ImageKit assets whose completion
  step failed.
- Add authorization and object-level ownership checks consistently for all
  user-specific APIs, rather than trusting a caller-supplied `userId`.
- Use durable idempotency/outbox patterns for notification publication and
  deduplication where delivery must survive restarts.
- Add explicit rate limits, quotas, malware/content checks, and upload
  lifecycle observability appropriate to the service plan.
- Configure production-grade plans, backups, retention, alerting, and incident
  response for PostgreSQL, Redis, RabbitMQ, and the web services.
- Confirm CORS, WebSocket origin rules, JWT expiry/rotation, and admin account
  provisioning against the actual production threat model.
