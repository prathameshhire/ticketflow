# AGENTS.md

## Repo Layout

```text
ticketflow-cloud/
  backend/                 Spring Boot / Spring MVC API
  frontend/                Angular client for Vercel
  docs/                    Architecture, plans, and deployment notes
  docker-compose.yml       Local development services only
  README.md                Project overview
  AGENTS.md                Contributor and agent guide
```

## Backend Commands

Run backend commands from `ticketflow-cloud/backend`.

Planned commands:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
mvn test
mvn package
```

Planned profiles:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## Frontend Commands

Run frontend commands from `ticketflow-cloud/frontend`.

Planned commands:

```bash
npm install
npm start
npm run build
npm test
```

## Testing Commands

Planned root-level verification flow:

```bash
docker compose up -d postgres
cd backend && mvn test
cd ../frontend && npm test
```

Backend testing should include unit tests for services, repository integration tests where useful, controller tests for API contracts, and security tests for authorization boundaries.

Frontend testing should include component tests, service tests, route guard tests, and focused workflow tests for ticket creation, assignment, filtering, and analytics views.

## Deployment Constraints

- Frontend deploys to Vercel as an Angular static build.
- Backend deploys to Render as a Web Service.
- Production database is Neon PostgreSQL.
- Docker Compose is for local development only.
- Optional H2 profile is for quick demos only and must not be treated as production-like storage.
- Do not use Redis, RabbitMQ, Kafka, Elasticsearch, paid Render databases, Render persistent disks, local file uploads, or services requiring paid Render features.
- Store all persistent production data in PostgreSQL.
- Keep the backend lightweight enough for a small Render Web Service.

## Coding Standards

- Prefer modular Java services with clear boundaries for tickets, users, alerts, and analytics.
- Keep controllers thin; put business rules in service classes.
- Use DTOs for API input and output instead of exposing persistence entities directly.
- Use Java Streams and Collections for readable in-memory transformations.
- Use `CompletableFuture`, `ExecutorService`, or Spring async only where concurrency improves responsiveness without adding infrastructure.
- Use database-backed workflows instead of in-memory queues for durable state.
- Prefer Flyway migrations for schema changes.
- Keep Angular features organized by domain.
- Avoid local filesystem persistence and local file upload flows.
- Keep configuration environment-driven for deployability.

## Done Criteria

A task is done when:

- The requested scope is implemented without unrelated refactors.
- Backend code compiles and relevant tests pass.
- Frontend code builds and relevant tests pass.
- New environment variables are documented.
- Database changes include migrations.
- Deployment constraints remain satisfied.
- Documentation is updated when behavior or setup changes.
