# Deployment Checklist

Use this checklist before deploying TicketFlow to Render, Vercel, and Neon.

## Local Test Checklist

- Run backend tests:

```bash
cd backend
mvn test
```

- Run frontend build with the intended API URL:

```bash
cd frontend
NG_APP_API_URL=http://localhost:8080/api npm run build
```

- Confirm generated frontend runtime config is ignored:

```bash
git check-ignore -v frontend/src/assets/env.js
```

- Start local PostgreSQL if testing PostgreSQL behavior:

```bash
docker compose up -d postgres
```

- Start backend and verify health:

```bash
curl http://localhost:8080/api/health
```

- Log in with a seeded user only when seed data is intentionally enabled.
- Confirm no real secrets are present in source-controlled files.

## Backend Render Checklist

- Render service root directory is `ticketflow-cloud/backend` for this GitHub repository.
- Build command is:

```bash
mvn clean package -DskipTests
```

- Start command is:

```bash
java -Xmx384m -jar target/*.jar
```

- Health check path is `/api/health`.
- Environment variables are set:

```text
SPRING_PROFILES_ACTIVE=prod
PORT=<provided by Render>
DATABASE_URL=<Neon JDBC URL>
DATABASE_USERNAME=<Neon username>
DATABASE_PASSWORD=<Neon password>
JWT_SECRET=<long random secret>
ALLOWED_ORIGINS=https://your-vercel-domain.vercel.app
APP_SEED_ENABLED=false
```

- If demo data is needed for the first deploy, temporarily set `APP_SEED_ENABLED=true`, deploy once, then set it back to `false`.
- Hikari maximum pool size remains `3`.
- No Render persistent disk is configured.
- No Redis, RabbitMQ, Kafka, Elasticsearch, local file uploads, or local filesystem persistence is required.
- `/api/health` is public and returns `UP`.

## Frontend Vercel Checklist

- Vercel project root directory is `ticketflow-cloud/frontend` for this GitHub repository.
- Build command is:

```bash
node scripts/generate-env.js && npm run build
```

- Output directory is:

```text
dist/ticketflow-frontend/browser
```

- Environment variable is set:

```text
NG_APP_API_URL=https://your-render-backend.onrender.com/api
```

- `frontend/vercel.json` exists and rewrites all routes to `/index.html`.
- Refreshing `/dashboard`, `/tickets`, `/tickets/new`, `/tickets/:id`, and `/alerts` works after deployment.
- The deployed Vercel domain is included in Render `ALLOWED_ORIGINS`.

## Neon Checklist

- Neon PostgreSQL database exists.
- Render `DATABASE_URL` uses JDBC format:

```text
jdbc:postgresql://<host>/<database>?sslmode=require
```

- Render `DATABASE_USERNAME` and `DATABASE_PASSWORD` match the Neon role credentials.
- Flyway migrations run successfully on first backend startup.
- `flyway_schema_history` contains migration version `1`.
- Persistent production data is stored in Neon only.

## Final Smoke Test Checklist

- Open the Vercel frontend URL.
- Register or log in.
- Confirm `/api/auth/me` succeeds through the UI.
- Open Dashboard and confirm ticket counts load.
- Create a customer ticket.
- As admin, assign the ticket to an agent.
- As admin or assigned agent, update ticket status.
- Add and view comments.
- Confirm alerts appear and can be marked read.
- Confirm direct refresh works on protected Angular routes.
- Confirm Render `/api/health` returns `UP`.
