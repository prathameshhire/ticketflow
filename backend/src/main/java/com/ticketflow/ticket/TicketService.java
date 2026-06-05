package com.ticketflow.ticket;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ticketflow.alert.AlertWorkflowService;
import com.ticketflow.analytics.DashboardSummaryCache;
import com.ticketflow.auth.UserPrincipal;
import com.ticketflow.common.ResourceNotFoundException;
import com.ticketflow.common.dto.PageResponse;
import com.ticketflow.ticket.dto.AssignTicketRequest;
import com.ticketflow.ticket.dto.CreateTicketCommentRequest;
import com.ticketflow.ticket.dto.CreateTicketRequest;
import com.ticketflow.ticket.dto.TicketCommentResponse;
import com.ticketflow.ticket.dto.TicketResponse;
import com.ticketflow.ticket.dto.UpdateTicketStatusRequest;
import com.ticketflow.user.User;
import com.ticketflow.user.UserRepository;
import com.ticketflow.user.UserRole;

@Service
public class TicketService {

    private static final int MAX_PAGE_SIZE = 100;

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final UserRepository userRepository;
    private final TicketAuthorizationService ticketAuthorizationService;
    private final AlertWorkflowService alertWorkflowService;
    private final DashboardSummaryCache dashboardSummaryCache;
    private final Clock clock;

    public TicketService(
            TicketRepository ticketRepository,
            TicketCommentRepository ticketCommentRepository,
            UserRepository userRepository,
            TicketAuthorizationService ticketAuthorizationService,
            AlertWorkflowService alertWorkflowService,
            DashboardSummaryCache dashboardSummaryCache
    ) {
        this.ticketRepository = ticketRepository;
        this.ticketCommentRepository = ticketCommentRepository;
        this.userRepository = userRepository;
        this.ticketAuthorizationService = ticketAuthorizationService;
        this.alertWorkflowService = alertWorkflowService;
        this.dashboardSummaryCache = dashboardSummaryCache;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public TicketResponse createTicket(UserPrincipal principal, CreateTicketRequest request) {
        if (!ticketAuthorizationService.canCreateTicket(principal)) {
            throw new AccessDeniedException("Only customers can create tickets.");
        }

        User customer = findUser(principal.id());
        Ticket ticket = Ticket.create(
                request.title().trim(),
                request.description().trim(),
                request.priority(),
                customer,
                calculateSlaDueAt(request.priority())
        );

        Ticket savedTicket = ticketRepository.save(ticket);
        dashboardSummaryCache.invalidateAll();
        return TicketResponse.from(savedTicket);
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> listTickets(
            UserPrincipal principal,
            int page,
            int size,
            TicketStatus status,
            TicketPriority priority,
            Long assignedAgentId,
            Long customerId,
            String query,
            String sort
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        TicketSortField sortField = TicketSortField.from(sort);

        Specification<Ticket> specification = TicketSpecifications.withUsers()
                .and(TicketSpecifications.visibleTo(principal))
                .and(TicketSpecifications.statusEquals(status))
                .and(TicketSpecifications.priorityEquals(priority))
                .and(TicketSpecifications.assignedAgentEquals(assignedAgentId))
                .and(TicketSpecifications.customerEquals(customerId))
                .and(TicketSpecifications.search(query))
                .and(TicketSpecifications.orderBy(sortField));

        Page<TicketResponse> tickets = ticketRepository.findAll(specification, pageable)
                .map(TicketResponse::from);

        return PageResponse.from(tickets);
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicket(UserPrincipal principal, Long id) {
        Ticket ticket = findTicket(id);
        requireCanView(principal, ticket);
        return TicketResponse.from(ticket);
    }

    @Transactional
    public TicketResponse updateStatus(UserPrincipal principal, Long id, UpdateTicketStatusRequest request) {
        Ticket ticket = findTicket(id);
        requireCanUpdate(principal, ticket);
        TicketStatus previousStatus = ticket.getStatus();

        ticket.setStatus(request.status());
        if (request.status() == TicketStatus.RESOLVED || request.status() == TicketStatus.CLOSED) {
            ticket.setResolvedAt(Instant.now(clock));
        } else {
            ticket.setResolvedAt(null);
        }

        Ticket savedTicket = ticketRepository.save(ticket);
        dashboardSummaryCache.invalidateAll();
        if (previousStatus != request.status()) {
            afterCommit(() -> alertWorkflowService.ticketStatusChanged(savedTicket.getId(), request.status()));
        }
        return TicketResponse.from(savedTicket);
    }

    @Transactional
    public TicketResponse assignTicket(UserPrincipal principal, Long id, AssignTicketRequest request) {
        if (principal.role() != UserRole.ADMIN) {
            throw new AccessDeniedException("Only admins can assign tickets.");
        }

        Ticket ticket = findTicket(id);
        User agent = null;
        if (request.assignedAgentId() != null) {
            agent = findUser(request.assignedAgentId());
            if (agent.getRole() != UserRole.AGENT) {
                throw new InvalidTicketOperationException("Tickets can only be assigned to agents.");
            }
        }

        ticket.setAssignedAgent(agent);
        Ticket savedTicket = ticketRepository.save(ticket);
        dashboardSummaryCache.invalidateAll();
        if (agent != null) {
            Long agentId = agent.getId();
            afterCommit(() -> alertWorkflowService.ticketAssigned(savedTicket.getId(), agentId));
        }
        return TicketResponse.from(savedTicket);
    }

    @Transactional
    public TicketCommentResponse addComment(
            UserPrincipal principal,
            Long ticketId,
            CreateTicketCommentRequest request
    ) {
        Ticket ticket = findTicket(ticketId);
        requireCanView(principal, ticket);
        User author = findUser(principal.id());

        TicketComment comment = TicketComment.create(ticket, author, request.body().trim());
        TicketComment savedComment = ticketCommentRepository.save(comment);
        dashboardSummaryCache.invalidateAll();
        afterCommit(() -> alertWorkflowService.commentAdded(savedComment.getId(), author.getId()));
        return TicketCommentResponse.from(savedComment);
    }

    @Transactional(readOnly = true)
    public List<TicketCommentResponse> listComments(UserPrincipal principal, Long ticketId) {
        Ticket ticket = findTicket(ticketId);
        requireCanView(principal, ticket);

        return ticketCommentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(TicketCommentResponse::from)
                .toList();
    }

    private Instant calculateSlaDueAt(TicketPriority priority) {
        long hours = switch (priority) {
            case URGENT -> 4;
            case HIGH -> 12;
            case MEDIUM -> 24;
            case LOW -> 72;
        };
        return Instant.now(clock).plusSeconds(hours * 60 * 60);
    }

    private Ticket findTicket(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found."));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private void requireCanView(UserPrincipal principal, Ticket ticket) {
        if (!ticketAuthorizationService.canViewTicket(principal, ticket)) {
            throw new AccessDeniedException("You do not have permission to view this ticket.");
        }
    }

    private void requireCanUpdate(UserPrincipal principal, Ticket ticket) {
        if (!ticketAuthorizationService.canUpdateTicket(principal, ticket)) {
            throw new AccessDeniedException("You do not have permission to update this ticket.");
        }
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
