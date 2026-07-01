package com.secretariapay.api.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FinancialDashboardResponse {

    private Long totalStudents;
    private Long blockedStudents;
    private Long pendingCharges;
    private Long paidCharges;
    private Long overdueCharges;
    private Long pendingPaymentProofs;
    private Long activeAcademicBlocks;

    private BigDecimal expectedRevenue;
    private BigDecimal receivedRevenue;
    private BigDecimal pendingRevenue;
    private BigDecimal overdueRevenue;

    private String currency;
    private LocalDateTime generatedAt;

    public Long getTotalStudents() {
        return totalStudents;
    }

    public FinancialDashboardResponse setTotalStudents(Long totalStudents) {
        this.totalStudents = totalStudents;
        return this;
    }

    public Long getBlockedStudents() {
        return blockedStudents;
    }

    public FinancialDashboardResponse setBlockedStudents(Long blockedStudents) {
        this.blockedStudents = blockedStudents;
        return this;
    }

    public Long getPendingCharges() {
        return pendingCharges;
    }

    public FinancialDashboardResponse setPendingCharges(Long pendingCharges) {
        this.pendingCharges = pendingCharges;
        return this;
    }

    public Long getPaidCharges() {
        return paidCharges;
    }

    public FinancialDashboardResponse setPaidCharges(Long paidCharges) {
        this.paidCharges = paidCharges;
        return this;
    }

    public Long getOverdueCharges() {
        return overdueCharges;
    }

    public FinancialDashboardResponse setOverdueCharges(Long overdueCharges) {
        this.overdueCharges = overdueCharges;
        return this;
    }

    public Long getPendingPaymentProofs() {
        return pendingPaymentProofs;
    }

    public FinancialDashboardResponse setPendingPaymentProofs(Long pendingPaymentProofs) {
        this.pendingPaymentProofs = pendingPaymentProofs;
        return this;
    }

    public Long getActiveAcademicBlocks() {
        return activeAcademicBlocks;
    }

    public FinancialDashboardResponse setActiveAcademicBlocks(Long activeAcademicBlocks) {
        this.activeAcademicBlocks = activeAcademicBlocks;
        return this;
    }

    public BigDecimal getExpectedRevenue() {
        return expectedRevenue;
    }

    public FinancialDashboardResponse setExpectedRevenue(BigDecimal expectedRevenue) {
        this.expectedRevenue = expectedRevenue;
        return this;
    }

    public BigDecimal getReceivedRevenue() {
        return receivedRevenue;
    }

    public FinancialDashboardResponse setReceivedRevenue(BigDecimal receivedRevenue) {
        this.receivedRevenue = receivedRevenue;
        return this;
    }

    public BigDecimal getPendingRevenue() {
        return pendingRevenue;
    }

    public FinancialDashboardResponse setPendingRevenue(BigDecimal pendingRevenue) {
        this.pendingRevenue = pendingRevenue;
        return this;
    }

    public BigDecimal getOverdueRevenue() {
        return overdueRevenue;
    }

    public FinancialDashboardResponse setOverdueRevenue(BigDecimal overdueRevenue) {
        this.overdueRevenue = overdueRevenue;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public FinancialDashboardResponse setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public FinancialDashboardResponse setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
        return this;
    }
}
