package com.secretariapay.api.dto.whatsapp;

public class WhatsAppCloudSendResult {

    private Boolean success;
    private String providerMessageId;
    private String errorMessage;
    private String rawResponse;

    public Boolean getSuccess() {
        return success;
    }

    public WhatsAppCloudSendResult setSuccess(Boolean success) {
        this.success = success;
        return this;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public WhatsAppCloudSendResult setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
        return this;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public WhatsAppCloudSendResult setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public WhatsAppCloudSendResult setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
        return this;
    }
}

