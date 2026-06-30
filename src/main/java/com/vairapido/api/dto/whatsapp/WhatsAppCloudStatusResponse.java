package com.vairapido.api.dto.whatsapp;

public class WhatsAppCloudStatusResponse {

    private Boolean enabled;
    private Boolean configured;
    private Boolean accessTokenConfigured;
    private Boolean phoneNumberIdConfigured;
    private String providerName;
    private String graphApiVersion;
    private String baseUrl;
    private String message;

    public Boolean getEnabled() {
        return enabled;
    }

    public WhatsAppCloudStatusResponse setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public Boolean getConfigured() {
        return configured;
    }

    public WhatsAppCloudStatusResponse setConfigured(Boolean configured) {
        this.configured = configured;
        return this;
    }

    public Boolean getAccessTokenConfigured() {
        return accessTokenConfigured;
    }

    public WhatsAppCloudStatusResponse setAccessTokenConfigured(Boolean accessTokenConfigured) {
        this.accessTokenConfigured = accessTokenConfigured;
        return this;
    }

    public Boolean getPhoneNumberIdConfigured() {
        return phoneNumberIdConfigured;
    }

    public WhatsAppCloudStatusResponse setPhoneNumberIdConfigured(Boolean phoneNumberIdConfigured) {
        this.phoneNumberIdConfigured = phoneNumberIdConfigured;
        return this;
    }

    public String getProviderName() {
        return providerName;
    }

    public WhatsAppCloudStatusResponse setProviderName(String providerName) {
        this.providerName = providerName;
        return this;
    }

    public String getGraphApiVersion() {
        return graphApiVersion;
    }

    public WhatsAppCloudStatusResponse setGraphApiVersion(String graphApiVersion) {
        this.graphApiVersion = graphApiVersion;
        return this;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public WhatsAppCloudStatusResponse setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public WhatsAppCloudStatusResponse setMessage(String message) {
        this.message = message;
        return this;
    }
}
