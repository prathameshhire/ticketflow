# Vercel Deployment

TicketFlow frontend deploys to Vercel as an Angular static site. Render remains the backend host, and Neon remains the external PostgreSQL database.

## Project Settings

Create a Vercel project from this repository and use:

```text
Root directory: ticketflow-cloud/frontend
Build command: node scripts/generate-env.js && npm run build
Output directory: dist/ticketflow-frontend/browser
```

If `ticketflow-cloud` itself is imported as the repository root, use `frontend` as the Vercel root directory instead. The output directory comes from the Angular application builder configured in `frontend/angular.json`.

## Environment Variable

Set this in Vercel:

```text
NG_APP_API_URL=https://your-render-backend.onrender.com/api
```

The build command writes `src/assets/env.js`, and `index.html` loads it before Angular starts. The app reads `window.__env.API_BASE_URL` at runtime.

## SPA Routing

`frontend/vercel.json` rewrites all routes to `index.html`, so direct visits to Angular routes such as `/dashboard`, `/tickets`, and `/alerts` work after deployment.

## Deployment Notes

- Docker is not required for Vercel.
- No paid UI kit or external asset host is required.
- The frontend stores the JWT in browser local storage and sends it with API requests through the Angular HTTP interceptor.
- Make sure the deployed Vercel URL is included in the backend `ALLOWED_ORIGINS` Render environment variable.
