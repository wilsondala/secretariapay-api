package com.secretariapay.api.service.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class InfinitePayLinkPaymentResponse {

    private boolean success;
    private boolean enabled;
    private String status;
    private String message;
    private String orderNsu;
    private String checkoutUrl;
    private BigDecimal amountBrl;
    private String currency = "BRL";
    private String rawResponse;
    private Map<String, Object> providerData = new LinkedHashMap<>();
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean isSuccess() {
        return success;
    }

    public InfinitePayLinkPaymentResponse setSuccess(boolean success) {
        this.success = success;
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public InfinitePayLinkPaymentResponse setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public InfinitePayLinkPaymentResponse setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public InfinitePayLinkPaymentResponse setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getOrderNsu() {
        return orderNsu;
    }

    public InfinitePayLinkPaymentResponse setOrderNsu(String orderNsu) {
        this.orderNsu = orderNsu;
        return this;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public InfinitePayLinkPaymentResponse setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
        return this;
    }

    public BigDecimal getAmountBrl() {
        return amountBrl;
    }

    public InfinitePayLinkPaymentResponse setAmountBrl(BigDecimal amountBrl) {
        this.amountBrl = amountBrl;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public InfinitePayLinkPaymentResponse setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public InfinitePayLinkPaymentResponse setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
        return this;
    }

    public Map<String, Object> getProviderData() {
        return providerData;
    }

    public InfinitePayLinkPaymentResponse setProviderData(Map<String, Object> providerData) {
        this.providerData = providerData == null ? new LinkedHashMap<>() : providerData;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public InfinitePayLinkPaymentResponse setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }
}
