package com.secretariapay.api.entity;

import com.secretariapay.api.entity.enums.BookingStatus;
import com.secretariapay.api.entity.enums.TicketAuditAction;
import com.secretariapay.api.entity.enums.TicketStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ticket_audit_logs")
public class TicketAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @Column(name = "ticket_code", nullable = false, length = 80)
    private String ticketCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TicketAuditAction action;

    @Column(nullable = false)
    private Boolean success;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_status", length = 30)
    private TicketStatus ticketStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status", length = 30)
    private BookingStatus bookingStatus;

    @Column(name = "ip_address", length = 80)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public TicketAuditLog setTicket(Ticket ticket) {
        this.ticket = ticket;
        return this;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public TicketAuditLog setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
        return this;
    }

    public TicketAuditAction getAction() {
        return action;
    }

    public TicketAuditLog setAction(TicketAuditAction action) {
        this.action = action;
        return this;
    }

    public Boolean getSuccess() {
        return success;
    }

    public TicketAuditLog setSuccess(Boolean success) {
        this.success = success;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public TicketAuditLog setMessage(String message) {
        this.message = message;
        return this;
    }

    public TicketStatus getTicketStatus() {
        return ticketStatus;
    }

    public TicketAuditLog setTicketStatus(TicketStatus ticketStatus) {
        this.ticketStatus = ticketStatus;
        return this;
    }

    public BookingStatus getBookingStatus() {
        return bookingStatus;
    }

    public TicketAuditLog setBookingStatus(BookingStatus bookingStatus) {
        this.bookingStatus = bookingStatus;
        return this;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public TicketAuditLog setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public TicketAuditLog setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
