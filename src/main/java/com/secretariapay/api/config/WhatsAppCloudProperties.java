package com.secretariapay.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WhatsAppCloudProperties {

    @Value("${vairapido.whatsapp.enabled:false}")
    private boolean enabled;

    @Value("${vairapido.whatsapp.access-token:}")
    private String accessToken;

    @Value("${vairapido.whatsapp.phone-number-id:}")
    private String phoneNumberId;

    @Value("${vairapido.whatsapp.graph-api-version:v20.0}")
    private String graphApiVersion;

    @Value("${vairapido.whatsapp.graph-api-base-url:https://graph.facebook.com}")
    private String baseUrl;

    public boolean isEnabled() {
        return enabled;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getPhoneNumberId() {
        return phoneNumberId;
    }

    public String getGraphApiVersion() {
        return graphApiVersion;
    }

    public String getBaseUrl() {
        return baseUrl;
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
