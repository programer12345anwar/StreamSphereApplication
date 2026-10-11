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

The frontend sends API requests to the live API Gateway at
`https://streamsphere-api-gateway.onrender.com`, configured in the API client.

## Production Deployment

For Vercel, set the project root to `frontend`. No API URL environment
variable is required; the live gateway URL is included in the production build.

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
