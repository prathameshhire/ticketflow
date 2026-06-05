package com.ticketflow.alert;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.ticketflow.ticket.Ticket;
import com.ticketflow.ticket.TicketComment;
import com.ticketflow.ticket.TicketCommentRepository;
import com.ticketflow.ticket.TicketRepository;
import com.ticketflow.ticket.TicketStatus;
import com.ticketflow.user.User;
import com.ticketflow.user.UserRepository;

@Service
public class AlertWorkflowService {

    private final AlertRepository alertRepository;
    private final TicketRepository ticketRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;
    private final Executor alertTaskExecutor;

    public AlertWorkflowService(
            AlertRepository alertRepository,
            TicketRepository ticketRepository,
            TicketCommentRepository ticketCommentRepository,
            UserRepository userRepository,
            TransactionTemplate transactionTemplate,
            @Qualifier("alertTaskExecutor") Executor alertTaskExecutor
    ) {
        this.alertRepository = alertRepository;
        this.ticketRepository = ticketRepository;
        this.ticketCommentRepository = ticketCommentRepository;
        this.userRepository = userRepository;
        this.transactionTemplate = transactionTemplate;
        this.alertTaskExecutor = alertTaskExecutor;
    }

    public CompletableFuture<Void> ticketAssigned(Long ticketId, Long agentId) {
        if (agentId == null) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> transactionTemplate.executeWithoutResult(status -> {
            Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
            User agent = userRepository.findById(agentId).orElse(null);
            if (ticket == null || agent == null) {
                return;
            }

            alertRepository.save(Alert.create(
                    agent,
                    AlertType.ASSIGNMENT,
                    "Ticket #" + ticket.getId() + " was assigned to you: " + ticket.getTitle()
            ));
        }), alertTaskExecutor);
    }

    public CompletableFuture<Void> ticketStatusChanged(Long ticketId, TicketStatus newStatus) {
        return CompletableFuture.runAsync(() -> transactionTemplate.executeWithoutResult(status -> {
            Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
            if (ticket == null || ticket.getCustomer() == null) {
                return;
            }

            alertRepository.save(Alert.create(
                    ticket.getCustomer(),
                    AlertType.STATUS_CHANGE,
                    "Ticket #" + ticket.getId() + " status changed to " + newStatus + "."
            ));
        }), alertTaskExecutor);
    }

    public CompletableFuture<Void> commentAdded(Long commentId, Long authorId) {
        return CompletableFuture.runAsync(() -> transactionTemplate.executeWithoutResult(status -> {
            TicketComment comment = ticketCommentRepository.findById(commentId).orElse(null);
            if (comment == null) {
                return;
            }

            Ticket ticket = comment.getTicket();
            Map<Long, User> recipients = new LinkedHashMap<>();
            if (ticket.getCustomer() != null) {
                recipients.put(ticket.getCustomer().getId(), ticket.getCustomer());
            }
            if (ticket.getAssignedAgent() != null) {
                recipients.put(ticket.getAssignedAgent().getId(), ticket.getAssignedAgent());
            }

            recipients.values()
                    .stream()
                    .filter(user -> !Objects.equals(user.getId(), authorId))
                    .map(user -> Alert.create(
                            user,
                            AlertType.COMMENT,
                            "New comment on ticket #" + ticket.getId() + ": " + ticket.getTitle()
                    ))
                    .forEach(alertRepository::save);
        }), alertTaskExecutor);
    }
}

