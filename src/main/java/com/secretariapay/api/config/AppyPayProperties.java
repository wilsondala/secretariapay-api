package com.secretariapay.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "secretariapay.appypay")
public class AppyPayProperties {

    private static final String DEFAULT_TOKEN_URL = "https://login.microsoftonline.com/appypaydev.onmicrosoft.com/oauth2/token";
    private static final String DEFAULT_CHARGES_URL = "https://gwy-api-tst.appypay.co.ao/v2.0/charges";
    private static final String DEFAULT_RESOURCE = "2aed7612-de64-46b5-9e59-1f48f8902d14";

    private boolean enabled = false;
    private String tokenUrl = DEFAULT_TOKEN_URL;
    private String chargesUrl = DEFAULT_CHARGES_URL;
    private String clientId = "";
    private String clientSecret = "";
    private String resource = DEFAULT_RESOURCE;
    private String gpoPaymentMethod = "";
    private String refPaymentMethod = "";

    public boolean isEnabled() {
        return enabled || Boolean.parseBoolean(env("APPYPAY_ENABLED"));
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTokenUrl() {
        return firstNonBlank(
                env("SECRETARIAPAY_APPYPAY_TOKEN_URL"),
                env("APPYPAY_AUTH_URL"),
                tokenUrl,
                DEFAULT_TOKEN_URL
        );
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    public String getChargesUrl() {
        String explicitChargesUrl = firstNonBlank(
                env("SECRETARIAPAY_APPYPAY_CHARGES_URL"),
                env("APPYPAY_CHARGES_URL")
        );
        if (!explicitChargesUrl.isBlank()) {
            return explicitChargesUrl;
        }

        String apiBaseUrl = firstNonBlank(env("APPYPAY_API_BASE_URL"), env("SECRETARIAPAY_APPYPAY_API_BASE_URL"));
        if (!apiBaseUrl.isBlank()) {
            return apiBaseUrl.replaceAll("/+$", "") + "/v2.0/charges";
        }

        return firstNonBlank(chargesUrl, DEFAULT_CHARGES_URL);
    }

    public void setChargesUrl(String chargesUrl) {
        this.chargesUrl = chargesUrl;
    }

    public String getClientId() {
        return firstNonBlank(env("SECRETARIAPAY_APPYPAY_CLIENT_ID"), env("APPYPAY_CLIENT_ID"), clientId);
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return firstNonBlank(
                env("SECRETARIAPAY_APPYPAY_CLIENT_SECRET"),
                env("APPYPAY_CLIENT_SECRET"),
                env("APPYPAY_CLIENT_PASSWORD"),
                clientSecret
        );
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getResource() {
        return firstNonBlank(env("SECRETARIAPAY_APPYPAY_RESOURCE"), env("APPYPAY_RESOURCE"), resource, DEFAULT_RESOURCE);
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getGpoPaymentMethod() {
        return firstNonBlank(
                env("SECRETARIAPAY_APPYPAY_GPO_PAYMENT_METHOD"),
                env("APPYPAY_PAYMENT_METHOD_GPO"),
                gpoPaymentMethod
        );
    }

    public void setGpoPaymentMethod(String gpoPaymentMethod) {
        this.gpoPaymentMethod = gpoPaymentMethod;
    }

    public String getRefPaymentMethod() {
        return firstNonBlank(
                env("SECRETARIAPAY_APPYPAY_REF_PAYMENT_METHOD"),
                env("APPYPAY_PAYMENT_METHOD_REF"),
                refPaymentMethod
        );
    }

    public void setRefPaymentMethod(String refPaymentMethod) {
        this.refPaymentMethod = refPaymentMethod;
    }

    public String getReferenceEntity() {
        return firstNonBlank(env("SECRETARIAPAY_APPYPAY_REFERENCE_ENTITY"), env("APPYPAY_REFERENCE_ENTITY"));
    }

    private String env(String name) {
        String value = System.getenv(name);
        return value == null ? "" : value.trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
