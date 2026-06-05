# Seed Users

Seed data only runs when `app.seed.enabled=true`.

For local H2:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=--app.seed.enabled=true
```

Demo credentials:

| Role | Email | Password |
| --- | --- | --- |
| Admin | `admin@ticketflow.dev` | `password123` |
| Agent | `agent@ticketflow.dev` | `password123` |
| Agent | `agent2@ticketflow.dev` | `password123` |
| Customer | `customer@ticketflow.dev` | `password123` |

Seed data includes:

- 4 users
- 15 sample tickets across `OPEN`, `IN_PROGRESS`, `RESOLVED`, and `CLOSED`
- All priorities: `LOW`, `MEDIUM`, `HIGH`, `URGENT`
- Ticket assignments for both agents
- Comments from customers and agents
- SLA due dates, including overdue examples
- Alerts for assignments, status changes, and SLA demo context

The seed runner is idempotent for users and skips sample ticket creation when tickets already exist.

