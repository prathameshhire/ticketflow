package com.ticketflow.ticket.dto;

import java.time.Instant;

import com.ticketflow.ticket.TicketComment;

public record TicketCommentResponse(Long id, Long ticketId, UserSummaryResponse author, String body, Instant createdAt) {

    public static TicketCommentResponse from(TicketComment comment) {
        return new TicketCommentResponse(
                comment.getId(),
                comment.getTicket().getId(),
                UserSummaryResponse.from(comment.getAuthor()),
                comment.getBody(),
                comment.getCreatedAt()
        );
    }
}

