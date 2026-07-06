package com.secretariapay.api.dto.appypay;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public class AppyPayChargeRequest {

    private UUID chargeId;

    @Size(max = 80, message = "O código da cobrança deve ter no máximo 80 caracteres.")
    private String chargeCode;

    @Size(max = 40, message = "O telefone deve ter no máximo 40 caracteres.")
    private String phoneNumber;

    @Size(max = 180, message = "A descrição deve ter no máximo 180 caracteres.")
    private String description;

    public UUID getChargeId() {
        return chargeId;
    }

    public AppyPayChargeRequest setChargeId(UUID chargeId) {
        this.chargeId = chargeId;
        return this;
    }

    public String getChargeCode() {
        return chargeCode;
    }

    public AppyPayChargeRequest setChargeCode(String chargeCode) {
        this.chargeCode = chargeCode;
        return this;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public AppyPayChargeRequest setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public AppyPayChargeRequest setDescription(String description) {
        this.description = description;
        return this;
    }
}
