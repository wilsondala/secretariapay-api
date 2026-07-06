package com.secretariapay.api.dto.appypay;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class AppyPayChargeResponse {

    private boolean success;
    private String message;
    private String paymentMethod;
    private UUID chargeId;
    private String chargeCode;
    private String merchantTransactionId;
    private BigDecimal amount;
    private String currency;
    private String referenceEntity;
    private String referenceNumber;
    private String providerStatus;
    private Integer providerHttpStatus;
    private String providerError;
    private boolean paid;
    private String receiptCode;
    private String receiptPdfUrl;
    private String receiptValidationUrl;
    private Map<String, Object> providerResponse = new LinkedHashMap<>();

    public boolean isSuccess() {
        return success;
    }

    public AppyPayChargeResponse setSuccess(boolean success) {
        this.success = success;
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

    public UUID getChargeId() {
        return chargeId;
    }

    public AppyPayChargeResponse setChargeId(UUID chargeId) {
        this.chargeId = chargeId;
        return this;
    }

    public String getChargeCode() {
        return chargeCode;
    }

    public AppyPayChargeResponse setChargeCode(String chargeCode) {
        this.chargeCode = chargeCode;
        return this;
    }

    public String getMerchantTransactionId() {
        return merchantTransactionId;
    }

    public AppyPayChargeResponse setMerchantTransactionId(String merchantTransactionId) {
        this.merchantTransactionId = merchantTransactionId;
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

    public String getReferenceEntity() {
        return referenceEntity;
    }

    public AppyPayChargeResponse setReferenceEntity(String referenceEntity) {
        this.referenceEntity = referenceEntity;
        return this;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public AppyPayChargeResponse setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
        return this;
    }

    public String getProviderStatus() {
        return providerStatus;
    }

    public AppyPayChargeResponse setProviderStatus(String providerStatus) {
        this.providerStatus = providerStatus;
        return this;
    }

    public Integer getProviderHttpStatus() {
        return providerHttpStatus;
    }

    public AppyPayChargeResponse setProviderHttpStatus(Integer providerHttpStatus) {
        this.providerHttpStatus = providerHttpStatus;
        return this;
    }

    public String getProviderError() {
        return providerError;
    }

    public AppyPayChargeResponse setProviderError(String providerError) {
        this.providerError = providerError;
        return this;
    }

    public boolean isPaid() {
        return paid;
    }

    public AppyPayChargeResponse setPaid(boolean paid) {
        this.paid = paid;
        return this;
    }

    public String getReceiptCode() {
        return receiptCode;
    }

    public AppyPayChargeResponse setReceiptCode(String receiptCode) {
        this.receiptCode = receiptCode;
        return this;
    }

    public String getReceiptPdfUrl() {
        return receiptPdfUrl;
    }

    public AppyPayChargeResponse setReceiptPdfUrl(String receiptPdfUrl) {
        this.receiptPdfUrl = receiptPdfUrl;
        return this;
    }

    public String getReceiptValidationUrl() {
        return receiptValidationUrl;
    }

    public AppyPayChargeResponse setReceiptValidationUrl(String receiptValidationUrl) {
        this.receiptValidationUrl = receiptValidationUrl;
        return this;
    }

    public Map<String, Object> getProviderResponse() {
        return providerResponse;
    }

    public AppyPayChargeResponse setProviderResponse(Map<String, Object> providerResponse) {
        this.providerResponse = providerResponse == null ? new LinkedHashMap<>() : providerResponse;
        return this;
    }
}
