# TicketFlow API

Base local URL:

```text
http://localhost:8080
```

All protected endpoints require:

```http
Authorization: Bearer <jwt>
Content-Type: application/json
```

## Health

```bash
curl http://localhost:8080/api/health
```

Response:

```json
{
  "app": "TicketFlow",
  "status": "UP",
  "timestamp": "2026-06-04T07:00:00Z"
}
```

## Authentication

### Register

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Demo Customer\",\"email\":\"demo@example.com\",\"password\":\"password123\"}"
```

Response:

```json
{
  "token": "jwt-token",
  "tokenType": "Bearer",
  "user": {
    "id": 1,
    "name": "Demo Customer",
    "email": "demo@example.com",
    "role": "CUSTOMER"
  }
}
```

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"customer@ticketflow.dev\",\"password\":\"password123\"}"
```

### Current User

```bash
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <jwt>"
```

## Tickets

### Create Ticket

Allowed: `CUSTOMER`

```bash
curl -X POST http://localhost:8080/api/tickets \
  -H "Authorization: Bearer <customer-jwt>" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"Checkout failure\",\"description\":\"Payment submit returns 500.\",\"priority\":\"URGENT\"}"
```

### List Tickets

Allowed visibility:

- `CUSTOMER`: own tickets
- `AGENT`: assigned tickets
- `ADMIN`: all tickets

Query params:

- `page`
- `size`
- `status`: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`
- `priority`: `LOW`, `MEDIUM`, `HIGH`, `URGENT`
- `assignedAgentId`
- `customerId`
- `q`: title/description search
- `sort`: `createdAt` or `priority`

```bash
curl "http://localhost:8080/api/tickets?page=0&size=20&status=OPEN&priority=URGENT&q=checkout&sort=createdAt" \
  -H "Authorization: Bearer <jwt>"
```

Response shape:

```json
{
  "content": [
    {
      "id": 1,
      "title": "Checkout failure",
      "description": "Payment submit returns 500.",
      "priority": "URGENT",
      "status": "OPEN",
      "customer": {
        "id": 4,
        "name": "Casey Customer",
        "email": "customer@ticketflow.dev",
        "role": "CUSTOMER"
      },
      "assignedAgent": null,
      "slaDueAt": "2026-06-04T11:00:00Z",
      "resolvedAt": null,
      "createdAt": "2026-06-04T07:00:00Z",
      "updatedAt": "2026-06-04T07:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

### Get Ticket

```bash
curl http://localhost:8080/api/tickets/1 \
  -H "Authorization: Bearer <jwt>"
```

### Assign Ticket

Allowed: `ADMIN`

```bash
curl -X PATCH http://localhost:8080/api/tickets/1/assign \
  -H "Authorization: Bearer <admin-jwt>" \
  -H "Content-Type: application/json" \
  -d "{\"assignedAgentId\":2}"
```

To unassign:

```json
{
  "assignedAgentId": null
}
```

### Update Status

Allowed: `ADMIN`, or assigned `AGENT`

```bash
curl -X PATCH http://localhost:8080/api/tickets/1/status \
  -H "Authorization: Bearer <agent-or-admin-jwt>" \
  -H "Content-Type: application/json" \
  -d "{\"status\":\"IN_PROGRESS\"}"
```

When status becomes `RESOLVED` or `CLOSED`, `resolvedAt` is set automatically.

### Add Comment

Allowed: any user who can view the ticket.

```bash
curl -X POST http://localhost:8080/api/tickets/1/comments \
  -H "Authorization: Bearer <jwt>" \
  -H "Content-Type: application/json" \
  -d "{\"body\":\"Investigating the payment gateway logs.\"}"
```

### List Comments

```bash
curl http://localhost:8080/api/tickets/1/comments \
  -H "Authorization: Bearer <jwt>"
```

## Alerts

Alerts are stored in PostgreSQL/H2 and created asynchronously after ticket mutations.

### List Alerts

```bash
curl http://localhost:8080/api/alerts \
  -H "Authorization: Bearer <jwt>"
```

### Mark Alert Read

```bash
curl -X PATCH http://localhost:8080/api/alerts/1/read \
  -H "Authorization: Bearer <jwt>"
```

### Mark All Alerts Read

```bash
curl -X PATCH http://localhost:8080/api/alerts/read-all \
  -H "Authorization: Bearer <jwt>"
```

## Dashboard

### Summary

```bash
curl http://localhost:8080/api/dashboard/summary \
  -H "Authorization: Bearer <jwt>"
```

Response:

```json
{
  "totalTickets": 15,
  "openCount": 5,
  "inProgressCount": 4,
  "resolvedCount": 3,
  "closedCount": 3,
  "overdueSlaCount": 3,
  "ticketsByPriority": {
    "LOW": 3,
    "MEDIUM": 5,
    "HIGH": 4,
    "URGENT": 3
  },
  "ticketsByStatus": {
    "OPEN": 5,
    "IN_PROGRESS": 4,
    "RESOLVED": 3,
    "CLOSED": 3
  },
  "agentWorkload": [
    {
      "agentId": 2,
      "agentName": "Maya Agent",
      "agentEmail": "agent@ticketflow.dev",
      "totalAssigned": 7,
      "openCount": 2,
      "inProgressCount": 3
    }
  ],
  "averageResolutionTimeHours": 2.0
}
```

## Errors

Errors use this shape:

```json
{
  "code": "ACCESS_DENIED",
  "message": "You do not have permission to access this resource.",
  "timestamp": "2026-06-04T07:00:00Z",
  "path": "/api/tickets/1"
}
```

Common codes:

- `UNAUTHORIZED`
- `ACCESS_DENIED`
- `VALIDATION_ERROR`
- `NOT_FOUND`
- `DUPLICATE_EMAIL`
- `INVALID_LOGIN`
- `INVALID_TOKEN`
- `INVALID_TICKET_OPERATION`

