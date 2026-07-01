package com.secretariapay.api.dto.whatsapp;

import java.time.LocalDateTime;

public class WhatsAppBackfillResponse {

    private Integer paymentInstructionMessagesCreated;
    private Integer ticketIssuedMessagesCreated;
    private Integer skippedWithoutWhatsapp;
    private Integer skippedAlreadyExisting;
    private LocalDateTime processedAt;
    private String message;

    public Integer getPaymentInstructionMessagesCreated() {
        return paymentInstructionMessagesCreated;
    }

    public WhatsAppBackfillResponse setPaymentInstructionMessagesCreated(Integer paymentInstructionMessagesCreated) {
        this.paymentInstructionMessagesCreated = paymentInstructionMessagesCreated;
        return this;
    }

    public Integer getTicketIssuedMessagesCreated() {
        return ticketIssuedMessagesCreated;
    }

    public WhatsAppBackfillResponse setTicketIssuedMessagesCreated(Integer ticketIssuedMessagesCreated) {
        this.ticketIssuedMessagesCreated = ticketIssuedMessagesCreated;
        return this;
    }

    public Integer getSkippedWithoutWhatsapp() {
        return skippedWithoutWhatsapp;
    }

    public WhatsAppBackfillResponse setSkippedWithoutWhatsapp(Integer skippedWithoutWhatsapp) {
        this.skippedWithoutWhatsapp = skippedWithoutWhatsapp;
        return this;
    }

    public Integer getSkippedAlreadyExisting() {
        return skippedAlreadyExisting;
    }

    public WhatsAppBackfillResponse setSkippedAlreadyExisting(Integer skippedAlreadyExisting) {
        this.skippedAlreadyExisting = skippedAlreadyExisting;
        return this;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public WhatsAppBackfillResponse setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public WhatsAppBackfillResponse setMessage(String message) {
        this.message = message;
        return this;
    }
}

