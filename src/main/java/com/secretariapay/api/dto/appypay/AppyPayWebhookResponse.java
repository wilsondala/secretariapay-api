package com.secretariapay.api.dto.appypay;

public class AppyPayWebhookResponse {

    private boolean received;
    private boolean processed;
    private boolean paid;
    private String merchantTransactionId;
    private String chargeCode;
    private String providerStatus;
    private String message;
    private String receiptCode;
    private String receiptPdfUrl;

    public boolean isReceived() {
        return received;
    }

    public AppyPayWebhookResponse setReceived(boolean received) {
        this.received = received;
        return this;
    }

    public boolean isProcessed() {
        return processed;
    }

    public AppyPayWebhookResponse setProcessed(boolean processed) {
        this.processed = processed;
        return this;
    }

    public boolean isPaid() {
        return paid;
    }

    public AppyPayWebhookResponse setPaid(boolean paid) {
        this.paid = paid;
        return this;
    }

    public String getMerchantTransactionId() {
        return merchantTransactionId;
    }

    public AppyPayWebhookResponse setMerchantTransactionId(String merchantTransactionId) {
        this.merchantTransactionId = merchantTransactionId;
        return this;
    }

    public String getChargeCode() {
        return chargeCode;
    }

    public AppyPayWebhookResponse setChargeCode(String chargeCode) {
        this.chargeCode = chargeCode;
        return this;
    }

    public String getProviderStatus() {
        return providerStatus;
    }

    public AppyPayWebhookResponse setProviderStatus(String providerStatus) {
        this.providerStatus = providerStatus;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public AppyPayWebhookResponse setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getReceiptCode() {
        return receiptCode;
    }

    public AppyPayWebhookResponse setReceiptCode(String receiptCode) {
        this.receiptCode = receiptCode;
        return this;
    }

    public String getReceiptPdfUrl() {
        return receiptPdfUrl;
    }

    public AppyPayWebhookResponse setReceiptPdfUrl(String receiptPdfUrl) {
        this.receiptPdfUrl = receiptPdfUrl;
        return this;
    }
}
