# Docker Setup

Docker is for local development only. Production deployment should use Render for the backend, Vercel for the frontend, and Neon PostgreSQL for the database.

## Start PostgreSQL Only

From the repository root:

```bash
docker compose up -d postgres
docker compose ps
```

Local PostgreSQL connection:

```text
Host: localhost
Port: 5432
Database: ticketflow
User: ticketflow
Password: ticketflow_dev_password
```

If port `5432` is already in use locally:

```bash
POSTGRES_PORT=15432 docker compose up -d postgres
```

The only named Docker volume is `ticketflow-postgres-data`, which stores local PostgreSQL data.

## Run Backend Locally Against Docker PostgreSQL

Start Postgres:

```bash
docker compose up -d postgres
```

Run the backend from `backend` using the local-only Docker profile:

```bash
cd backend
SPRING_PROFILES_ACTIVE=docker \
DATABASE_URL=jdbc:postgresql://localhost:5432/ticketflow \
DATABASE_USERNAME=ticketflow \
DATABASE_PASSWORD=ticketflow_dev_password \
JWT_SECRET=local-dev-placeholder-not-a-real-secret-change-me-32chars \
ALLOWED_ORIGINS=http://localhost:4200,http://127.0.0.1:4200 \
APP_SEED_ENABLED=true \
mvn spring-boot:run
```

If you started Postgres with a custom host port, use that port in `DATABASE_URL`, for example `jdbc:postgresql://localhost:15432/ticketflow`.

PowerShell:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE = "docker"
$env:DATABASE_URL = "jdbc:postgresql://localhost:5432/ticketflow"
$env:DATABASE_USERNAME = "ticketflow"
$env:DATABASE_PASSWORD = "ticketflow_dev_password"
$env:JWT_SECRET = "local-dev-placeholder-not-a-real-secret-change-me-32chars"
$env:ALLOWED_ORIGINS = "http://localhost:4200,http://127.0.0.1:4200"
$env:APP_SEED_ENABLED = "true"
mvn spring-boot:run
```

Check the backend:

```bash
curl http://localhost:8080/api/health
```

## Run Backend In Docker

From the repository root:

```bash
docker compose --profile backend up --build
```

This starts PostgreSQL and the Spring Boot backend. The backend uses the local-only `docker` profile, a small Hikari pool, seeded demo data, and memory-conscious JVM flags from the Dockerfile.

If `8080` is already in use:

```bash
BACKEND_PORT=18080 docker compose --profile backend up --build
```

## Run The Whole Stack Locally

From the repository root:

```bash
docker compose --profile stack up --build
```

URLs:

```text
Frontend: http://localhost:4200
Backend:  http://localhost:8080
Health:   http://localhost:8080/api/health
```

The frontend Docker image is optional and exists only for local stack testing. Vercel remains the primary frontend deployment path.

If the standard ports are already in use:

```bash
POSTGRES_PORT=15432 BACKEND_PORT=18080 FRONTEND_PORT=14200 docker compose --profile stack up --build
```

## Reset Local Data

Stop containers and remove the local PostgreSQL volume:

```bash
docker compose down -v
```

Start fresh:

```bash
docker compose up -d postgres
```

This deletes only Docker's local PostgreSQL data volume. It does not affect Neon, Render, Vercel, or any production data.
