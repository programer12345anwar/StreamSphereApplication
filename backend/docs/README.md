# StreamSphere project documentation

This folder is the maintained technical reference for StreamSphere. It is
organized for day-to-day development, production troubleshooting, revision, and
explaining the project in an interview.

## Start here

| Document | What it covers |
| --- | --- |
| [Architecture](architecture/system-architecture.md) | Services, responsibilities, data ownership, request flow, storage, security boundaries, and design trade-offs |
| [API reference](api/api-reference.md) | Gateway routes and the main REST endpoints grouped by feature |
| [Notification events](events/notification-events.md) | RabbitMQ and WebSocket notification flows, message fields, retries, and limitations |
| [Development and deployment](development-and-deployment.md) | Local prerequisites, service startup, environment variables, tests, Render, and production checks |
| [Interview and project walkthrough](interview-guide.md) | Short and long project explanations, feature walkthrough, likely questions, and honest current limitations |

The repository also has a Render-specific operational guide at
[`backend/DEPLOYMENT_RENDER.md`](../DEPLOYMENT_RENDER.md). This guide is
referenced for Render provisioning details; the docs in this folder explain the
broader system.

## Keeping these docs useful

- Treat implementation and configuration as the source of truth. Update the
  relevant page when a service boundary, endpoint, event, environment variable,
  or deployment procedure changes.
- Keep secrets, real credentials, private user information, and production
  database contents out of documentation.
- Clearly label planned work; do not describe it as implemented behavior.
- Prefer relative links between documents so this documentation remains usable
  in GitHub and in a local checkout.
