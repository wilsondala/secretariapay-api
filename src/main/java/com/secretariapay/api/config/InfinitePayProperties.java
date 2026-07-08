package com.secretariapay.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "secretariapay.infinitepay")
public class InfinitePayProperties {

    private static final String DEFAULT_LINKS_URL = "https://api.checkout.infinitepay.io/links";
    private static final String DEFAULT_API_BASE_URL = "https://secretariapay-api.paixaoangola.com";

    private boolean enabled = false;
    private String handle = "";
    private String linksUrl = DEFAULT_LINKS_URL;
    private String successUrl = DEFAULT_API_BASE_URL + "/api/v1/public/infinitepay/success";
    private String cancelUrl = DEFAULT_API_BASE_URL + "/api/v1/public/infinitepay/cancel";
    private BigDecimal testAmountBrl = new BigDecimal("5.00");

    public boolean isEnabled() {
        return enabled || Boolean.parseBoolean(env("SECRETARIAPAY_INFINITEPAY_ENABLED"));
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHandle() {
        return firstNonBlank(env("SECRETARIAPAY_INFINITEPAY_HANDLE"), env("INFINITEPAY_HANDLE"), handle);
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public String getLinksUrl() {
        return firstNonBlank(env("SECRETARIAPAY_INFINITEPAY_LINKS_URL"), env("INFINITEPAY_LINKS_URL"), linksUrl, DEFAULT_LINKS_URL);
    }

    public void setLinksUrl(String linksUrl) {
        this.linksUrl = linksUrl;
    }

    public String getSuccessUrl() {
        return firstNonBlank(env("SECRETARIAPAY_INFINITEPAY_SUCCESS_URL"), env("INFINITEPAY_SUCCESS_URL"), successUrl);
    }

    public void setSuccessUrl(String successUrl) {
        this.successUrl = successUrl;
    }

    public String getCancelUrl() {
        return firstNonBlank(env("SECRETARIAPAY_INFINITEPAY_CANCEL_URL"), env("INFINITEPAY_CANCEL_URL"), cancelUrl);
    }

    public void setCancelUrl(String cancelUrl) {
        this.cancelUrl = cancelUrl;
    }

    public BigDecimal getTestAmountBrl() {
        String value = firstNonBlank(env("SECRETARIAPAY_INFINITEPAY_TEST_AMOUNT_BRL"), env("INFINITEPAY_TEST_AMOUNT_BRL"));
        if (!value.isBlank()) {
            try {
                return new BigDecimal(value.replace(",", "."));
            } catch (Exception ignored) {
                return testAmountBrl;
            }
        }
        return testAmountBrl == null ? new BigDecimal("5.00") : testAmountBrl;
    }

    public void setTestAmountBrl(BigDecimal testAmountBrl) {
        this.testAmountBrl = testAmountBrl;
    }

    private String env(String name) {
        String value = System.getenv(name);
        return value == null ? "" : value.trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
