package com.secretariapay.api.dto.whatsapp;

public class SecretariaPayMessageStatusRequest {

    private String providerMessageId;
    private String failureReason;

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public SecretariaPayMessageStatusRequest setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
        return this;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public SecretariaPayMessageStatusRequest setFailureReason(String failureReason) {
        this.failureReason = failureReason;
        return this;
    }
}
