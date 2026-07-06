package com.secretariapay.api.dto.gateway;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class GatewayMockReferenceRequest {
    @Size(max = 20)
    public String entity;

    @NotBlank
    @Size(max = 80)
    public String referenceNumber;
}
