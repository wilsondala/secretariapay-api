package com.vairapido.api.entity;

import com.vairapido.api.entity.enums.WhatsAppMessageStatus;
import com.vairapido.api.entity.enums.WhatsAppMessageType;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "whatsapp_messages")
public class WhatsAppMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 50)
    private WhatsAppMessageType messageType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WhatsAppMessageStatus status = WhatsAppMessageStatus.PENDING;

    @Column(name = "to_phone", nullable = false, length = 40)
    private String toPhone;

    @Column(name = "passenger_name", length = 160)
    private String passengerName;

    @Column(name = "reference_code", length = 80)
    private String referenceCode;

    @Column(name = "message_body", nullable = false, columnDefinition = "TEXT")
    private String messageBody;

    @Column(name = "provider_name", length = 80)
    private String providerName;

    @Column(name = "provider_message_id", length = 160)
    private String providerMessageId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = WhatsAppMessageStatus.PENDING;
        }

        if (this.providerName == null || this.providerName.isBlank()) {
            this.providerName = "WHATSAPP_SIMULADO";
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

    public WhatsAppMessage setBooking(Booking booking) {
        this.booking = booking;
        return this;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public WhatsAppMessage setTicket(Ticket ticket) {
        this.ticket = ticket;
        return this;
    }

    public WhatsAppMessageType getMessageType() {
        return messageType;
    }

    public WhatsAppMessage setMessageType(WhatsAppMessageType messageType) {
        this.messageType = messageType;
        return this;
    }

    public WhatsAppMessageStatus getStatus() {
        return status;
    }

    public WhatsAppMessage setStatus(WhatsAppMessageStatus status) {
        this.status = status;
        return this;
    }

    public String getToPhone() {
        return toPhone;
    }

    public WhatsAppMessage setToPhone(String toPhone) {
        this.toPhone = toPhone;
        return this;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public WhatsAppMessage setPassengerName(String passengerName) {
        this.passengerName = passengerName;
        return this;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public WhatsAppMessage setReferenceCode(String referenceCode) {
        this.referenceCode = referenceCode;
        return this;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public WhatsAppMessage setMessageBody(String messageBody) {
        this.messageBody = messageBody;
        return this;
    }

    public String getProviderName() {
        return providerName;
    }

    public WhatsAppMessage setProviderName(String providerName) {
        this.providerName = providerName;
        return this;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public WhatsAppMessage setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
        return this;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public WhatsAppMessage setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public WhatsAppMessage setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
        return this;
    }

    public LocalDateTime getFailedAt() {
        return failedAt;
    }

    public WhatsAppMessage setFailedAt(LocalDateTime failedAt) {
        this.failedAt = failedAt;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}