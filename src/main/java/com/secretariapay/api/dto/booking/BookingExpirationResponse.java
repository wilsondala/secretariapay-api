package com.secretariapay.api.dto.booking;

import java.time.LocalDateTime;

public class BookingExpirationResponse {

    private int expiredBookings;
    private LocalDateTime processedAt;
    private String message;

    public int getExpiredBookings() {
        return expiredBookings;
    }

    public BookingExpirationResponse setExpiredBookings(int expiredBookings) {
        this.expiredBookings = expiredBookings;
        return this;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public BookingExpirationResponse setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public BookingExpirationResponse setMessage(String message) {
        this.message = message;
        return this;
    }
}
