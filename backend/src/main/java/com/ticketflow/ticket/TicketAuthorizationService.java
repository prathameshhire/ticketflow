package com.ticketflow.ticket;

import org.springframework.stereotype.Service;

import com.ticketflow.auth.UserPrincipal;
import com.ticketflow.user.UserRole;

@Service
public class TicketAuthorizationService {

    public boolean canCreateTicket(UserPrincipal principal) {
        return principal != null && principal.role() == UserRole.CUSTOMER;
    }

    public boolean canViewTicket(UserPrincipal principal, Ticket ticket) {
        if (principal == null || ticket == null) {
            return false;
        }
        if (principal.role() == UserRole.ADMIN) {
            return true;
        }
        if (principal.role() == UserRole.CUSTOMER) {
            return ticket.getCustomer() != null && principal.id().equals(ticket.getCustomer().getId());
        }
        return principal.role() == UserRole.AGENT
                && ticket.getAssignedAgent() != null
                && principal.id().equals(ticket.getAssignedAgent().getId());
    }

    public boolean canUpdateTicket(UserPrincipal principal, Ticket ticket) {
        if (principal == null || ticket == null) {
            return false;
        }
        if (principal.role() == UserRole.ADMIN) {
            return true;
        }
        return principal.role() == UserRole.AGENT
                && ticket.getAssignedAgent() != null
                && principal.id().equals(ticket.getAssignedAgent().getId());
    }
}
