package com.ticketflow.ticket.dto;

import java.time.Instant;

import com.ticketflow.ticket.Ticket;
import com.ticketflow.ticket.TicketPriority;
import com.ticketflow.ticket.TicketStatus;

public record TicketResponse(
        Long id,
        String title,
        String description,
        TicketPriority priority,
        TicketStatus status,
        UserSummaryResponse customer,
        UserSummaryResponse assignedAgent,
        Instant slaDueAt,
        Instant resolvedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getStatus(),
                UserSummaryResponse.from(ticket.getCustomer()),
                UserSummaryResponse.from(ticket.getAssignedAgent()),
                ticket.getSlaDueAt(),
                ticket.getResolvedAt(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}

