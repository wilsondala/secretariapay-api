package com.secretariapay.api.dto.gateway;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class GatewayChargeRequest {

    @NotNull(message = "A cobrança é obrigatória.")
    private UUID chargeId;

    @Size(max = 40, message = "O telefone deve ter no máximo 40 caracteres.")
    private String phoneNumber;

    @Size(max = 180, message = "A descrição deve ter no máximo 180 caracteres.")
    private String description;

    public UUID getChargeId() { return chargeId; }
    public GatewayChargeRequest setChargeId(UUID chargeId) { this.chargeId = chargeId; return this; }
    public String getPhoneNumber() { return phoneNumber; }
    public GatewayChargeRequest setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; return this; }
    public String getDescription() { return description; }
    public GatewayChargeRequest setDescription(String description) { this.description = description; return this; }
}
