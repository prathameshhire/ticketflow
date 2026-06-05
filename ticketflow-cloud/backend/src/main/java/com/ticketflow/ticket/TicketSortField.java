package com.ticketflow.ticket;

public enum TicketSortField {
    CREATED_AT("createdAt"),
    PRIORITY("priority");

    private final String property;

    TicketSortField(String property) {
        this.property = property;
    }

    public String property() {
        return property;
    }

    public static TicketSortField from(String value) {
        if (value == null || value.isBlank()) {
            return CREATED_AT;
        }

        return switch (value.trim().toLowerCase()) {
            case "createdat", "created_at" -> CREATED_AT;
            case "priority" -> PRIORITY;
            default -> throw new InvalidTicketOperationException("Sort must be createdAt or priority.");
        };
    }
}

