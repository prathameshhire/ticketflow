package com.ticketflow.ticket.dto;

import com.ticketflow.user.User;
import com.ticketflow.user.UserRole;

public record UserSummaryResponse(Long id, String name, String email, UserRole role) {

    public static UserSummaryResponse from(User user) {
        if (user == null) {
            return null;
        }
        return new UserSummaryResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}

