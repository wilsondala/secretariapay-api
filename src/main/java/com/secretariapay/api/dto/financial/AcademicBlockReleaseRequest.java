package com.secretariapay.api.dto.financial;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public class AcademicBlockReleaseRequest {

    private UUID releasedByUserId;

    @Size(max = 1000, message = "A observação deve ter no máximo 1000 caracteres.")
    private String releaseNote;

    public UUID getReleasedByUserId() {
        return releasedByUserId;
    }

    public AcademicBlockReleaseRequest setReleasedByUserId(UUID releasedByUserId) {
        this.releasedByUserId = releasedByUserId;
        return this;
    }

    public String getReleaseNote() {
        return releaseNote;
    }

    public AcademicBlockReleaseRequest setReleaseNote(String releaseNote) {
        this.releaseNote = releaseNote;
        return this;
    }
}
