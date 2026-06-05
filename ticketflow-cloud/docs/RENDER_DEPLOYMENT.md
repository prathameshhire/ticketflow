# Render Deployment

TicketFlow backend deploys to Render as a lightweight Web Service. Production data should live in external Neon PostgreSQL, not on Render disks or paid Render databases.

## Service Settings

Create a new Render Web Service from this repository.

```text
Root directory: ticketflow-cloud/backend
Build command: mvn clean package -DskipTests
Start command: java -Xmx384m -jar target/*.jar
Health check path: /api/health
```

If `ticketflow-cloud` itself is imported as the repository root, use `backend` as the Render root directory instead.

Use Java 17. The backend Dockerfile is for local development and is not required for Render.

## Environment Variables

Set these in Render:

```text
SPRING_PROFILES_ACTIVE=prod
PORT=<provided by Render>
DATABASE_URL=<Neon JDBC URL>
DATABASE_USERNAME=<Neon username>
DATABASE_PASSWORD=<Neon password>
JWT_SECRET=<long random secret>
ALLOWED_ORIGINS=https://your-vercel-domain.vercel.app
APP_SEED_ENABLED=true
```

After the initial demo seed data is created, `APP_SEED_ENABLED` can be changed to `false` if desired. The seed runner is idempotent for the demo users and skips ticket creation once tickets already exist.

## Backend Config Checklist

The backend is configured for Render constraints:

- `server.port=${PORT:8080}` so Render can inject the runtime port.
- Hikari maximum pool size is `3` in production.
- `/api/health` is public and suitable for Render health checks.
- JPA open-in-view is disabled.
- Flyway creates the PostgreSQL schema, while Hibernate validates it in `prod`.
- Persistent app data is stored in PostgreSQL only.
- No Redis, RabbitMQ, Kafka, Elasticsearch, Render persistent disks, local file uploads, or paid Render database features are required.
- Alert fan-out uses a small in-process async executor; it is not an external worker or durable local-state process.

## First Deploy Flow

1. Create the Neon database and copy its JDBC URL and credentials.
2. Create the Render Web Service with root directory `backend`.
3. Add the environment variables above.
4. Deploy.
5. Open `https://your-render-backend.onrender.com/api/health`.
6. Confirm the frontend Vercel domain is listed in `ALLOWED_ORIGINS`.

For the first deploy, keep `APP_SEED_ENABLED=true` if you want demo accounts and sample tickets. For a cleaner production-like database, set it to `false`.
