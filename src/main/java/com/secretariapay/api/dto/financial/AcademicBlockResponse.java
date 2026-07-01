package com.secretariapay.api.dto.financial;

import com.secretariapay.api.entity.enums.financial.AcademicBlockStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class AcademicBlockResponse {

    private UUID id;
    private UUID studentId;
    private String studentName;
    private String studentNumber;
    private UUID chargeId;
    private String chargeCode;
    private String blockedService;
    private String reason;
    private AcademicBlockStatus status;
    private UUID blockedByUserId;
    private String blockedByName;
    private LocalDateTime blockedAt;
    private UUID releasedByUserId;
    private String releasedByName;
    private LocalDateTime releasedAt;
    private String releaseNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public AcademicBlockResponse setId(UUID id) {
        this.id = id;
        return this;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public AcademicBlockResponse setStudentId(UUID studentId) {
        this.studentId = studentId;
        return this;
    }

    public String getStudentName() {
        return studentName;
    }

    public AcademicBlockResponse setStudentName(String studentName) {
        this.studentName = studentName;
        return this;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public AcademicBlockResponse setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
        return this;
    }

    public UUID getChargeId() {
        return chargeId;
    }

    public AcademicBlockResponse setChargeId(UUID chargeId) {
        this.chargeId = chargeId;
        return this;
    }

    public String getChargeCode() {
        return chargeCode;
    }

    public AcademicBlockResponse setChargeCode(String chargeCode) {
        this.chargeCode = chargeCode;
        return this;
    }

    public String getBlockedService() {
        return blockedService;
    }

    public AcademicBlockResponse setBlockedService(String blockedService) {
        this.blockedService = blockedService;
        return this;
    }

    public String getReason() {
        return reason;
    }

    public AcademicBlockResponse setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public AcademicBlockStatus getStatus() {
        return status;
    }

    public AcademicBlockResponse setStatus(AcademicBlockStatus status) {
        this.status = status;
        return this;
    }

    public UUID getBlockedByUserId() {
        return blockedByUserId;
    }

    public AcademicBlockResponse setBlockedByUserId(UUID blockedByUserId) {
        this.blockedByUserId = blockedByUserId;
        return this;
    }

    public String getBlockedByName() {
        return blockedByName;
    }

    public AcademicBlockResponse setBlockedByName(String blockedByName) {
        this.blockedByName = blockedByName;
        return this;
    }

    public LocalDateTime getBlockedAt() {
        return blockedAt;
    }

    public AcademicBlockResponse setBlockedAt(LocalDateTime blockedAt) {
        this.blockedAt = blockedAt;
        return this;
    }

    public UUID getReleasedByUserId() {
        return releasedByUserId;
    }

    public AcademicBlockResponse setReleasedByUserId(UUID releasedByUserId) {
        this.releasedByUserId = releasedByUserId;
        return this;
    }

    public String getReleasedByName() {
        return releasedByName;
    }

    public AcademicBlockResponse setReleasedByName(String releasedByName) {
        this.releasedByName = releasedByName;
        return this;
    }

    public LocalDateTime getReleasedAt() {
        return releasedAt;
    }

    public AcademicBlockResponse setReleasedAt(LocalDateTime releasedAt) {
        this.releasedAt = releasedAt;
        return this;
    }

    public String getReleaseNote() {
        return releaseNote;
    }

    public AcademicBlockResponse setReleaseNote(String releaseNote) {
        this.releaseNote = releaseNote;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public AcademicBlockResponse setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public AcademicBlockResponse setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}
