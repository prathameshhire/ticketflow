# Neon Setup

TicketFlow uses Neon as the external PostgreSQL database for production. The database is not hosted on Render and does not use local Docker storage.

## Create Database

1. Create or open a Neon project.
2. Create a PostgreSQL database named `ticketflow`, or use the default database if preferred.
3. Open the Neon connection details for the target branch.
4. Choose the Java/JDBC connection string format.

## Render Environment Values

Copy the Neon values into Render:

```text
DATABASE_URL=jdbc:postgresql://<host>/<database>?sslmode=require
DATABASE_USERNAME=<Neon role/user>
DATABASE_PASSWORD=<Neon password>
```

Neon may show a single connection string containing the username and password. For TicketFlow, split it into:

- `DATABASE_URL`: the JDBC URL without embedded credentials when possible
- `DATABASE_USERNAME`: the Neon username or role
- `DATABASE_PASSWORD`: the Neon password

Example shape:

```text
DATABASE_URL=jdbc:postgresql://ep-example.us-east-2.aws.neon.tech/ticketflow?sslmode=require
DATABASE_USERNAME=ticketflow_owner
DATABASE_PASSWORD=<copied secret>
```

## Schema Creation

The backend `prod` profile enables Flyway migrations and sets Hibernate to validate the schema. On first startup, Flyway creates the PostgreSQL tables, indexes, and foreign keys. Hibernate then validates that the entity model matches the database.

## Production Boundary

Use Neon for all persistent production data. Docker PostgreSQL is only for local development, and H2 is only for quick local demos.
