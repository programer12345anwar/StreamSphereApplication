# StreamSphere Frontend

React/Vite frontend for StreamSphere, a video-sharing application backed by the StreamSphere API Gateway.

## Tech Stack

- React
- Vite
- Tailwind CSS
- shadcn/ui
- Vitest

## Local Development

```sh
npm install
npm run dev
```

The frontend fetches its API base URL at runtime from the Gateway's
`/api/config` endpoint, then sends application requests to the returned URL.
`VITE_API_CONFIG_URL` can override the bootstrap Gateway URL; it defaults to
`https://streamsphere-api-gateway.onrender.com`.

Video uploads request short-lived authorization from the Video service and send
the file directly to ImageKit. Configure the ImageKit URL endpoint and keys on
the Video service, and allow the deployed frontend origin in the ImageKit
account's upload security settings. Video metadata is persisted only after
ImageKit confirms the uploaded file.

## Production Deployment

For Vercel, set the project root to `frontend`. If the stable Gateway bootstrap
URL changes, set `VITE_API_CONFIG_URL` to the new Gateway base URL and redeploy.

Then use the standard commands:

```sh
npm install
npm run build
```

## Quality Checks

```sh
npm run lint
npm run test
npm run build
```
