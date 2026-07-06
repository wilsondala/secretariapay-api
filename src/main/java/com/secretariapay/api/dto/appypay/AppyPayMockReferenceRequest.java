package com.secretariapay.api.dto.appypay;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AppyPayMockReferenceRequest {

    @Size(max = 20, message = "A entidade deve ter no máximo 20 caracteres.")
    private String entity;

    @NotBlank(message = "A referência é obrigatória.")
    @Size(max = 80, message = "A referência deve ter no máximo 80 caracteres.")
    private String referenceNumber;

    public String getEntity() {
        return entity;
    }

    public AppyPayMockReferenceRequest setEntity(String entity) {
        this.entity = entity;
        return this;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public AppyPayMockReferenceRequest setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
        return this;
    }
}
