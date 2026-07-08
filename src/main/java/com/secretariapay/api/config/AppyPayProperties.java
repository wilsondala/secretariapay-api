package com.secretariapay.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "secretariapay.appypay")
public class AppyPayProperties {

    private boolean enabled = false;
    private String tokenUrl = "https://login.microsoftonline.com/appypaydev.onmicrosoft.com/oauth2/token";
    private String chargesUrl = "https://gwy-api-tst.appypay.co.ao/v2.0/charges";
    private String clientId = "";
    private String clientSecret = "";
    private String resource = "2aed7612-de64-46b5-9e59-1f48f8902d14";
    private String gpoPaymentMethod = "";
    private String refPaymentMethod = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    public String getChargesUrl() {
        return chargesUrl;
    }

    public void setChargesUrl(String chargesUrl) {
        this.chargesUrl = chargesUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getGpoPaymentMethod() {
        return gpoPaymentMethod;
    }

    public void setGpoPaymentMethod(String gpoPaymentMethod) {
        this.gpoPaymentMethod = gpoPaymentMethod;
    }

    public String getRefPaymentMethod() {
        return refPaymentMethod;
    }

    public void setRefPaymentMethod(String refPaymentMethod) {
        this.refPaymentMethod = refPaymentMethod;
    }
}
