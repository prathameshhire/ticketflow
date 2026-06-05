# Local Setup

## Requirements

- Java 17
- Maven 3.9+
- Docker Desktop, only if using local PostgreSQL

## Backend With H2

H2 is the fastest local demo path.

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

With seed data:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=--app.seed.enabled=true
```

Health check:

```bash
curl http://localhost:8080/api/health
```

H2 console:

```text
http://localhost:8080/h2-console
```

JDBC URL:

```text
jdbc:h2:mem:ticketflow
```

## Backend Tests

```bash
cd backend
mvn test
```

## Local PostgreSQL

Docker Compose is for local development only.

```bash
docker compose up -d postgres
```

The current backend `dev` profile uses H2. A future local PostgreSQL profile can point Spring to:

```text
jdbc:postgresql://localhost:5432/ticketflow
```

Compose credentials:

```text
database: ticketflow
username: ticketflow
password: ticketflow_dev_password
```

## Production-Like Environment Variables

Render/Neon production settings are environment-driven:

```text
PORT
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
JWT_SECRET
ALLOWED_ORIGINS
JWT_EXPIRATION_MINUTES
ALERT_CORE_POOL_SIZE
ALERT_MAX_POOL_SIZE
ALERT_QUEUE_CAPACITY
DASHBOARD_CACHE_TTL_SECONDS
APP_SEED_ENABLED
```

For production, keep `APP_SEED_ENABLED=false`.

## Frontend Notes

Frontend implementation has not started yet. The API contract in [API.md](API.md) is intended for Angular service and model development.
