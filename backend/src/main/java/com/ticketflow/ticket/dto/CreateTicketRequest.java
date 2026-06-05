package com.ticketflow.ticket.dto;

import com.ticketflow.ticket.TicketPriority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank
        @Size(max = 180)
        String title,

        @NotBlank
        @Size(max = 10_000)
        String description,

        @NotNull
        TicketPriority priority
) {
}

