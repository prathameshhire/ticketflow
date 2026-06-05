package com.ticketflow.ticket;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ticketflow.auth.UserPrincipal;
import com.ticketflow.common.dto.PageResponse;
import com.ticketflow.ticket.dto.AssignTicketRequest;
import com.ticketflow.ticket.dto.CreateTicketCommentRequest;
import com.ticketflow.ticket.dto.CreateTicketRequest;
import com.ticketflow.ticket.dto.TicketCommentResponse;
import com.ticketflow.ticket.dto.TicketResponse;
import com.ticketflow.ticket.dto.UpdateTicketStatusRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public TicketResponse createTicket(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateTicketRequest request
    ) {
        return ticketService.createTicket(principal, request);
    }

    @GetMapping
    public PageResponse<TicketResponse> listTickets(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) Long assignedAgentId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(defaultValue = "createdAt") String sort
    ) {
        return ticketService.listTickets(principal, page, size, status, priority, assignedAgentId, customerId, query, sort);
    }

    @GetMapping("/{id}")
    public TicketResponse getTicket(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return ticketService.getTicket(principal, id);
    }

    @PatchMapping("/{id}/status")
    public TicketResponse updateStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketStatusRequest request
    ) {
        return ticketService.updateStatus(principal, id, request);
    }

    @PatchMapping("/{id}/assign")
    public TicketResponse assignTicket(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody AssignTicketRequest request
    ) {
        return ticketService.assignTicket(principal, id, request);
    }

    @PostMapping("/{id}/comments")
    public TicketCommentResponse addComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody CreateTicketCommentRequest request
    ) {
        return ticketService.addComment(principal, id, request);
    }

    @GetMapping("/{id}/comments")
    public List<TicketCommentResponse> listComments(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return ticketService.listComments(principal, id);
    }
}

