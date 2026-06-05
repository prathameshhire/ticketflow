package com.ticketflow.auth.dto;

import com.ticketflow.auth.UserPrincipal;
import com.ticketflow.user.User;
import com.ticketflow.user.UserRole;

public record CurrentUserResponse(Long id, String name, String email, UserRole role) {

    public static CurrentUserResponse from(User user) {
        return new CurrentUserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    public static CurrentUserResponse from(UserPrincipal principal) {
        return new CurrentUserResponse(principal.id(), principal.name(), principal.email(), principal.role());
    }
}

