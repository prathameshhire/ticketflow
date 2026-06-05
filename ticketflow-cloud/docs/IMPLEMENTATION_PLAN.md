# TicketFlow Implementation Plan

## 1. Product Scope

TicketFlow will be an incident ticketing platform for small support or operations teams. The first production-ready version should support authenticated users, ticket lifecycle management, assignment workflows, alert notifications, and lightweight analytics without requiring paid infrastructure add-ons.

Primary users:

- Agents who triage, update, and resolve incidents
- Managers who monitor workload, SLA risk, and resolution trends
- Admins who manage users, roles, categories, priorities, and alert rules

## 2. Deployment Architecture

```text
Angular on Vercel
        |
        | HTTPS JSON API
        v
Spring Boot Web Service on Render
        |
        | JDBC
        v
Neon PostgreSQL
```

Local development:

- `docker-compose.yml` runs PostgreSQL only.
- Backend runs locally with the `local` Spring profile against Docker PostgreSQL.
- Optional `h2` profile runs the backend without Docker for quick demos.
- Frontend runs locally with Angular dev server and proxies API calls to the backend.

Production:

- Vercel serves the Angular build.
- Render runs the Spring Boot API as a stateless Web Service.
- Neon stores all persistent data.
- No Render disks or paid managed Render databases are required.

## 3. Monorepo Structure

```text
ticketflow-cloud/
  backend/
    pom.xml
    src/main/java/com/ticketflow/
    src/main/resources/
    src/test/java/com/ticketflow/
  frontend/
    package.json
    angular.json
    src/app/
  docs/
    IMPLEMENTATION_PLAN.md
  docker-compose.yml
  README.md
  AGENTS.md
```

The current backend scaffold includes Spring Boot startup, profile configuration, package structure, security baseline, tests, and health endpoints. Domain application code will be added in later implementation steps.

## 4. Backend Plan

### Framework and Libraries

- Java 21 or Java 17, depending on target compatibility
- Spring Boot with Spring MVC
- Spring Data JPA
- Spring Security
- Bean Validation
- Flyway
- PostgreSQL JDBC driver
- H2 for demo profile only
- JUnit 5, Mockito, Spring Boot Test, Testcontainers where practical

### Planned Backend Modules

- `user`: users, roles, authentication-facing profile data, admin user management
- `ticket`: incident tickets, comments, assignments, priorities, categories, status transitions
- `alert`: alert rules, notification preferences, alert events, delivery status
- `analytics`: ticket counts, SLA risk summaries, agent workload, resolution metrics
- `audit`: database-backed audit trail for meaningful ticket and admin actions
- `common`: shared exceptions, API responses, pagination, time utilities, validation helpers
- `config`: security, CORS, async executor, persistence, OpenAPI configuration if added

### Service Boundaries

Design modular service classes so responsibilities stay clear:

- `TicketCommandService`: create, update, assign, transition, and comment on tickets
- `TicketQueryService`: filtering, pagination, detail views, and saved views
- `UserService`: user profile and admin user operations
- `RoleService`: role checks and role administration
- `AlertRuleService`: CRUD and evaluation of alert rules
- `AlertDispatchService`: async alert delivery workflow
- `AnalyticsService`: dashboard metrics and aggregate views
- `AuditService`: durable audit event creation

Use Java Streams and Collections for mapping, grouping, filtering, and aggregation where they keep the code readable.

Use Spring async or `CompletableFuture` for:

- alert dispatch after ticket changes
- analytics refresh tasks that should not block ticket commands
- non-critical notification status updates

Avoid introducing external brokers. Durable state should remain in PostgreSQL.

## 5. Data Model Plan

Initial PostgreSQL tables:

- `users`
- `roles`
- `user_roles`
- `tickets`
- `ticket_comments`
- `ticket_status_history`
- `ticket_assignments`
- `ticket_categories`
- `alert_rules`
- `alert_events`
- `notification_preferences`
- `audit_events`

Suggested ticket fields:

- `id`
- `ticket_number`
- `title`
- `description`
- `status`
- `priority`
- `category_id`
- `reporter_id`
- `assignee_id`
- `created_at`
- `updated_at`
- `due_at`
- `resolved_at`

Store attachments as external URLs only if a future free-compatible object storage option is selected. Do not store uploaded files on Render or in the local filesystem.

## 6. API Plan

Initial REST API groups:

- `POST /api/auth/login`
- `GET /api/users/me`
- `GET /api/users`
- `POST /api/users`
- `GET /api/tickets`
- `POST /api/tickets`
- `GET /api/tickets/{id}`
- `PATCH /api/tickets/{id}`
- `POST /api/tickets/{id}/comments`
- `POST /api/tickets/{id}/assign`
- `POST /api/tickets/{id}/transition`
- `GET /api/alerts/rules`
- `POST /api/alerts/rules`
- `GET /api/analytics/summary`
- `GET /api/analytics/workload`
- `GET /api/analytics/sla-risk`

Use DTOs for every request and response. Do not expose JPA entities directly.

Use pagination for ticket and user lists from the first implementation pass.

## 7. Frontend Plan

### Framework

- Angular
- Angular Router
- Reactive Forms
- HttpClient
- Feature modules or standalone feature areas by domain
- Environment-based API base URL

### Planned Views

- Login
- Dashboard
- Ticket list with filters and sorting
- Ticket detail with comments and status history
- Ticket creation form
- Assignment and status transition controls
- Alert rules management
- Analytics dashboard
- Admin users and roles

### Frontend Structure

```text
src/app/
  core/
    api/
    auth/
    guards/
    interceptors/
  shared/
    components/
    pipes/
    models/
  features/
    dashboard/
    tickets/
    alerts/
    analytics/
    admin/
```

The UI should be operational and task-focused: dense enough for repeated use, clear for scanning, and restrained rather than marketing-oriented.

## 8. Security Plan

First implementation option:

- JWT-based stateless authentication
- BCrypt password hashing
- Role-based authorization with `ADMIN`, `MANAGER`, and `AGENT`
- CORS restricted to configured frontend origins
- Validation on all write endpoints
- No secrets committed to source control

Future option:

- Integrate an external auth provider if needed, while keeping the backend stateless.

## 9. Configuration Plan

Backend environment variables:

- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `JWT_SECRET`
- `CORS_ALLOWED_ORIGINS`
- `SPRING_PROFILES_ACTIVE`

Frontend environment variables:

- `NG_APP_API_BASE_URL` or Angular environment equivalent

Profiles:

- `local`: PostgreSQL from Docker Compose
- `prod`: Neon PostgreSQL
- `h2`: in-memory H2 for quick demo
- `test`: test configuration

## 10. Local Development Plan

1. Start PostgreSQL:

   ```bash
   docker compose up -d postgres
   ```

2. Run backend with the local profile.

3. Run frontend dev server.

4. Use seeded demo users and tickets after seed migrations or a development data loader is added.

Docker Compose must not become the production deployment path.

## 11. Deployment Plan

### Backend on Render

- Build with Maven.
- Run the Spring Boot jar as a Web Service.
- Configure Neon database environment variables.
- Set `SPRING_PROFILES_ACTIVE=prod`.
- Keep service stateless.
- Use health endpoint for Render health checks.

### Frontend on Vercel

- Build Angular app.
- Configure API base URL to point at the Render backend.
- Ensure routing fallback works for Angular routes.

### Database on Neon

- Create PostgreSQL database.
- Configure SSL-compatible JDBC settings as needed.
- Run Flyway migrations on backend startup or through a controlled release command.

## 12. Testing Plan

Backend:

- Unit tests for service rules
- Controller tests for request validation and response contracts
- Repository tests for query behavior
- Security tests for role access
- Integration smoke tests for ticket lifecycle

Frontend:

- Component tests for ticket list, ticket detail, and forms
- Service tests for API clients
- Guard and interceptor tests
- End-to-end smoke tests later if the stack adds a browser testing setup

CI plan:

- Backend test job
- Frontend build and test job
- Optional lint and formatting jobs

## 13. Implementation Phases

### Phase 1: Project Bootstrapping

- Create Spring Boot backend with Maven.
- Create Angular frontend.
- Add local PostgreSQL Docker Compose wiring.
- Add environment templates.
- Add basic health endpoint.
- Add README setup instructions.

### Phase 2: Persistence and Domain Foundation

- Add Flyway migrations.
- Add core entities and repositories.
- Add DTO mapping conventions.
- Add global exception handling.
- Add validation strategy.

### Phase 3: Users and Security

- Add users, roles, and password hashing.
- Add authentication endpoints.
- Add JWT filter and role-based authorization.
- Add current-user endpoint.
- Add seeded local demo users.

### Phase 4: Ticket Workflow

- Add ticket creation, list, detail, update, assignment, comments, and status transitions.
- Add ticket history and audit events.
- Add filters for status, priority, assignee, reporter, date ranges, and text search using PostgreSQL-friendly queries.

### Phase 5: Alerts

- Add alert rules.
- Create alert events after relevant ticket changes.
- Dispatch alert workflows asynchronously with Spring async or `CompletableFuture`.
- Persist delivery status in PostgreSQL.

### Phase 6: Analytics

- Add aggregate queries for dashboard metrics.
- Add agent workload and SLA risk endpoints.
- Use Streams for final DTO shaping and grouping where readable.
- Consider cached database summary rows only if query load requires it.

### Phase 7: Angular Application

- Add authentication flow.
- Add dashboard shell.
- Add ticket list and detail workflows.
- Add ticket creation and update forms.
- Add alerts and analytics views.
- Add role-aware navigation and guards.

### Phase 8: Deployment Hardening

- Add production profile configuration.
- Add CORS environment configuration.
- Add Vercel rewrite configuration.
- Add Render deployment notes.
- Validate Neon connection settings.
- Add health checks and basic operational logging.

### Phase 9: Polish and Resume-Ready Evidence

- Add architecture diagram.
- Add screenshots.
- Add realistic seed data.
- Add final deployment documentation.
- Add project highlights that accurately reflect implemented modular Java services, async workflows, PostgreSQL persistence, and Angular frontend.

## 14. Non-Goals

- No Redis
- No RabbitMQ
- No Kafka
- No Elasticsearch
- No paid Render database
- No Render persistent disks
- No local file upload persistence
- No production Docker Compose deployment
- No heavyweight microservice split

## 15. Initial Done Criteria

The planning scaffold is complete when:

- The requested monorepo structure exists.
- `README.md` explains the project, targets, constraints, and current status.
- `AGENTS.md` documents commands, constraints, standards, and done criteria.
- `docs/IMPLEMENTATION_PLAN.md` gives a detailed phased implementation plan.
- No full application code has been implemented yet.
