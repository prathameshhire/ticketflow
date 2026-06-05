package com.ticketflow.ticket;

public class InvalidTicketOperationException extends RuntimeException {

    public InvalidTicketOperationException(String message) {
        super(message);
    }
}

