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

## Create and deploy

1. In Render, select **New + > Blueprint**.
2. Connect this GitHub repository and select the branch containing `render.yaml`.
3. Review the resources in the Blueprint. All services use the `oregon` region;
   change every region together in `render.yaml` before creating resources if
   you want a different supported region.
4. During initial setup, enter the prompted values for:
   - `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, and
     `RABBITMQ_PASSWORD` for both Central and Notification.
   - `IMAGE_URL`, `IMAGE_PUBLIC_KEY`, and `IMAGE_PRIVATE_KEY` for Video.
   - `MAIL_USERNAME` and `MAIL_PASSWORD` for Notification.
   - `PLATFORM_BASE_URL` and `PLATFORM_LOGO_URL` for Notification.
   - `CORS_ALLOWED_ORIGINS` for each service. Enter the exact HTTPS frontend and
     admin origins, comma-separated, without a trailing slash.
5. Review the plan and cost shown by Render, then create the Blueprint. Render
   provisions PostgreSQL and Redis and deploys the four backend services.
6. Open each service's Events and Logs in Render. Wait for each deployment to
   finish and for `/actuator/health` to report `UP`.
7. Copy the public URL of the `streamsphere-api-gateway` service. Set
   `VITE_API_GATEWAY_URL` to that URL in the frontend's hosting provider, then
   rebuild/redeploy the frontend.

The Blueprint generates `CENTRAL_SECRET_KEY` automatically and wires the
database, Redis, Central API, and gateway service URLs through Render references.
Do not put secrets in `render.yaml` or commit a local `.env` file.

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
