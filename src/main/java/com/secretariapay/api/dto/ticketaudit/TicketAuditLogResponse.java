package com.secretariapay.api.dto.ticketaudit;

import com.secretariapay.api.entity.enums.BookingStatus;
import com.secretariapay.api.entity.enums.TicketAuditAction;
import com.secretariapay.api.entity.enums.TicketStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class TicketAuditLogResponse {

    private UUID id;
    private String ticketCode;
    private TicketAuditAction action;
    private Boolean success;
    private String message;
    private TicketStatus ticketStatus;
    private BookingStatus bookingStatus;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public TicketAuditLogResponse setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public TicketAuditLogResponse setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
        return this;
    }

    public TicketAuditAction getAction() {
        return action;
    }

    public TicketAuditLogResponse setAction(TicketAuditAction action) {
        this.action = action;
        return this;
    }

    public Boolean getSuccess() {
        return success;
    }

    public TicketAuditLogResponse setSuccess(Boolean success) {
        this.success = success;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public TicketAuditLogResponse setMessage(String message) {
        this.message = message;
        return this;
    }

    public TicketStatus getTicketStatus() {
        return ticketStatus;
    }

    public TicketAuditLogResponse setTicketStatus(TicketStatus ticketStatus) {
        this.ticketStatus = ticketStatus;
        return this;
    }

    public BookingStatus getBookingStatus() {
        return bookingStatus;
    }

    public TicketAuditLogResponse setBookingStatus(BookingStatus bookingStatus) {
        this.bookingStatus = bookingStatus;
        return this;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public TicketAuditLogResponse setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public TicketAuditLogResponse setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public TicketAuditLogResponse setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }
}
