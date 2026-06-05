package com.ticketflow.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ticketflow.alert.AlertWorkflowService;
import com.ticketflow.analytics.DashboardSummaryCache;
import com.ticketflow.support.TestFixtures;
import com.ticketflow.ticket.dto.AssignTicketRequest;
import com.ticketflow.ticket.dto.CreateTicketRequest;
import com.ticketflow.ticket.dto.UpdateTicketStatusRequest;
import com.ticketflow.user.User;
import com.ticketflow.user.UserRepository;
import com.ticketflow.user.UserRole;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketCommentRepository ticketCommentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TicketAuthorizationService ticketAuthorizationService;

    @Mock
    private AlertWorkflowService alertWorkflowService;

    @Mock
    private DashboardSummaryCache dashboardSummaryCache;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(
                ticketRepository,
                ticketCommentRepository,
                userRepository,
                ticketAuthorizationService,
                alertWorkflowService,
                dashboardSummaryCache
        );
    }

    @Test
    void createTicketCalculatesSlaAndInvalidatesDashboardCache() {
        User customer = TestFixtures.user(1L, UserRole.CUSTOMER);
        when(ticketAuthorizationService.canCreateTicket(TestFixtures.principal(customer))).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            ReflectionTestUtils.setField(ticket, "id", 10L);
            return ticket;
        });

        var response = ticketService.createTicket(
                TestFixtures.principal(customer),
                new CreateTicketRequest("Urgent issue", "Production outage", TicketPriority.URGENT)
        );

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.slaDueAt()).isNotNull();
        verify(dashboardSummaryCache).invalidateAll();
    }

    @Test
    void updateStatusSetsResolvedAtAndQueuesStatusAlert() {
        User customer = TestFixtures.user(1L, UserRole.CUSTOMER);
        User agent = TestFixtures.user(2L, UserRole.AGENT);
        Ticket ticket = TestFixtures.ticket(10L, customer, agent, TicketPriority.HIGH, TicketStatus.IN_PROGRESS);
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(ticketAuthorizationService.canUpdateTicket(TestFixtures.principal(agent), ticket)).thenReturn(true);
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(alertWorkflowService.ticketStatusChanged(10L, TicketStatus.RESOLVED))
                .thenReturn(CompletableFuture.completedFuture(null));

        var response = ticketService.updateStatus(
                TestFixtures.principal(agent),
                10L,
                new UpdateTicketStatusRequest(TicketStatus.RESOLVED)
        );

        assertThat(response.status()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(response.resolvedAt()).isNotNull();
        verify(alertWorkflowService).ticketStatusChanged(10L, TicketStatus.RESOLVED);
        verify(dashboardSummaryCache).invalidateAll();
    }

    @Test
    void adminAssignsTicketToAgentAndQueuesAssignmentAlert() {
        User admin = TestFixtures.user(1L, UserRole.ADMIN);
        User customer = TestFixtures.user(2L, UserRole.CUSTOMER);
        User agent = TestFixtures.user(3L, UserRole.AGENT);
        Ticket ticket = TestFixtures.ticket(10L, customer, null, TicketPriority.MEDIUM, TicketStatus.OPEN);
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(3L)).thenReturn(Optional.of(agent));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(alertWorkflowService.ticketAssigned(10L, 3L)).thenReturn(CompletableFuture.completedFuture(null));

        var response = ticketService.assignTicket(
                TestFixtures.principal(admin),
                10L,
                new AssignTicketRequest(3L)
        );

        assertThat(response.assignedAgent().id()).isEqualTo(3L);
        verify(alertWorkflowService).ticketAssigned(10L, 3L);
        verify(dashboardSummaryCache).invalidateAll();

        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(ticketCaptor.capture());
        assertThat(ticketCaptor.getValue().getAssignedAgent()).isEqualTo(agent);
    }
}

