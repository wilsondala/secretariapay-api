package com.secretariapay.api.dto.financial;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class PaymentProofReviewRequest {

    @NotNull(message = "O utilizador responsável pela validação é obrigatório.")
    private UUID reviewedByUserId;

    @Size(max = 1000, message = "A observação deve ter no máximo 1000 caracteres.")
    private String reviewNote;

    public UUID getReviewedByUserId() {
        return reviewedByUserId;
    }

    public PaymentProofReviewRequest setReviewedByUserId(UUID reviewedByUserId) {
        this.reviewedByUserId = reviewedByUserId;
        return this;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public PaymentProofReviewRequest setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
        return this;
    }
}
