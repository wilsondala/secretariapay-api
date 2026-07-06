package com.secretariapay.api.dto.gateway;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public class GatewayChargeResponse {
    public boolean success;
    public String message;
    public String paymentMethod;
    public UUID chargeId;
    public String chargeCode;
    public String merchantTransactionId;
    public BigDecimal amount;
    public String currency;
    public String referenceEntity;
    public String referenceNumber;
    public String providerStatus;
    public Integer providerHttpStatus;
    public String providerError;
    public Map<String, Object> providerResponse;
}
