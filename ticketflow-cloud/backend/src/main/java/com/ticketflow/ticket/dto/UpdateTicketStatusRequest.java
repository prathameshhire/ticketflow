package com.ticketflow.ticket.dto;

import com.ticketflow.ticket.TicketStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateTicketStatusRequest(@NotNull TicketStatus status) {
}

