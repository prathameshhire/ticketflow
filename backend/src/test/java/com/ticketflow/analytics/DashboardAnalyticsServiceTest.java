package com.ticketflow.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import com.ticketflow.support.TestFixtures;
import com.ticketflow.analytics.dto.DashboardSummaryResponse;
import com.ticketflow.ticket.Ticket;
import com.ticketflow.ticket.TicketPriority;
import com.ticketflow.ticket.TicketRepository;
import com.ticketflow.ticket.TicketStatus;
import com.ticketflow.user.User;
import com.ticketflow.user.UserRole;

@ExtendWith(MockitoExtension.class)
class DashboardAnalyticsServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private DashboardSummaryCache dashboardSummaryCache;

    @Test
    void summaryGroupsTicketsByStatusPriorityAndAgentWorkload() {
        User admin = TestFixtures.user(1L, UserRole.ADMIN);
        User customer = TestFixtures.user(2L, UserRole.CUSTOMER);
        User agent = TestFixtures.user(3L, UserRole.AGENT);
        List<Ticket> tickets = List.of(
                TestFixtures.ticket(10L, customer, agent, TicketPriority.URGENT, TicketStatus.OPEN),
                TestFixtures.ticket(11L, customer, agent, TicketPriority.HIGH, TicketStatus.IN_PROGRESS),
                TestFixtures.ticket(12L, customer, agent, TicketPriority.LOW, TicketStatus.RESOLVED)
        );

        when(dashboardSummaryCache.getOrCompute(any(String.class), anySupplier())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(1);
            return supplier.get();
        });
        when(ticketRepository.findAll(anyTicketSpecification())).thenReturn(tickets);

        DashboardAnalyticsService service = new DashboardAnalyticsService(ticketRepository, dashboardSummaryCache);
        var summary = service.summary(TestFixtures.principal(admin));

        assertThat(summary.totalTickets()).isEqualTo(3);
        assertThat(summary.openCount()).isEqualTo(1);
        assertThat(summary.inProgressCount()).isEqualTo(1);
        assertThat(summary.resolvedCount()).isEqualTo(1);
        assertThat(summary.ticketsByPriority().get(TicketPriority.URGENT)).isEqualTo(1);
        assertThat(summary.agentWorkload()).hasSize(1);
        assertThat(summary.agentWorkload().get(0).totalAssigned()).isEqualTo(3);
        assertThat(summary.averageResolutionTimeHours()).isGreaterThan(0);
        verify(dashboardSummaryCache).getOrCompute(any(String.class), anySupplier());
    }

    private Supplier<DashboardSummaryResponse> anySupplier() {
        return org.mockito.ArgumentMatchers.any();
    }

    private Specification<Ticket> anyTicketSpecification() {
        return org.mockito.ArgumentMatchers.any();
    }
}
