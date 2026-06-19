# StreamSphere Deployment Guide (Render + Docker)

## 1. Deployment topology

You should deploy backend services **separately**.

1. `central`
2. `video-service`
3. `notification-api`
4. `api-gateway`

The gateway is the public backend entrypoint used by frontend.  
Yes, you deploy all 4 backend services independently.

## 2. Required managed dependencies

Provision these before service rollout:

1. PostgreSQL (for `central`)
2. RabbitMQ (for `central` producer and `notification-api` consumer)
3. SMTP credentials (for `notification-api`)
4. ImageKit credentials (for `video-service`)

## 3. Render service creation

Create 4 Render Web Services, each with `Runtime: Docker`.

1. Service `central` with root directory `StreamSphereBackend/central`
2. Service `video-service` with root directory `StreamSphereBackend/video-service`
3. Service `notification-api` with root directory `StreamSphereBackend/notification-api`
4. Service `api-gateway` with root directory `StreamSphereBackend/api-gateway`

Each service already contains a root `Dockerfile`.

## 4. Environment variables

Use each `.env.example` as baseline.

1. `StreamSphereBackend/central/.env.example`
2. `StreamSphereBackend/video-service/.env.example`
3. `StreamSphereBackend/notification-api/.env.example`
4. `StreamSphereBackend/api-gateway/.env.example`

Set real production values for secrets and URLs in Render dashboard.

### Central service

```env
DB_URL=
DB_USERNAME=
DB_PASSWORD=
CENTRAL_SECRET_KEY=
RABBITMQ_HOST=
RABBITMQ_PORT=
RABBITMQ_USERNAME=
RABBITMQ_PASSWORD=
RABBITMQ_EXCHANGE_NAME=
RABBITMQ_QUEUE_NAME=
RABBITMQ_ROUTING_KEY=
CORS_ALLOWED_ORIGINS=
```

### Video service

```env
IMAGE_URL=
IMAGE_PRIVATE_KEY=
IMAGE_PUBLIC_KEY=
CENTRAL_API_URL=
CORS_ALLOWED_ORIGINS=
```

### Notification service

```env
RABBITMQ_HOST=
RABBITMQ_PORT=
RABBITMQ_USERNAME=
RABBITMQ_PASSWORD=
RABBITMQ_EXCHANGE_NAME=
RABBITMQ_QUEUE_NAME=
RABBITMQ_ROUTING_KEY=
MAIL_HOST=
MAIL_PORT=
MAIL_USERNAME=
MAIL_PASSWORD=
PLATFORM_NAME=
PLATFORM_BASE_URL=
PLATFORM_LOGO_URL=
CORS_ALLOWED_ORIGINS=
```

### API gateway

```env
CENTRAL_SERVICE_URL=
VIDEO_SERVICE_URL=
NOTIFICATION_SERVICE_URL=
CORS_ALLOWED_ORIGINS=
```

## 5. Service URL wiring

After first deploy, copy Render service URLs and wire:

1. `CENTRAL_API_URL=<central-service-url>/api/v1/central` in `video-service`
2. `CENTRAL_SERVICE_URL=<central-service-url>` in `api-gateway`
3. `VIDEO_SERVICE_URL=<video-service-url>` in `api-gateway`
4. `NOTIFICATION_SERVICE_URL=<notification-api-url>` in `api-gateway`

Use Render internal/private networking where available.

## 6. Frontend deployment

Deploy `StreamSphereAppFrontend` as a Render Static Site.

1. Build command: `npm run build`
2. Publish directory: `dist`
3. Environment variable: `VITE_API_GATEWAY_URL=<api-gateway-public-url>`

Frontend should call only gateway, never internal service URLs directly.

## 7. Deployment order

1. Provision PostgreSQL and RabbitMQ
2. Deploy `central`
3. Deploy `notification-api`
4. Deploy `video-service`
5. Deploy `api-gateway`
6. Deploy frontend static site
7. Update CORS values on all backend services with frontend domain

## 8. Health checks and smoke tests

Health endpoints:

1. `GET /actuator/health` on each backend service

Gateway smoke routes:

1. `GET /api/v1/central/videos`
2. `POST /api/central/user/register`
3. `POST /api/central/user/login`
4. `POST /api/v1/video/upload` (auth + multipart)

## 9. Production hardening checklist

1. Use strong random `CENTRAL_SECRET_KEY`
2. Restrict `CORS_ALLOWED_ORIGINS` to exact frontend domains
3. Store secrets only in Render environment variables
4. Rotate any leaked local/dev credentials before go-live
5. Keep `JPA_DDL_AUTO=update` only for non-critical environments; use migrations for long-term production
