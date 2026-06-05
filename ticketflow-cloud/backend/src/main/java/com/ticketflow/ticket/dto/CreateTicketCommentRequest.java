package com.ticketflow.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketCommentRequest(
        @NotBlank
        @Size(max = 5_000)
        String body
) {
}

