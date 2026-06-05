package com.ticketflow.seed;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ticketflow.alert.Alert;
import com.ticketflow.alert.AlertRepository;
import com.ticketflow.alert.AlertType;
import com.ticketflow.ticket.Ticket;
import com.ticketflow.ticket.TicketComment;
import com.ticketflow.ticket.TicketCommentRepository;
import com.ticketflow.ticket.TicketPriority;
import com.ticketflow.ticket.TicketRepository;
import com.ticketflow.ticket.TicketStatus;
import com.ticketflow.user.User;
import com.ticketflow.user.UserRepository;
import com.ticketflow.user.UserRole;

@Component
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class SeedDataRunner implements CommandLineRunner {

    private static final String DEMO_PASSWORD = "password123";

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final AlertRepository alertRepository;
    private final PasswordEncoder passwordEncoder;

    public SeedDataRunner(
            UserRepository userRepository,
            TicketRepository ticketRepository,
            TicketCommentRepository ticketCommentRepository,
            AlertRepository alertRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.ticketCommentRepository = ticketCommentRepository;
        this.alertRepository = alertRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        User admin = upsertUser("TicketFlow Admin", "admin@ticketflow.dev", UserRole.ADMIN);
        User agent = upsertUser("Maya Agent", "agent@ticketflow.dev", UserRole.AGENT);
        User agent2 = upsertUser("Noah Agent", "agent2@ticketflow.dev", UserRole.AGENT);
        User customer = upsertUser("Casey Customer", "customer@ticketflow.dev", UserRole.CUSTOMER);

        if (ticketRepository.count() > 0) {
            return;
        }

        createSampleTickets(admin, agent, agent2, customer);
    }

    private User upsertUser(String name, String email, UserRole role) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> userRepository.save(User.create(
                        name,
                        email,
                        passwordEncoder.encode(DEMO_PASSWORD),
                        role
                )));
    }

    private void createSampleTickets(User admin, User agent, User agent2, User customer) {
        Instant now = Instant.now();
        List<SeedTicket> tickets = List.of(
                new SeedTicket("Checkout API returns 500", "Customers cannot complete card payments.", TicketPriority.URGENT, TicketStatus.IN_PROGRESS, agent, now.plus(3, ChronoUnit.HOURS)),
                new SeedTicket("Password reset email missing", "Reset email does not arrive for some customers.", TicketPriority.HIGH, TicketStatus.OPEN, null, now.plus(10, ChronoUnit.HOURS)),
                new SeedTicket("Billing page slow", "Billing page takes more than eight seconds to load.", TicketPriority.MEDIUM, TicketStatus.IN_PROGRESS, agent2, now.plus(21, ChronoUnit.HOURS)),
                new SeedTicket("Export CSV formatting issue", "CSV export has an extra delimiter in notes.", TicketPriority.LOW, TicketStatus.OPEN, null, now.plus(70, ChronoUnit.HOURS)),
                new SeedTicket("Webhook delivery failures", "Partner webhooks are retrying repeatedly.", TicketPriority.URGENT, TicketStatus.RESOLVED, agent, now.minus(2, ChronoUnit.HOURS)),
                new SeedTicket("Customer profile typo", "Customer display name has stale capitalization.", TicketPriority.LOW, TicketStatus.CLOSED, agent2, now.minus(12, ChronoUnit.HOURS)),
                new SeedTicket("Attachment preview broken", "PDF preview fails for larger files.", TicketPriority.MEDIUM, TicketStatus.OPEN, agent, now.minus(3, ChronoUnit.HOURS)),
                new SeedTicket("SLA timer inaccurate", "SLA countdown shows local browser time incorrectly.", TicketPriority.HIGH, TicketStatus.IN_PROGRESS, agent2, now.minus(1, ChronoUnit.HOURS)),
                new SeedTicket("Notification preference not saved", "Email preference toggles reset after refresh.", TicketPriority.MEDIUM, TicketStatus.RESOLVED, agent2, now.plus(5, ChronoUnit.HOURS)),
                new SeedTicket("Search misses exact ticket title", "Exact title search returns no results.", TicketPriority.HIGH, TicketStatus.OPEN, agent, now.plus(7, ChronoUnit.HOURS)),
                new SeedTicket("Mobile layout clips status chip", "Status chip overlaps the timestamp on mobile.", TicketPriority.LOW, TicketStatus.CLOSED, agent2, now.plus(50, ChronoUnit.HOURS)),
                new SeedTicket("Agent assignment not visible", "Assigned agent column is blank after update.", TicketPriority.MEDIUM, TicketStatus.IN_PROGRESS, agent, now.plus(14, ChronoUnit.HOURS)),
                new SeedTicket("Audit trail missing comment event", "Comment creation is not shown in history.", TicketPriority.HIGH, TicketStatus.RESOLVED, agent2, now.minus(8, ChronoUnit.HOURS)),
                new SeedTicket("Portal session expires early", "Customer portal session ends after five minutes.", TicketPriority.URGENT, TicketStatus.OPEN, agent, now.plus(2, ChronoUnit.HOURS)),
                new SeedTicket("Closed ticket still editable", "Closed tickets allow comment edits from old sessions.", TicketPriority.MEDIUM, TicketStatus.CLOSED, agent2, now.minus(18, ChronoUnit.HOURS))
        );

        for (SeedTicket seedTicket : tickets) {
            Ticket ticket = Ticket.create(
                    seedTicket.title(),
                    seedTicket.description(),
                    seedTicket.priority(),
                    customer,
                    seedTicket.slaDueAt()
            );
            ticket.setStatus(seedTicket.status());
            ticket.setAssignedAgent(seedTicket.agent());
            if (seedTicket.status() == TicketStatus.RESOLVED || seedTicket.status() == TicketStatus.CLOSED) {
                ticket.setResolvedAt(Instant.now().minus(2, ChronoUnit.HOURS));
            }

            Ticket savedTicket = ticketRepository.save(ticket);
            ticketCommentRepository.save(TicketComment.create(savedTicket, customer, "Initial report: " + seedTicket.description()));
            if (seedTicket.agent() != null) {
                ticketCommentRepository.save(TicketComment.create(savedTicket, seedTicket.agent(), "Triage started for " + seedTicket.priority() + " priority."));
                alertRepository.save(Alert.create(seedTicket.agent(), AlertType.ASSIGNMENT,
                        "Seed ticket assigned: #" + savedTicket.getId() + " " + savedTicket.getTitle()));
            }
            alertRepository.save(Alert.create(customer, AlertType.STATUS_CHANGE,
                    "Seed ticket #" + savedTicket.getId() + " is " + savedTicket.getStatus() + "."));
        }

        alertRepository.save(Alert.create(admin, AlertType.SLA_WARNING, "Seed data includes overdue SLA examples for dashboard testing."));
    }

    private record SeedTicket(
            String title,
            String description,
            TicketPriority priority,
            TicketStatus status,
            User agent,
            Instant slaDueAt
    ) {
    }
}

