package com.secretariapay.api.dto.financial;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class PaymentProofReviewRequest {

    @JsonAlias({"reviewerId", "userId", "reviewedBy"})
    private UUID reviewedByUserId;

    @JsonAlias({"notes", "note", "reason", "observation", "observacao"})
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
