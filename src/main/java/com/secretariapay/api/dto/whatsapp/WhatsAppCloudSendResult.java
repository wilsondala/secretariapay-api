package com.secretariapay.api.dto.whatsapp;

public class WhatsAppCloudSendResult {

    private Boolean success;
    private String providerMessageId;
    private String errorMessage;
    private String rawResponse;
    private Integer httpStatus;

    public static WhatsAppCloudSendResult sent(String providerMessageId, Integer httpStatus) {
        return new WhatsAppCloudSendResult()
                .setSuccess(true)
                .setProviderMessageId(providerMessageId)
                .setHttpStatus(httpStatus)
                .setErrorMessage(null);
    }

    public static WhatsAppCloudSendResult failed(String errorMessage, Integer httpStatus) {
        return new WhatsAppCloudSendResult()
                .setSuccess(false)
                .setProviderMessageId(null)
                .setHttpStatus(httpStatus)
                .setErrorMessage(errorMessage);
    }

    public Boolean getSuccess() {
        return success;
    }

    public boolean isSuccess() {
        return Boolean.TRUE.equals(success);
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

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public WhatsAppCloudSendResult setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
        return this;
    }
}
