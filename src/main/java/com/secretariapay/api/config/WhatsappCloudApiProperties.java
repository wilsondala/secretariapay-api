package com.secretariapay.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "secretariapay.whatsapp")
public class WhatsappCloudApiProperties {

    private Boolean enabled = false;
    private String phoneNumberId;
    private String accessToken;
    private String graphApiVersion = "v20.0";
    private String graphApiBaseUrl = "https://graph.facebook.com";

    public Boolean getEnabled() {
        return enabled;
    }

    public WhatsappCloudApiProperties setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    public String getPhoneNumberId() {
        return phoneNumberId;
    }

    public WhatsappCloudApiProperties setPhoneNumberId(String phoneNumberId) {
        this.phoneNumberId = phoneNumberId;
        return this;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public WhatsappCloudApiProperties setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    public String getGraphApiVersion() {
        return graphApiVersion;
    }

    public WhatsappCloudApiProperties setGraphApiVersion(String graphApiVersion) {
        this.graphApiVersion = graphApiVersion;
        return this;
    }

    public String getGraphApiBaseUrl() {
        return graphApiBaseUrl;
    }

    public WhatsappCloudApiProperties setGraphApiBaseUrl(String graphApiBaseUrl) {
        this.graphApiBaseUrl = graphApiBaseUrl;
        return this;
    }

    public boolean isConfigured() {
        return hasText(phoneNumberId)
                && hasText(accessToken)
                && hasText(graphApiVersion)
                && hasText(graphApiBaseUrl);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
