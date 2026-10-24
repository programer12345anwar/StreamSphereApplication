# Deploying StreamSphere on Render

The repository-root `render.yaml` defines the four backend web services plus a
Render Postgres database and Redis instance. Use it to create or update the
backend resources together as one Render Blueprint.

## Before creating the Blueprint

1. Push the repository to GitHub.
2. Create a hosted RabbitMQ instance with a TLS connection, and have its host,
   port, username, and password ready. The Blueprint expects TLS by default.
3. Have ImageKit API keys and SMTP credentials ready.
4. Decide the exact public frontend and admin origins for the CORS prompts.

The Blueprint sets each Docker service's root directory to `backend`. Its
Dockerfile paths are relative to that directory, and its Docker build context is
the backend directory so each Dockerfile can copy the Maven parent POM and its
module POMs.

## Create and deploy

1. In Render, select **New + > Blueprint**.
2. Connect this GitHub repository and select the branch containing `render.yaml`.
3. Review the resources in the Blueprint. All services use the `oregon` region;
   change every region together in `render.yaml` before creating resources if
   you want a different supported region.
4. During initial setup, enter the prompted values for:
   - `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, and
     `RABBITMQ_PASSWORD`, and the exact `RABBITMQ_VIRTUAL_HOST` for both
     Central and Notification.
   - `IMAGE_URL`, `IMAGE_PUBLIC_KEY`, and `IMAGE_PRIVATE_KEY` for Video.
   - `MAIL_USERNAME` and `MAIL_PASSWORD` for Notification.
   - `PLATFORM_BASE_URL` and `PLATFORM_LOGO_URL` for Notification.
   - `CORS_ALLOWED_ORIGINS` for each service. Enter the exact HTTPS frontend and
     admin origins, comma-separated, without a trailing slash.
5. Review the plan and cost shown by Render, then create the Blueprint. Render
   provisions PostgreSQL and Redis and deploys the four backend services.
6. Open each service's Events and Logs in Render. Wait for each deployment to
   finish and for `/actuator/health` to report `UP`.
7. Frontend and Admin use the public
   `https://streamsphere-api-gateway.onrender.com` URL configured in their API
   clients. If your Render gateway URL changes, update the URL in both apps and
   rebuild/redeploy them.

The Blueprint generates `CENTRAL_SECRET_KEY` automatically and wires the
database, Redis, Central API, and gateway service URLs through Render references.
Do not put secrets in `render.yaml` or commit a local `.env` file.
The Gateway routes Central and Video API calls to their private Render
host-port addresses (`CENTRAL_SERVICE_HOSTPORT` and `VIDEO_SERVICE_HOSTPORT`).
These services are in the same region, so internal traffic avoids an unnecessary
round trip through Render's public edge. The Video service also uses its private
`CENTRAL_SERVICE_HOSTPORT` address for Central API calls. Browser CORS is
enforced at the Gateway; it removes the browser `Origin` header before
forwarding API requests so downstream services do not reject origins already
allowed by the Gateway.
The API Gateway exposes `GET /api/config`, which returns the public API base URL
from `PUBLIC_API_BASE_URL`. If the Gateway's public URL changes, update that
Render environment variable and the `VITE_API_CONFIG_URL` value in both Vercel
projects; application API URLs are then read dynamically from the config
endpoint.

`sync: false` values are requested during the initial Blueprint creation only.
If you add or change one later, update it in the relevant service's Render
Environment page.

## Runtime and cost notes

The Blueprint uses Render's `free` plans to make a no-cost first deployment
possible where those plans are available. Free web services can sleep when idle,
and free Postgres has limited lifetime/storage and is not appropriate for
production data. Check Render's current plan limits before using this for a live
production launch; upgrade the service and database plans as needed.

All four services and the database are configured for `oregon` so private
database and Redis references work. Use a supported Render region and keep the
region the same for all resources.

If your RabbitMQ provider does not use TLS, set
`SPRING_RABBITMQ_SSL_ENABLED=false` in both the Central and Notification
services. Never use local `guest` credentials or `localhost` for production
dependencies.

## Smoke tests

Check health:

```text
GET https://<service-url>/actuator/health
```

Try gateway routes:

```text
GET  https://<gateway-url>/api/v1/central/videos
POST https://<gateway-url>/api/central/user/register
POST https://<gateway-url>/api/central/user/login
POST https://<gateway-url>/api/v1/video/upload
```

Video upload requires valid ImageKit credentials and an authenticated request.

## Production checklist

- Replace free plans with production-appropriate plans and confirm database
  backups/retention.
- Use exact frontend/admin HTTPS origins in CORS settings.
- Keep ImageKit, RabbitMQ, SMTP, and database credentials only in Render's
  Environment settings.
- Rotate any credentials that were exposed or used in local development.
