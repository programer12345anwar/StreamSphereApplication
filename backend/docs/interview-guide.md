# StreamSphere interview and project walkthrough

This guide is intended for revision and for explaining the project clearly.
Describe current behavior accurately and distinguish implemented features from
future improvements.

## 30-second introduction

> StreamSphere is a video-sharing application built with React and a Java 17
> Spring Boot microservices backend. The API Gateway provides one browser-facing
> API, Central owns accounts and relational video/channel metadata in
> PostgreSQL, Video handles secure media-upload orchestration, and Notification
> consumes RabbitMQ messages to send emails. New video bytes upload directly
> from the browser to ImageKit, while verified media URLs and metadata are
> stored in Central. Redis accelerates selected catalog reads, and WebSockets
> support real-time in-app notifications.

## Two-minute walkthrough

1. **Client and entry point:** React frontend and admin console use the Gateway
   `/api/config` endpoint to discover the public API base URL. The Gateway
   routes requests to the responsible backend service and owns browser CORS.
2. **Identity:** Central registers/logs in users, hashes passwords with BCrypt,
   and issues JWTs. Service security filters use those tokens to authenticate
   requests; Admin operations require the `ADMIN` role.
3. **Catalog:** Central stores users, channels, video metadata, and engagement
   data in PostgreSQL. Feed/search/trending/shorts endpoints return DTOs rather
   than media bytes. Feed queries only return published public videos.
4. **Media:** The Video service authenticates channel ownership, creates
   short-lived ImageKit upload authorization, and validates the uploaded asset
   at completion. The browser sends bytes directly to ImageKit; Central stores
   the verified CDN URL alongside the metadata.
5. **Notifications:** Central publishes email notification messages through
   RabbitMQ. Notification service handles SMTP delivery. A separate Central
   WebSocket path stores and pushes in-app notifications.
6. **Performance and operations:** Redis caches selected Central reads with
   bounded TTLs and cache invalidation; Flyway manages schema migrations;
   Docker/Render host services and dependencies; CI tests backend modules and
   builds the frontend.

For already existing users, the authenticated profile endpoint also lets a new
frontend version backfill a missing user ID into a stored browser session.

## Feature-to-service map

| Feature | Main owner | Supporting components |
| --- | --- | --- |
| Login, registration, JWT | Central | Gateway, Frontend/Admin |
| Channel ownership and subscriptions | Central | Video calls Central for upload permission |
| Video upload authorization and verification | Video | Central, ImageKit |
| Video metadata and Home feed | Central | PostgreSQL, Redis |
| Video byte storage and playback CDN | ImageKit | Browser; Video verifies asset |
| Search, trending, shorts | Central | PostgreSQL, Redis for selected cached reads |
| Likes, comments, watch history, playlists, reports | Central | PostgreSQL |
| Email notifications | Central + Notification | RabbitMQ, SMTP |
| Real-time in-app notifications | Central | PostgreSQL, SockJS/STOMP WebSocket |
| Browser routing and CORS | API Gateway | Central, Video, Notification |
| Admin moderation and analytics | Central | Admin frontend |

## Questions you may be asked

### Why use microservices instead of one Spring Boot application?

The services separate operational capabilities and ownership: Central owns
relational application data, Video handles media integration, Notification
handles asynchronous delivery, and Gateway centralizes browser routing. This
allows independent deployment/scaling, but introduces network failures,
distributed tracing, configuration, and data-consistency complexity. For a
small team, a modular monolith could be simpler; service boundaries are useful
when they match operational needs.

### Why store videos outside PostgreSQL?

Video files are large binary objects with different throughput and delivery
needs from relational metadata. PostgreSQL stores searchable/relational data
and the media URL; ImageKit stores and serves the bytes through a media CDN.
This avoids growing the database with large blobs and lets media delivery scale
independently.

### Walk through the upload flow.

The authenticated owner asks Video for a short-lived signed authorization.
Video calls Central to verify the channel owner and returns a generated ID,
safe filename/folder, public key, and ImageKit signature. The browser uploads
directly to ImageKit. It then sends the returned file ID plus metadata to
Video. Video rechecks ownership and fetches asset details from ImageKit to
verify file ID, path/name, MIME, size, and URL host before Central persists the
metadata and CDN URL.

### Why does the feed show only some uploaded videos?

The feed query filters for `PUBLISHED` and `PUBLIC`. Metadata can exist while
the status/visibility excludes it. Playback also depends on the media URL
pointing to an existing asset. For old local-disk uploads, the database row can
survive a redeploy while the container file is lost; re-uploading to durable
ImageKit storage is necessary.

### Why does the browser upload directly rather than proxying through Video?

Direct upload avoids a second large network hop and avoids buffering the file
in the API Gateway/service. It also removes dependence on ephemeral app disks.
The trade-off is that the browser interacts with ImageKit and the completion
endpoint must independently verify the claimed asset before saving metadata.

### What is the role of RabbitMQ?

It decouples email delivery from synchronous API work. Central publishes a
typed message, and Notification service consumes it and sends SMTP email. This
allows asynchronous handling and bounded retries/dead-letter routing. Current
producer/deduplication behavior is best effort, so it should not be described
as exactly-once or transactionally guaranteed.

### What is Redis used for?

Central uses Redis as a cache for selected read paths, such as video feeds,
trending/shorts, individual video metadata, and channel summaries. TTLs bound
staleness and write operations evict related entries. PostgreSQL remains the
source of truth; cache failures should fall back to database-backed behavior.

### How does authentication work?

Central authenticates credentials and issues a JWT containing the identity and
role claims. Spring Security filters verify tokens and establish the request
principal. Protected APIs require the token, and admin APIs additionally
require the admin role. Video forwards the caller's Authorization header for
Central ownership/metadata calls. Object-level authorization should be
consistent for every endpoint that accepts a caller-supplied user ID.

### How are database schema changes managed?

Flyway SQL migrations are versioned under
`backend/central/src/main/resources/db/migration`. Production config uses
Hibernate validation instead of automatic schema mutation, so schema changes
are reviewed and applied explicitly.

### How do you deploy and validate it?

The Render Blueprint provisions Gateway, Central, Video, Notification,
PostgreSQL, and Redis. RabbitMQ, ImageKit, and SMTP credentials are configured
externally. Frontend/Admin are deployed separately. CI runs Java tests for the
backend modules and frontend tests/build. After deployment, validate health,
runtime API config, feed response, signed upload/completion, saved CDN URL,
actual playback, and notification delivery. CI passing is not the same as
deployment succeeding.

## Current limitations worth stating honestly

- Render's current free service/database plans are not a production capacity or
  durability recommendation; check current provider limits and upgrade for
  real workloads.
- Earlier local-disk video uploads can be missing after ephemeral host
  restarts. The ImageKit flow protects new uploads after correct configuration
  and redeployment; it cannot recreate old files. A channel owner can restore
  the existing record by uploading the original source through the replace
  flow; if no source copy exists, a new upload is required.
- ImageKit upload followed by metadata completion is a two-system workflow.
  Failed completion can leave an orphaned asset; automated cleanup/reconciliation
  is not yet implemented.
- RabbitMQ publishing is best effort in the current producer, and the duplicate
  guard is in memory. This is not transactional exactly-once notification
  delivery.
- Some APIs identify a user via a caller-supplied `userId`. JWT authentication
  alone does not prove that the caller owns that ID; review and strengthen
  object-level authorization consistently.
- `service-registry` and infrastructure examples exist in the repository, but
  the current Render Blueprint routes via configured service URLs and does not
  deploy a registry.
- A media worker is not needed for the current direct upload and playback
  flow. Transcoding, adaptive bitrate packaging, thumbnails, moderation, or
  asynchronous media processing would be separate future capabilities.

## Suggested closing summary

> The core design separates relational metadata from media storage, uses signed
> direct-to-CDN uploads with server-side verification, and keeps the browser
> API behind a Gateway. PostgreSQL is authoritative, Redis is an optimization,
> RabbitMQ handles asynchronous email, and Flyway controls schema evolution.
> The main next steps for production maturity are stronger object-level
> authorization, durable event delivery/idempotency, upload reconciliation,
> and production-grade infrastructure plans and monitoring.
