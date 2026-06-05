package com.ticketflow.auth;

import java.time.Instant;

import com.ticketflow.user.UserRole;

public record JwtClaims(Long userId, String email, String name, UserRole role, Instant issuedAt, Instant expiresAt) {
}

