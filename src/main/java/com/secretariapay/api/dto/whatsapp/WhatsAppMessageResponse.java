package com.secretariapay.api.dto.whatsapp;

import com.secretariapay.api.entity.enums.WhatsAppMessageStatus;
import com.secretariapay.api.entity.enums.WhatsAppMessageType;

import java.time.LocalDateTime;
import java.util.UUID;

public class WhatsAppMessageResponse {

    private UUID id;
    private UUID bookingId;
    private UUID ticketId;
    private WhatsAppMessageType messageType;
    private WhatsAppMessageStatus status;
    private String toPhone;
    private String passengerName;
    private String referenceCode;
    private String messageBody;
    private String providerName;
    private String providerMessageId;
    private String errorMessage;
    private LocalDateTime sentAt;
    private LocalDateTime failedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public WhatsAppMessageResponse setId(UUID id) {
        this.id = id;
        return this;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public WhatsAppMessageResponse setBookingId(UUID bookingId) {
        this.bookingId = bookingId;
        return this;
    }

    public UUID getTicketId() {
        return ticketId;
    }

    public WhatsAppMessageResponse setTicketId(UUID ticketId) {
        this.ticketId = ticketId;
        return this;
    }

    public WhatsAppMessageType getMessageType() {
        return messageType;
    }

    public WhatsAppMessageResponse setMessageType(WhatsAppMessageType messageType) {
        this.messageType = messageType;
        return this;
    }

    public WhatsAppMessageStatus getStatus() {
        return status;
    }

    public WhatsAppMessageResponse setStatus(WhatsAppMessageStatus status) {
        this.status = status;
        return this;
    }

    public String getToPhone() {
        return toPhone;
    }

    public WhatsAppMessageResponse setToPhone(String toPhone) {
        this.toPhone = toPhone;
        return this;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public WhatsAppMessageResponse setPassengerName(String passengerName) {
        this.passengerName = passengerName;
        return this;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public WhatsAppMessageResponse setReferenceCode(String referenceCode) {
        this.referenceCode = referenceCode;
        return this;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public WhatsAppMessageResponse setMessageBody(String messageBody) {
        this.messageBody = messageBody;
        return this;
    }

    public String getProviderName() {
        return providerName;
    }

    public WhatsAppMessageResponse setProviderName(String providerName) {
        this.providerName = providerName;
        return this;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public WhatsAppMessageResponse setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
        return this;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public WhatsAppMessageResponse setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public WhatsAppMessageResponse setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
        return this;
    }

    public LocalDateTime getFailedAt() {
        return failedAt;
    }

    public WhatsAppMessageResponse setFailedAt(LocalDateTime failedAt) {
        this.failedAt = failedAt;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public WhatsAppMessageResponse setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public WhatsAppMessageResponse setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}
