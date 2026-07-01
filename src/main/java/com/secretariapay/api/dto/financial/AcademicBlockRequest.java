package com.secretariapay.api.dto.financial;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class AcademicBlockRequest {

    @NotNull(message = "O estudante é obrigatório.")
    private UUID studentId;

    private UUID chargeId;

    @NotBlank(message = "O serviço bloqueado é obrigatório.")
    @Size(max = 120, message = "O serviço bloqueado deve ter no máximo 120 caracteres.")
    private String blockedService;

    @NotBlank(message = "O motivo do bloqueio é obrigatório.")
    private String reason;

    private UUID blockedByUserId;

    public UUID getStudentId() {
        return studentId;
    }

    public AcademicBlockRequest setStudentId(UUID studentId) {
        this.studentId = studentId;
        return this;
    }

    public UUID getChargeId() {
        return chargeId;
    }

    public AcademicBlockRequest setChargeId(UUID chargeId) {
        this.chargeId = chargeId;
        return this;
    }

    public String getBlockedService() {
        return blockedService;
    }

    public AcademicBlockRequest setBlockedService(String blockedService) {
        this.blockedService = blockedService;
        return this;
    }

    public String getReason() {
        return reason;
    }

    public AcademicBlockRequest setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public UUID getBlockedByUserId() {
        return blockedByUserId;
    }

    public AcademicBlockRequest setBlockedByUserId(UUID blockedByUserId) {
        this.blockedByUserId = blockedByUserId;
        return this;
    }
}
