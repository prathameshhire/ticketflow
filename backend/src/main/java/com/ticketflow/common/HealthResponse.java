package com.ticketflow.common;

import java.time.Instant;

public record HealthResponse(String app, String status, Instant timestamp) {
}

