package com.secretariapay.api.entity;

import com.secretariapay.api.entity.enums.TicketStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(name = "ticket_code", nullable = false, unique = true, length = 50)
    private String ticketCode;

    @Column(name = "qr_code_url", nullable = false, columnDefinition = "TEXT")
    private String qrCodeUrl;

    @Column(name = "validation_url", nullable = false, columnDefinition = "TEXT")
    private String validationUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketStatus status = TicketStatus.VALID;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;

        if (this.issuedAt == null) {
            this.issuedAt = now;
        }

        if (this.status == null) {
            this.status = TicketStatus.VALID;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Booking getBooking() {
        return booking;
    }

    public Ticket setBooking(Booking booking) {
        this.booking = booking;
        return this;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public Ticket setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
        return this;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public Ticket setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
        return this;
    }

    public String getValidationUrl() {
        return validationUrl;
    }

    public Ticket setValidationUrl(String validationUrl) {
        this.validationUrl = validationUrl;
        return this;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public Ticket setStatus(TicketStatus status) {
        this.status = status;
        return this;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public Ticket setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
        return this;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public Ticket setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
        return this;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public Ticket setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
