package com.ticketflow.auth.dto;

public record AuthResponse(String token, String tokenType, CurrentUserResponse user) {
}

