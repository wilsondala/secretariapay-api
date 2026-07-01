package com.secretariapay.api.dto.report;

import java.time.LocalDateTime;
import java.util.UUID;

public class OperationalReportResponse {

    private UUID companyId;
    private String scope;

    private LocalDateTime periodStartAt;
    private LocalDateTime periodEndAt;

    private long totalTickets;
    private long validTickets;
    private long usedTickets;
    private long cancelledTickets;

    private long publicValidations;
    private long successfulPublicValidations;
    private long failedPublicValidations;

    private long boardingAttempts;
    private long successfulBoardings;
    private long failedBoardings;

    private long suspiciousAttempts;

    private LocalDateTime generatedAt;

    public UUID getCompanyId() {
        return companyId;
    }

    public OperationalReportResponse setCompanyId(UUID companyId) {
        this.companyId = companyId;
        return this;
    }

    public String getScope() {
        return scope;
    }

    public OperationalReportResponse setScope(String scope) {
        this.scope = scope;
        return this;
    }

    public LocalDateTime getPeriodStartAt() {
        return periodStartAt;
    }

    public OperationalReportResponse setPeriodStartAt(LocalDateTime periodStartAt) {
        this.periodStartAt = periodStartAt;
        return this;
    }

    public LocalDateTime getPeriodEndAt() {
        return periodEndAt;
    }

    public OperationalReportResponse setPeriodEndAt(LocalDateTime periodEndAt) {
        this.periodEndAt = periodEndAt;
        return this;
    }

    public long getTotalTickets() {
        return totalTickets;
    }

    public OperationalReportResponse setTotalTickets(long totalTickets) {
        this.totalTickets = totalTickets;
        return this;
    }

    public long getValidTickets() {
        return validTickets;
    }

    public OperationalReportResponse setValidTickets(long validTickets) {
        this.validTickets = validTickets;
        return this;
    }

    public long getUsedTickets() {
        return usedTickets;
    }

    public OperationalReportResponse setUsedTickets(long usedTickets) {
        this.usedTickets = usedTickets;
        return this;
    }

    public long getCancelledTickets() {
        return cancelledTickets;
    }

    public OperationalReportResponse setCancelledTickets(long cancelledTickets) {
        this.cancelledTickets = cancelledTickets;
        return this;
    }

    public long getPublicValidations() {
        return publicValidations;
    }

    public OperationalReportResponse setPublicValidations(long publicValidations) {
        this.publicValidations = publicValidations;
        return this;
    }

    public long getSuccessfulPublicValidations() {
        return successfulPublicValidations;
    }

    public OperationalReportResponse setSuccessfulPublicValidations(long successfulPublicValidations) {
        this.successfulPublicValidations = successfulPublicValidations;
        return this;
    }

    public long getFailedPublicValidations() {
        return failedPublicValidations;
    }

    public OperationalReportResponse setFailedPublicValidations(long failedPublicValidations) {
        this.failedPublicValidations = failedPublicValidations;
        return this;
    }

    public long getBoardingAttempts() {
        return boardingAttempts;
    }

    public OperationalReportResponse setBoardingAttempts(long boardingAttempts) {
        this.boardingAttempts = boardingAttempts;
        return this;
    }

    public long getSuccessfulBoardings() {
        return successfulBoardings;
    }

    public OperationalReportResponse setSuccessfulBoardings(long successfulBoardings) {
        this.successfulBoardings = successfulBoardings;
        return this;
    }

    public long getFailedBoardings() {
        return failedBoardings;
    }

    public OperationalReportResponse setFailedBoardings(long failedBoardings) {
        this.failedBoardings = failedBoardings;
        return this;
    }

    public long getSuspiciousAttempts() {
        return suspiciousAttempts;
    }

    public OperationalReportResponse setSuspiciousAttempts(long suspiciousAttempts) {
        this.suspiciousAttempts = suspiciousAttempts;
        return this;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public OperationalReportResponse setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
        return this;
    }
}
