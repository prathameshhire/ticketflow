package com.ticketflow.analytics.dto;

public record AgentWorkloadResponse(
        Long agentId,
        String agentName,
        String agentEmail,
        long totalAssigned,
        long openCount,
        long inProgressCount
) {
}

