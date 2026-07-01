package com.secretariapay.api.dto.whatsapp;

public class WhatsAppDiagnosticsResponse {

    private Boolean enabled;
    private Boolean phoneNumberIdConfigured;
    private Boolean accessTokenConfigured;
    private String graphApiVersion;
    private String graphApiBaseUrl;
    private String mode;
    private String safetyMessage;

    public Boolean getEnabled() {
        return enabled;
    }

    public WhatsAppDiagnosticsResponse setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public Boolean getPhoneNumberIdConfigured() {
        return phoneNumberIdConfigured;
    }

    public WhatsAppDiagnosticsResponse setPhoneNumberIdConfigured(Boolean phoneNumberIdConfigured) {
        this.phoneNumberIdConfigured = phoneNumberIdConfigured;
        return this;
    }

    public Boolean getAccessTokenConfigured() {
        return accessTokenConfigured;
    }

    public WhatsAppDiagnosticsResponse setAccessTokenConfigured(Boolean accessTokenConfigured) {
        this.accessTokenConfigured = accessTokenConfigured;
        return this;
    }

    public String getGraphApiVersion() {
        return graphApiVersion;
    }

    public WhatsAppDiagnosticsResponse setGraphApiVersion(String graphApiVersion) {
        this.graphApiVersion = graphApiVersion;
        return this;
    }

    public String getGraphApiBaseUrl() {
        return graphApiBaseUrl;
    }

    public WhatsAppDiagnosticsResponse setGraphApiBaseUrl(String graphApiBaseUrl) {
        this.graphApiBaseUrl = graphApiBaseUrl;
        return this;
    }

    public String getMode() {
        return mode;
    }

    public WhatsAppDiagnosticsResponse setMode(String mode) {
        this.mode = mode;
        return this;
    }

    public String getSafetyMessage() {
        return safetyMessage;
    }

    public WhatsAppDiagnosticsResponse setSafetyMessage(String safetyMessage) {
        this.safetyMessage = safetyMessage;
        return this;
    }
}
