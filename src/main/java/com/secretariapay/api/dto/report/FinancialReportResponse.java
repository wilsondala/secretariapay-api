package com.secretariapay.api.dto.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class FinancialReportResponse {

    private UUID companyId;
    private String scope;
    private String currency;

    private LocalDateTime periodStartAt;
    private LocalDateTime periodEndAt;

    private long paidBookings;
    private long pendingBookings;
    private long cancelledBookings;
    private long expiredBookings;
    private long issuedTickets;

    private BigDecimal totalRevenue;
    private BigDecimal averageTicketAmount;

    private LocalDateTime generatedAt;

    public UUID getCompanyId() {
        return companyId;
    }

    public FinancialReportResponse setCompanyId(UUID companyId) {
        this.companyId = companyId;
        return this;
    }

    public String getScope() {
        return scope;
    }

    public FinancialReportResponse setScope(String scope) {
        this.scope = scope;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public FinancialReportResponse setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public LocalDateTime getPeriodStartAt() {
        return periodStartAt;
    }

    public FinancialReportResponse setPeriodStartAt(LocalDateTime periodStartAt) {
        this.periodStartAt = periodStartAt;
        return this;
    }

    public LocalDateTime getPeriodEndAt() {
        return periodEndAt;
    }

    public FinancialReportResponse setPeriodEndAt(LocalDateTime periodEndAt) {
        this.periodEndAt = periodEndAt;
        return this;
    }

    public long getPaidBookings() {
        return paidBookings;
    }

    public FinancialReportResponse setPaidBookings(long paidBookings) {
        this.paidBookings = paidBookings;
        return this;
    }

    public long getPendingBookings() {
        return pendingBookings;
    }

    public FinancialReportResponse setPendingBookings(long pendingBookings) {
        this.pendingBookings = pendingBookings;
        return this;
    }

    public long getCancelledBookings() {
        return cancelledBookings;
    }

    public FinancialReportResponse setCancelledBookings(long cancelledBookings) {
        this.cancelledBookings = cancelledBookings;
        return this;
    }

    public long getExpiredBookings() {
        return expiredBookings;
    }

    public FinancialReportResponse setExpiredBookings(long expiredBookings) {
        this.expiredBookings = expiredBookings;
        return this;
    }

    public long getIssuedTickets() {
        return issuedTickets;
    }

    public FinancialReportResponse setIssuedTickets(long issuedTickets) {
        this.issuedTickets = issuedTickets;
        return this;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public FinancialReportResponse setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
        return this;
    }

    public BigDecimal getAverageTicketAmount() {
        return averageTicketAmount;
    }

    public FinancialReportResponse setAverageTicketAmount(BigDecimal averageTicketAmount) {
        this.averageTicketAmount = averageTicketAmount;
        return this;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public FinancialReportResponse setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
        return this;
    }
}
