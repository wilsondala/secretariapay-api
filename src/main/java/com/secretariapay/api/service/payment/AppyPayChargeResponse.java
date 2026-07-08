package com.secretariapay.api.service.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class AppyPayChargeResponse {

    private boolean success;
    private boolean appyPayEnabled;
    private String status;
    private String message;
    private String paymentMethod;
    private String merchantTransactionId;
    private String providerChargeId;
    private BigDecimal amount;
    private String currency;
    private String rawResponse;
    private Map<String, Object> providerData = new LinkedHashMap<>();
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean isSuccess() {
        return success;
    }

    public AppyPayChargeResponse setSuccess(boolean success) {
        this.success = success;
        return this;
    }

    public boolean isAppyPayEnabled() {
        return appyPayEnabled;
    }

    public AppyPayChargeResponse setAppyPayEnabled(boolean appyPayEnabled) {
        this.appyPayEnabled = appyPayEnabled;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public AppyPayChargeResponse setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public AppyPayChargeResponse setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public AppyPayChargeResponse setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
        return this;
    }

    public String getMerchantTransactionId() {
        return merchantTransactionId;
    }

    public AppyPayChargeResponse setMerchantTransactionId(String merchantTransactionId) {
        this.merchantTransactionId = merchantTransactionId;
        return this;
    }

    public String getProviderChargeId() {
        return providerChargeId;
    }

    public AppyPayChargeResponse setProviderChargeId(String providerChargeId) {
        this.providerChargeId = providerChargeId;
        return this;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public AppyPayChargeResponse setAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public AppyPayChargeResponse setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public AppyPayChargeResponse setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
        return this;
    }

    public Map<String, Object> getProviderData() {
        return providerData;
    }

    public AppyPayChargeResponse setProviderData(Map<String, Object> providerData) {
        this.providerData = providerData == null ? new LinkedHashMap<>() : providerData;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public AppyPayChargeResponse setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }
}
