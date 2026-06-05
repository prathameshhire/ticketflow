package com.ticketflow.alert.dto;

import java.time.Instant;

import com.ticketflow.alert.Alert;
import com.ticketflow.alert.AlertType;
import com.ticketflow.ticket.dto.UserSummaryResponse;

public record AlertResponse(
        Long id,
        UserSummaryResponse recipient,
        AlertType type,
        String message,
        boolean readFlag,
        Instant createdAt
) {

    public static AlertResponse from(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                UserSummaryResponse.from(alert.getRecipient()),
                alert.getType(),
                alert.getMessage(),
                alert.isReadFlag(),
                alert.getCreatedAt()
        );
    }
}

