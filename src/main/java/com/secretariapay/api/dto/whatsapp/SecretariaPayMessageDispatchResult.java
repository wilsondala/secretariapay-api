package com.secretariapay.api.dto.whatsapp;

import java.time.LocalDateTime;
import java.util.UUID;

public class SecretariaPayMessageDispatchResult {

    private UUID messageId;
    private String status;
    private String providerMessageId;
    private String failureReason;
    private String recipientPhone;
    private LocalDateTime processedAt;

    public UUID getMessageId() {
        return messageId;
    }

    public SecretariaPayMessageDispatchResult setMessageId(UUID messageId) {
        this.messageId = messageId;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public SecretariaPayMessageDispatchResult setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public SecretariaPayMessageDispatchResult setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
        return this;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public SecretariaPayMessageDispatchResult setFailureReason(String failureReason) {
        this.failureReason = failureReason;
        return this;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public SecretariaPayMessageDispatchResult setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
        return this;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public SecretariaPayMessageDispatchResult setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
        return this;
    }
}
