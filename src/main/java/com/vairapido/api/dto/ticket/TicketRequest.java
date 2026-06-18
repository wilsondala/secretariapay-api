package com.vairapido.api.dto.ticket;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class TicketRequest {

    @NotNull(message = "A reserva é obrigatória.")
    private UUID bookingId;

    public UUID getBookingId() {
        return bookingId;
    }

    public TicketRequest setBookingId(UUID bookingId) {
        this.bookingId = bookingId;
        return this;
    }
}