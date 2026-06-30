package com.vairapido.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vairapido.whatsapp.cloud")
public class WhatsAppCloudProperties {

    private boolean enabled = false;
    private String accessToken;
    private String phoneNumberId;
    private String graphApiVersion = "v20.0";
    private String baseUrl = "https://graph.facebook.com";

    public boolean isEnabled() {
        return enabled;
    }

    public WhatsAppCloudProperties setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public WhatsAppCloudProperties setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    public String getPhoneNumberId() {
        return phoneNumberId;
    }

    public WhatsAppCloudProperties setPhoneNumberId(String phoneNumberId) {
        this.phoneNumberId = phoneNumberId;
        return this;
    }

    public String getGraphApiVersion() {
        return graphApiVersion;
    }

    public WhatsAppCloudProperties setGraphApiVersion(String graphApiVersion) {
        this.graphApiVersion = graphApiVersion;
        return this;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public WhatsAppCloudProperties setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public boolean hasAccessToken() {
        return accessToken != null && !accessToken.isBlank();
    }

    public boolean hasPhoneNumberId() {
        return phoneNumberId != null && !phoneNumberId.isBlank();
    }

    public boolean isConfigured() {
        return enabled && hasAccessToken() && hasPhoneNumberId();
    }

    public String getNormalizedGraphApiVersion() {
        if (graphApiVersion == null || graphApiVersion.isBlank()) {
            return "v20.0";
        }

        return graphApiVersion.startsWith("v")
                ? graphApiVersion.trim()
                : "v" + graphApiVersion.trim();
    }

    public String getNormalizedBaseUrl() {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://graph.facebook.com";
        }

        return baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
    }
}
