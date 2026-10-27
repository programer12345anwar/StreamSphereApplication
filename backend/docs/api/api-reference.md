# API reference

This is a concise map of the main REST surface. The Gateway is the public
browser-facing entry point; routes are forwarded to the service that owns the
API. Use the service's generated OpenAPI/Swagger documentation for full DTO
schemas and current response shapes.

## Base URLs

| Environment | Base URL |
| --- | --- |
| Local Gateway | `http://localhost:8080` |
| Render | `https://<gateway-public-host>` |

Frontend and Admin first call `GET /api/config`; its `apiBaseUrl` field is the
base URL for application requests. Configure the Gateway's
`PUBLIC_API_BASE_URL` in production.

## Authentication conventions

- Registration and login are public and return a JWT.
- Protected routes expect `Authorization: Bearer <token>`.
- Video upload authorization and completion require an authenticated channel
  owner.
- Admin routes require a valid token with the `ADMIN` role.
- Public feed and playback routes are intentionally readable without login.
- A number of current user-specific APIs still accept `userId` as a query
  parameter. Treat this as an implementation limitation and verify ownership
  on the server before using or extending these endpoints.

## Runtime configuration and health

| Method | Path | Service | Description |
| --- | --- | --- | --- |
| `GET` | `/api/config` | Gateway | Public runtime API base URL; response is not cached |
| `GET` | `/actuator/health` | Each service | Health status used by orchestration |

## Accounts and security

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/central/user/register` | Register an account and return a JWT/role |
| `POST` | `/api/central/user/login` | Authenticate credentials and return a JWT/role |
| `GET` | `/api/central/user/me` | Return the authenticated user's safe profile, including the database ID |
| `GET` | `/api/v1/central/security/validate-token/{token}` | Validate a token |
| `GET` | `/api/v1/central/security/get-credential/{token}` | Read the token credential claim |

Registration/login request and error details are defined by the Central DTOs.
Do not log or share passwords, JWTs, or token-bearing URLs. Avoid putting
tokens in query strings outside these existing compatibility endpoints.

## Channels

Base path: `/api/v1/central/channel`

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/create` | Create a channel for the authenticated user |
| `GET` | `/my-channel?email=...` | Find a channel by owner email |
| `GET` | `/popular` | Return popular channel summaries |
| `GET` | `/search?q=...` | Search channels |
| `GET` | `/channelWithTag?tag=...` | Find channels by tag |
| `GET` | `/subscribed?userId=...` | List channels subscribed to by a user |
| `PUT` | `/{channelId}/subscribe?userId=...` | Subscribe a user to a channel |
| `PUT` | `/{channelId}/unsubscribe?userId=...` | Remove a channel subscription |
| `GET` | `/{channelId}/check-subscription?userId=...` | Check subscription state |
| `GET` | `/{channelId}/upload-permission[?videoId=...]` | Verify channel ownership; optional video ID additionally verifies that the video belongs to that channel |
| `PUT` | `/{channelId}/video/{videoId}/media` | Replace media URL for an existing channel video; channel owner only, HTTPS URL required |

The `my-channel` route currently takes an email parameter. Do not treat the
parameter itself as proof of ownership; the dedicated upload permission check
uses the authenticated principal.

## Video catalog

Base path: `/api/v1/central/videos`

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/?limit=30` | Latest public, published videos |
| `GET` | `/{videoId}` | Fetch one video's metadata |
| `GET` | `/trending?limit=30` | Videos from the recent trending window, ordered by views |
| `GET` | `/search?q=...&limit=30` | Search video name/description, channel, and tags |
| `GET` | `/subscriptions?userId=...&limit=30` | Public/published videos from subscribed channels |
| `GET` | `/shorts?limit=10` | Public/published videos marked as shorts |

The Home feed query only selects rows with `status=PUBLISHED` and
`visibility=PUBLIC`. An uploaded row that is still processing, private, or
unlisted will not appear in that feed.

## Video upload and media delivery

Base path: `/api/v1/video`

### Current direct-to-ImageKit upload

1. `POST /upload/authorization?channelId={uuid}&fileName={name}&fileSize={bytes}[&replaceVideoId={existingVideoId}]`
   - Requires a valid JWT and channel ownership.
   - Returns `videoId`, `fileName`, ImageKit `folder`, `publicKey`,
     short-lived `authentication` values, and whether this is a replacement.
     For a replacement, `videoId` is the existing record's ID and its channel
     ownership is checked.
2. Browser posts the bytes directly to
   `https://upload.imagekit.io/api/v1/files/upload` with the authorization
   fields and ImageKit multipart fields.
3. `POST /upload/complete`
   - Requires a valid JWT and the JSON body below.
   - Video verifies the returned ImageKit file ID via ImageKit's API and asks
     Central to persist the verified metadata.
   - Returns `201 Created` for a new video and `200 OK` when replacing media
     on an existing video.

Example completion request:

```json
{
  "channelId": "00000000-0000-0000-0000-000000000000",
  "videoId": "generated-uuid",
  "fileId": "imagekit-file-id",
  "replaceExisting": false,
  "name": "Video title",
  "description": "Optional description",
  "tags": ["example"],
  "isShort": false
}
```

The authenticated user must own `channelId`. Accepted video size defaults to
100 MiB (104,857,600 bytes), matching the current ImageKit account limit. The
Video service limit can be configured with `VIDEO_UPLOAD_MAX_BYTES`; when
changing it, keep the frontend validation and the ImageKit account limit in
sync. The frontend also checks that the browser-provided MIME type starts with
`video/`; the server independently verifies the MIME type reported by ImageKit.

To restore an existing video whose old local file is gone, the channel owner
must still have the original source file. Start from the watch page's
**Replace missing video file** action. The flow keeps the same video ID and
preserves title, description, tags, visibility, status, and view count while
updating only the verified media URL. The server verifies ownership both
before upload and at completion; it rejects replacement attempts for videos
from another channel.

### Legacy paths

| Method | Path | Current behavior |
| --- | --- | --- |
| `POST` | `/upload` | Disabled; returns HTTP 410 to prevent new local-disk uploads |
| `GET` | `/stream/{filename}` | Legacy local-media stream route; old files may be unavailable after an ephemeral deployment restart |

New upload completion stores the absolute ImageKit media URL in the video
record. Playback should use that URL directly. PostgreSQL stores metadata and a
URL; it does not contain the video bytes.

## Engagement, library, and notifications

### Engagement: `/api/v1/central/engagement`

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/video/{videoId}/like?userId={uuid}&isLike={boolean}` | Add or remove a like |
| `POST` | `/video/{videoId}/comment?userId={uuid}` | Add a comment; JSON includes `text`, optional `parentId` |
| `GET` | `/video/{videoId}/comments?page=0&size=20` | Paginated comments |
| `GET` | `/comment/{parentId}/replies?page=0&size=20` | Paginated replies |
| `POST` | `/video/{videoId}/view` | Record a view |
| `POST` | `/video/{videoId}/report?userId={uuid}` | Submit a report; JSON includes `reason` |

### Library: `/api/v1/central/library`

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/history?userId={uuid}&videoId={id}` | Record watch history |
| `GET` | `/history?userId={uuid}&page=0&size=30` | Read paginated watch history |
| `DELETE` | `/history?userId={uuid}` | Clear watch history |
| `PUT` | `/history/resume?userId={uuid}&videoId={id}&resumeTime={seconds}` | Save resume position |
| `GET` | `/history/resume?userId={uuid}&videoId={id}` | Read resume position |
| `POST` | `/playlist?channelId={uuid}&name={name}` | Create a playlist |
| `GET` | `/channel/{channelId}/playlists` | List channel playlists |
| `POST` | `/playlist/{playlistId}/video/{videoId}` | Add a video |
| `DELETE` | `/playlist/{playlistId}/video/{videoId}` | Remove a video |
| `DELETE` | `/playlist/{playlistId}` | Delete a playlist |

### In-app notifications: `/api/v1/central/notifications`

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/?userId={uuid}&page=0&size=20` | Read paginated notifications |
| `GET` | `/unread-count?userId={uuid}` | Count unread notifications |
| `PUT` | `/{id}/read` | Mark one notification read |
| `PUT` | `/read-all?userId={uuid}` | Mark all notifications read |

## Admin

Base path: `/api/v1/admin`. These endpoints require the `ADMIN` role as
enforced by Central.

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/analytics` | Aggregate counts |
| `GET` | `/users?page=0&size=20` | Paginated users |
| `PUT` | `/users/{id}/role?role=...` | Change a user's role |
| `DELETE` | `/users/{id}` | Delete a user |
| `GET` | `/videos?page=0&size=20` | Paginated videos |
| `DELETE` | `/videos/{id}` | Delete a video |
| `GET` | `/channels?page=0&size=20` | Paginated channels |
| `GET` | `/reports?page=0&size=20` | Paginated reports |
| `PUT` | `/reports/{id}/status?status=...` | Update report status |

## OpenAPI / Swagger

Service OpenAPI endpoints are `/v3/api-docs`; Swagger UI is normally available
at `/swagger-ui.html`. The Gateway aggregates docs behind `/docs/central/**`,
`/docs/video/**`, and `/docs/notification/**`. Exact hostnames and external
documentation URLs are environment-dependent.
