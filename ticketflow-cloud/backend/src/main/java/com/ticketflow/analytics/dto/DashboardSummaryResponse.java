package com.ticketflow.analytics.dto;

import java.util.List;
import java.util.Map;

import com.ticketflow.ticket.TicketPriority;
import com.ticketflow.ticket.TicketStatus;

public record DashboardSummaryResponse(
        long totalTickets,
        long openCount,
        long inProgressCount,
        long resolvedCount,
        long closedCount,
        long overdueSlaCount,
        Map<TicketPriority, Long> ticketsByPriority,
        Map<TicketStatus, Long> ticketsByStatus,
        List<AgentWorkloadResponse> agentWorkload,
        double averageResolutionTimeHours
) {
}

