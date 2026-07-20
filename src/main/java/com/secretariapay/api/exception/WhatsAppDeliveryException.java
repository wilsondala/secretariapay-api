package com.secretariapay.api.exception;

public class WhatsAppDeliveryException extends RuntimeException {

    private final Integer providerHttpStatus;

    public WhatsAppDeliveryException(String message, Integer providerHttpStatus) {
        super(message);
        this.providerHttpStatus = providerHttpStatus;
    }

    public Integer getProviderHttpStatus() {
        return providerHttpStatus;
    }
}
