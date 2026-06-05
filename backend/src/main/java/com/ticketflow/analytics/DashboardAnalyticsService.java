package com.ticketflow.analytics;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ticketflow.analytics.dto.AgentWorkloadResponse;
import com.ticketflow.analytics.dto.DashboardSummaryResponse;
import com.ticketflow.auth.UserPrincipal;
import com.ticketflow.ticket.Ticket;
import com.ticketflow.ticket.TicketPriority;
import com.ticketflow.ticket.TicketRepository;
import com.ticketflow.ticket.TicketSpecifications;
import com.ticketflow.ticket.TicketStatus;
import com.ticketflow.user.User;

@Service
public class DashboardAnalyticsService {

    private final TicketRepository ticketRepository;
    private final DashboardSummaryCache dashboardSummaryCache;

    public DashboardAnalyticsService(
            TicketRepository ticketRepository,
            DashboardSummaryCache dashboardSummaryCache
    ) {
        this.ticketRepository = ticketRepository;
        this.dashboardSummaryCache = dashboardSummaryCache;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(UserPrincipal principal) {
        String cacheKey = principal.role() + ":" + principal.id();
        return dashboardSummaryCache.getOrCompute(cacheKey, () -> computeSummary(principal));
    }

    private DashboardSummaryResponse computeSummary(UserPrincipal principal) {
        Specification<Ticket> specification = TicketSpecifications.visibleTo(principal);
        List<Ticket> tickets = ticketRepository.findAll(specification);
        Instant now = Instant.now();

        Map<TicketStatus, Long> byStatus = enumCountMap(
                TicketStatus.class,
                tickets.stream().collect(Collectors.groupingBy(Ticket::getStatus, () -> new EnumMap<>(TicketStatus.class), Collectors.counting()))
        );
        Map<TicketPriority, Long> byPriority = enumCountMap(
                TicketPriority.class,
                tickets.stream().collect(Collectors.groupingBy(Ticket::getPriority, () -> new EnumMap<>(TicketPriority.class), Collectors.counting()))
        );

        long overdueSlaCount = tickets.stream()
                .filter(ticket -> ticket.getSlaDueAt() != null && ticket.getSlaDueAt().isBefore(now))
                .filter(ticket -> ticket.getStatus() != TicketStatus.RESOLVED && ticket.getStatus() != TicketStatus.CLOSED)
                .count();

        double averageResolutionTimeHours = tickets.stream()
                .filter(ticket -> ticket.getCreatedAt() != null && ticket.getResolvedAt() != null)
                .mapToDouble(ticket -> Duration.between(ticket.getCreatedAt(), ticket.getResolvedAt()).toMinutes() / 60.0)
                .average()
                .orElse(0.0);

        List<AgentWorkloadResponse> agentWorkload = tickets.stream()
                .filter(ticket -> ticket.getAssignedAgent() != null)
                .collect(Collectors.groupingBy(ticket -> ticket.getAssignedAgent().getId(), Collectors.toList()))
                .values()
                .stream()
                .map(this::toWorkload)
                .sorted(Comparator.comparing(AgentWorkloadResponse::totalAssigned).reversed()
                        .thenComparing(AgentWorkloadResponse::agentName))
                .toList();

        return new DashboardSummaryResponse(
                tickets.size(),
                byStatus.get(TicketStatus.OPEN),
                byStatus.get(TicketStatus.IN_PROGRESS),
                byStatus.get(TicketStatus.RESOLVED),
                byStatus.get(TicketStatus.CLOSED),
                overdueSlaCount,
                byPriority,
                byStatus,
                agentWorkload,
                averageResolutionTimeHours
        );
    }

    private AgentWorkloadResponse toWorkload(List<Ticket> groupedTickets) {
        User agent = groupedTickets.get(0).getAssignedAgent();
        long totalAssigned = groupedTickets.size();
        long openCount = groupedTickets.stream()
                .filter(ticket -> ticket.getStatus() == TicketStatus.OPEN)
                .count();
        long inProgressCount = groupedTickets.stream()
                .filter(ticket -> ticket.getStatus() == TicketStatus.IN_PROGRESS)
                .count();

        return new AgentWorkloadResponse(
                agent.getId(),
                agent.getName(),
                agent.getEmail(),
                totalAssigned,
                openCount,
                inProgressCount
        );
    }

    private <E extends Enum<E>> Map<E, Long> enumCountMap(Class<E> enumType, Map<E, Long> counts) {
        EnumMap<E, Long> result = new EnumMap<>(enumType);
        Arrays.stream(enumType.getEnumConstants())
                .forEach(value -> result.put(value, counts.getOrDefault(value, 0L)));
        return result;
    }
}
