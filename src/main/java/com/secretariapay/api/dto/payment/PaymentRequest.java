package com.secretariapay.api.dto.payment;

import com.secretariapay.api.entity.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class PaymentRequest {

    @NotNull(message = "A reserva é obrigatória.")
    private UUID bookingId;

    @NotNull(message = "O método de pagamento é obrigatório.")
    private PaymentMethod method;

    public UUID getBookingId() {
        return bookingId;
    }

    public PaymentRequest setBookingId(UUID bookingId) {
        this.bookingId = bookingId;
        return this;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public PaymentRequest setMethod(PaymentMethod method) {
        this.method = method;
        return this;
    }
}
