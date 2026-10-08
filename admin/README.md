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

By default, local development uses `http://localhost:8080` as the API Gateway URL. To override it, create a `.env` file:

```sh
VITE_API_GATEWAY_URL=http://localhost:8080
```

## Production Deployment

For Vercel, set the project root to `admin` and configure:

```sh
VITE_API_GATEWAY_URL=https://your-api-gateway.example.com
```

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
