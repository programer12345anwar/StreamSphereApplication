# Development and deployment

This document describes the repository's current backend development path.
For Render Blueprint steps and provider-specific operations, see
[`backend/DEPLOYMENT_RENDER.md`](../DEPLOYMENT_RENDER.md).

## Requirements

- Java 17 (the Maven parent config targets release 17).
- Node.js 20 or a compatible current Node release for frontend tooling.
- Docker Desktop / Docker Compose for local PostgreSQL and Redis.
- PostgreSQL-compatible database.
- RabbitMQ for notification publishing/consuming (a local broker or hosted
  provider).
- ImageKit account/API keys for video uploads.
- SMTP credentials if testing outbound email.

## Local dependency services

The current `backend/docker-compose.yml` starts PostgreSQL, pgAdmin, and Redis.
It does **not** start the Spring Boot apps or RabbitMQ.

From a PowerShell terminal:

```powershell
Set-Location D:\path\to\StreamSphereApplication\backend
Copy-Item .env.example .env
docker compose up -d db redis pgadmin
```

Review `.env` and replace local defaults as needed. Never commit `.env`.
`backend/.env.example` contains example values; use real secrets only in a
local ignored `.env` or a secret manager. The local defaults are for
development and are not suitable production credentials.

If RabbitMQ is not available locally, Central may continue some operations
without publishing messages because the producer logs publish failures as
best-effort. Email delivery then cannot be validated. For a full notification
test, start/configure a RabbitMQ broker with matching Central and Notification
exchange, queue, and routing-key settings.

## Start backend services

Each service is an independent Spring Boot application with its own Maven
wrapper. Run each command in a separate terminal; configure environment
variables before starting if the local `.env` is not being loaded by your
launch configuration.

```powershell
Set-Location D:\path\to\StreamSphereApplication\backend\api-gateway
.\mvnw.cmd spring-boot:run

Set-Location D:\path\to\StreamSphereApplication\backend\central
.\mvnw.cmd spring-boot:run

Set-Location D:\path\to\StreamSphereApplication\backend\video-service
.\mvnw.cmd spring-boot:run

Set-Location D:\path\to\StreamSphereApplication\backend\notification-api
.\mvnw.cmd spring-boot:run
```

Default local ports:

| App | Port |
| --- | ---: |
| API Gateway | 8080 |
| Notification | 8082 |
| Video | 8083 |
| Central | 8084 |
| Frontend Vite | 5173 (or next available port) |
| Admin Vite | 5173 if available; Vite selects the next free port when occupied |

The Gateway's default upstream service URLs point at `localhost` ports from
these settings. Check each service's `application.properties` if changing a
local port. The Service Registry module is not required for this manual
URL-based local run.

## Start the web apps

In separate terminals:

```powershell
Set-Location D:\path\to\StreamSphereApplication\frontend
npm ci
npm run dev
```

For the Admin app:

```powershell
Set-Location D:\path\to\StreamSphereApplication\admin
npm ci
npm run dev
```

Both clients load their API endpoint at runtime from Gateway `/api/config`.
Override `VITE_API_CONFIG_URL` if the Gateway does not use the default
production bootstrap URL.

## Environment configuration

The canonical local example is `backend/.env.example`; individual services
also have module-level examples. Key settings include:

| Purpose | Variables |
| --- | --- |
| PostgreSQL | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, or Render's `DB_CONNECTION_STRING` |
| Schema lifecycle | `JPA_DDL_AUTO`, `FLYWAY_ENABLED`, `FLYWAY_BASELINE_ON_MIGRATE` |
| JWT | `CENTRAL_SECRET_KEY` |
| Redis | `REDIS_HOST`, `REDIS_PORT`, `REDIS_TIMEOUT` |
| RabbitMQ | `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `RABBITMQ_VIRTUAL_HOST`, `SPRING_RABBITMQ_SSL_ENABLED` |
| ImageKit | `IMAGE_URL`, `IMAGE_PUBLIC_KEY`, `IMAGE_PRIVATE_KEY` |
| Upload limit | `MAX_UPLOAD_FILE_SIZE`, `MAX_UPLOAD_REQUEST_SIZE`; Video service also has `VIDEO_UPLOAD_MAX_BYTES` defaulting to 500 MiB |
| Service addresses | `CENTRAL_API_URL`, `CENTRAL_SERVICE_URL`, `VIDEO_SERVICE_URL`, `NOTIFICATION_SERVICE_URL` |
| Browser access | `CORS_ENABLED`, `CORS_ALLOWED_ORIGINS` |
| Gateway runtime config | `PUBLIC_API_BASE_URL` |
| Email | `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, and SMTP flags |
| Notification branding | `PLATFORM_NAME`, `PLATFORM_BASE_URL`, `PLATFORM_LOGO_URL` |

Names can have service-specific aliases. Confirm the target service's
`application.properties` and Render environment settings before changing a
value. Keep ImageKit's private key and all database/broker/SMTP credentials
server-side.

## Build and test

From the repository root:

```powershell
Set-Location D:\path\to\StreamSphereApplication\backend\central
.\mvnw.cmd test

Set-Location D:\path\to\StreamSphereApplication\backend\video-service
.\mvnw.cmd test

Set-Location D:\path\to\StreamSphereApplication\backend\notification-api
.\mvnw.cmd test

Set-Location D:\path\to\StreamSphereApplication\backend\api-gateway
.\mvnw.cmd test
```

Frontend validation:

```powershell
Set-Location frontend
npm ci
npm run test
npm run build
```

Admin uses the same Vite scripts; check `admin/package.json` for its available
checks.

GitHub Actions runs Central, Video, Notification, Gateway, and Docker Compose
configuration checks plus frontend tests/build on pushes to `main`/`master`
and pull requests. CI is validation, not proof that Render or Vercel has
deployed a revision.

## Production deployment overview

- `render.yaml` provisions the backend web services, PostgreSQL, and Redis.
  The current Blueprint deploys Gateway, Central, Video, and Notification.
- Configure a hosted RabbitMQ broker and credentials; configure SMTP for email.
- Set ImageKit URL endpoint and keys on the Video service. Allow the frontend
  origin in the ImageKit upload security settings.
- Set exact frontend/admin origins in Gateway CORS configuration. Keep secrets
  in Render's environment/secret settings, not in Git.
- Deploy the frontend and Admin separately (their READMEs describe Vercel
  project roots). Point `VITE_API_CONFIG_URL` at the public Gateway if it
  changes.
- In production, set `PUBLIC_API_BASE_URL` to the externally reachable Gateway
  URL. Browser clients use this value after `/api/config`.
- Prefer private service-to-service URLs inside the same Render region.
- Upgrade free-tier resources and configure backups, retention, monitoring,
  and scaling before relying on the system for production workloads.

## Production smoke checks

1. Open `/actuator/health` on Gateway, Central, Video, and Notification.
2. Call Gateway `GET /api/config`; ensure `apiBaseUrl` is the public HTTPS
   Gateway URL.
3. Call `GET /api/v1/central/videos?limit=5`; confirm the response is an array
   and returned records are `PUBLISHED`/`PUBLIC`.
4. Register/login with a test account; confirm protected requests carry a JWT.
5. Create or locate a channel owned by that account.
6. Upload a small test video; inspect Network requests:
   authorization -> ImageKit upload -> completion.
7. Confirm the saved `video_link` is an HTTPS ImageKit URL and playback works
   directly from the CDN, including range/seek behavior.
8. Verify database metadata separately from ImageKit media storage. A row with
   an old local `/api/v1/video/stream/...` URL does not guarantee that the local
   file remains on the host.
9. Trigger a test notification only with safe test email and inspect RabbitMQ,
   Notification logs, and mail provider delivery.

## Inspecting PostgreSQL metadata

The Central service owns video records. Connect to the same database configured
by `DB_URL` locally or `DB_CONNECTION_STRING` on Render, then run a read-only
query in pgAdmin's Query Tool or `psql`:

```sql
SELECT id, name, status, visibility, channel_id,
       upload_date_time, video_link, thumbnail_link
FROM videos
ORDER BY upload_date_time DESC
LIMIT 50;
```

This confirms that metadata exists and shows the stored media URL. It does not
confirm that the referenced bytes exist in ImageKit. For current uploads,
`video_link` should be an HTTPS URL on the configured ImageKit host. A legacy
relative `/api/v1/video/stream/...` URL points at local service storage and may
no longer resolve after a container restart/redeploy.

For a legacy video that returns 404, deploy the current backend and frontend,
sign in as the channel owner, open that video's Watch page, choose **Replace
missing video file**, and upload the original source again. The replacement
flow keeps the existing video record and updates only its verified ImageKit
URL. If the original source file is unavailable, it cannot be recovered from
the metadata row; upload a new source as a new video instead.

The frontend requests the current user profile after validating an existing
session. This backfills the user ID into old browser sessions and prevents
requests such as resume-time/subscription checks from sending
`userId=undefined`. If the browser still shows that request after deployment,
sign out and sign back in, then confirm `/api/central/user/me` returns the
profile `id`.

## Troubleshooting map

| Symptom | Check |
| --- | --- |
| Home says no videos | Central feed only includes `PUBLISHED` + `PUBLIC`; inspect query result, status, visibility, and whether the cache was invalidated |
| Metadata exists but playback fails | Compare `videoLink` to the actual ImageKit URL; check asset existence, CDN response, HTTPS host, and browser Network request |
| Upload authorization is 403 | Confirm JWT is present and authenticated email owns the channel |
| Authorization is 503 | Check Video service ImageKit public key/configuration |
| ImageKit upload is blocked | Verify signed fields, ImageKit account restrictions, CORS/security allowlist, and endpoint |
| Completion is rejected | Confirm file ID belongs to the generated filename/folder, file is within size limit and has `video/*` MIME, and URL host matches ImageKit config |
| Completion returns gateway/dependency error | Check Video-to-Central internal URL, forwarded Authorization, Central health, and ImageKit credentials/logs |
| Old video gives 404/500 | Local storage is not durable on ephemeral containers; re-upload source media to ImageKit after deployment |
| Watch page offers no replacement button | Confirm you are signed in as the channel owner, the video has a legacy `/api/v1/video/stream/` URL, and latest frontend/backend deployments are active |
| Replacement upload returns 403 | Confirm that the logged-in account owns the video's channel and the request uses the existing video ID |
| Resume/subscription requests contain `userId=undefined` | Deploy Central and frontend updates, reload/sign in, and verify `/api/central/user/me` supplies an ID |
| Email does not arrive | Inspect RabbitMQ topology/consumer, message type, SMTP credentials, provider logs, and spam/quota settings |
| Browser CORS error | Check Gateway exact allowed origin and runtime config; do not fix by exposing private service URLs |
