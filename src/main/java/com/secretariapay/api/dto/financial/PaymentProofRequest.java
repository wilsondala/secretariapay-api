package com.secretariapay.api.dto.financial;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class PaymentProofRequest {

    @NotNull(message = "A cobrança é obrigatória.")
    private UUID chargeId;

    @NotBlank(message = "A URL do comprovativo é obrigatória.")
    private String fileUrl;

    @Size(max = 180, message = "O nome do arquivo deve ter no máximo 180 caracteres.")
    private String fileName;

    @Size(max = 120, message = "O tipo do arquivo deve ter no máximo 120 caracteres.")
    private String mimeType;

    @Size(max = 40, message = "O telefone deve ter no máximo 40 caracteres.")
    private String submittedByPhone;

    public UUID getChargeId() {
        return chargeId;
    }

    public PaymentProofRequest setChargeId(UUID chargeId) {
        this.chargeId = chargeId;
        return this;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public PaymentProofRequest setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
        return this;
    }

    public String getFileName() {
        return fileName;
    }

    public PaymentProofRequest setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public String getMimeType() {
        return mimeType;
    }

    public PaymentProofRequest setMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    public String getSubmittedByPhone() {
        return submittedByPhone;
    }

    public PaymentProofRequest setSubmittedByPhone(String submittedByPhone) {
        this.submittedByPhone = submittedByPhone;
        return this;
    }
}
