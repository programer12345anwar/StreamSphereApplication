# StreamSphere Admin

React/Vite admin console for StreamSphere, backed by the StreamSphere API Gateway.

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

The admin app fetches its API base URL at runtime from the Gateway's
`/api/config` endpoint, then sends application requests to the returned URL.
`VITE_API_CONFIG_URL` can override the bootstrap Gateway URL; it defaults to
`https://streamsphere-api-gateway.onrender.com`.

## Production Deployment

For Vercel, set the project root to `admin`. The included `vercel.json` rewrites
client-side routes such as `/login` to the SPA entry point. If the stable
Gateway bootstrap URL changes, set `VITE_API_CONFIG_URL` to the new Gateway
base URL and redeploy.

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
