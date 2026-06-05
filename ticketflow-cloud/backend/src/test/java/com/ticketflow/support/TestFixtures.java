package com.ticketflow.support;

import java.time.Instant;

import org.springframework.test.util.ReflectionTestUtils;

import com.ticketflow.alert.Alert;
import com.ticketflow.alert.AlertType;
import com.ticketflow.auth.UserPrincipal;
import com.ticketflow.ticket.Ticket;
import com.ticketflow.ticket.TicketPriority;
import com.ticketflow.ticket.TicketStatus;
import com.ticketflow.user.User;
import com.ticketflow.user.UserRole;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static User user(long id, UserRole role) {
        User user = User.create(
                role.name() + " User",
                role.name().toLowerCase() + id + "@example.com",
                "hash",
                role
        );
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "createdAt", Instant.now().minusSeconds(3600));
        ReflectionTestUtils.setField(user, "updatedAt", Instant.now().minusSeconds(1800));
        return user;
    }

    public static UserPrincipal principal(User user) {
        return UserPrincipal.from(user);
    }

    public static Ticket ticket(
            long id,
            User customer,
            User assignedAgent,
            TicketPriority priority,
            TicketStatus status
    ) {
        Ticket ticket = Ticket.create(
                "Ticket " + id,
                "Ticket description " + id,
                priority,
                customer,
                Instant.now().plusSeconds(3600)
        );
        ticket.setAssignedAgent(assignedAgent);
        ticket.setStatus(status);
        ReflectionTestUtils.setField(ticket, "id", id);
        ReflectionTestUtils.setField(ticket, "createdAt", Instant.now().minusSeconds(7200));
        ReflectionTestUtils.setField(ticket, "updatedAt", Instant.now().minusSeconds(3600));
        if (status == TicketStatus.RESOLVED || status == TicketStatus.CLOSED) {
            ticket.setResolvedAt(Instant.now().minusSeconds(1800));
        }
        return ticket;
    }

    public static Alert alert(long id, User recipient, AlertType type) {
        Alert alert = Alert.create(recipient, type, "Alert " + id);
        ReflectionTestUtils.setField(alert, "id", id);
        ReflectionTestUtils.setField(alert, "createdAt", Instant.now());
        return alert;
    }
}

