package com.secretariapay.api.dto.whatsappcloud;

import java.time.LocalDateTime;

public class WhatsappOutboundMessageResult {

    private Boolean enabled;
    private Boolean attempted;
    private Boolean sent;
    private String phoneNumber;
    private String providerMessageId;
    private String errorMessage;
    private LocalDateTime sentAt;

    public Boolean getEnabled() {
        return enabled;
    }

    public WhatsappOutboundMessageResult setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public Boolean getAttempted() {
        return attempted;
    }

    public WhatsappOutboundMessageResult setAttempted(Boolean attempted) {
        this.attempted = attempted;
        return this;
    }

    public Boolean getSent() {
        return sent;
    }

    public WhatsappOutboundMessageResult setSent(Boolean sent) {
        this.sent = sent;
        return this;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public WhatsappOutboundMessageResult setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public WhatsappOutboundMessageResult setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
        return this;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public WhatsappOutboundMessageResult setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public WhatsappOutboundMessageResult setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
        return this;
    }
}
