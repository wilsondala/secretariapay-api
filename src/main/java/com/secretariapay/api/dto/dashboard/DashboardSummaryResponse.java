package com.secretariapay.api.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DashboardSummaryResponse {

    private long totalTransportCompanies;
    private long totalRoutes;
    private long totalTrips;
    private long totalPassengers;
    private long totalBookings;
    private long totalPayments;
    private long totalTickets;

    private long pendingBookings;
    private long paidBookings;
    private long issuedTicketBookings;
    private long expiredBookings;
    private long cancelledBookings;

    private long pendingPayments;
    private long paidPayments;
    private long expiredPayments;
    private long cancelledPayments;

    private long validTickets;
    private long usedTickets;
    private long cancelledTickets;

    private BigDecimal confirmedRevenue;
    private LocalDateTime generatedAt;

    public long getTotalTransportCompanies() {
        return totalTransportCompanies;
    }

    public DashboardSummaryResponse setTotalTransportCompanies(long totalTransportCompanies) {
        this.totalTransportCompanies = totalTransportCompanies;
        return this;
    }

    public long getTotalRoutes() {
        return totalRoutes;
    }

    public DashboardSummaryResponse setTotalRoutes(long totalRoutes) {
        this.totalRoutes = totalRoutes;
        return this;
    }

    public long getTotalTrips() {
        return totalTrips;
    }

    public DashboardSummaryResponse setTotalTrips(long totalTrips) {
        this.totalTrips = totalTrips;
        return this;
    }

    public long getTotalPassengers() {
        return totalPassengers;
    }

    public DashboardSummaryResponse setTotalPassengers(long totalPassengers) {
        this.totalPassengers = totalPassengers;
        return this;
    }

    public long getTotalBookings() {
        return totalBookings;
    }

    public DashboardSummaryResponse setTotalBookings(long totalBookings) {
        this.totalBookings = totalBookings;
        return this;
    }

    public long getTotalPayments() {
        return totalPayments;
    }

    public DashboardSummaryResponse setTotalPayments(long totalPayments) {
        this.totalPayments = totalPayments;
        return this;
    }

    public long getTotalTickets() {
        return totalTickets;
    }

    public DashboardSummaryResponse setTotalTickets(long totalTickets) {
        this.totalTickets = totalTickets;
        return this;
    }

    public long getPendingBookings() {
        return pendingBookings;
    }

    public DashboardSummaryResponse setPendingBookings(long pendingBookings) {
        this.pendingBookings = pendingBookings;
        return this;
    }

    public long getPaidBookings() {
        return paidBookings;
    }

    public DashboardSummaryResponse setPaidBookings(long paidBookings) {
        this.paidBookings = paidBookings;
        return this;
    }

    public long getIssuedTicketBookings() {
        return issuedTicketBookings;
    }

    public DashboardSummaryResponse setIssuedTicketBookings(long issuedTicketBookings) {
        this.issuedTicketBookings = issuedTicketBookings;
        return this;
    }

    public long getExpiredBookings() {
        return expiredBookings;
    }

    public DashboardSummaryResponse setExpiredBookings(long expiredBookings) {
        this.expiredBookings = expiredBookings;
        return this;
    }

    public long getCancelledBookings() {
        return cancelledBookings;
    }

    public DashboardSummaryResponse setCancelledBookings(long cancelledBookings) {
        this.cancelledBookings = cancelledBookings;
        return this;
    }

    public long getPendingPayments() {
        return pendingPayments;
    }

    public DashboardSummaryResponse setPendingPayments(long pendingPayments) {
        this.pendingPayments = pendingPayments;
        return this;
    }

    public long getPaidPayments() {
        return paidPayments;
    }

    public DashboardSummaryResponse setPaidPayments(long paidPayments) {
        this.paidPayments = paidPayments;
        return this;
    }

    public long getExpiredPayments() {
        return expiredPayments;
    }

    public DashboardSummaryResponse setExpiredPayments(long expiredPayments) {
        this.expiredPayments = expiredPayments;
        return this;
    }

    public long getCancelledPayments() {
        return cancelledPayments;
    }

    public DashboardSummaryResponse setCancelledPayments(long cancelledPayments) {
        this.cancelledPayments = cancelledPayments;
        return this;
    }

    public long getValidTickets() {
        return validTickets;
    }

    public DashboardSummaryResponse setValidTickets(long validTickets) {
        this.validTickets = validTickets;
        return this;
    }

    public long getUsedTickets() {
        return usedTickets;
    }

    public DashboardSummaryResponse setUsedTickets(long usedTickets) {
        this.usedTickets = usedTickets;
        return this;
    }

    public long getCancelledTickets() {
        return cancelledTickets;
    }

    public DashboardSummaryResponse setCancelledTickets(long cancelledTickets) {
        this.cancelledTickets = cancelledTickets;
        return this;
    }

    public BigDecimal getConfirmedRevenue() {
        return confirmedRevenue;
    }

    public DashboardSummaryResponse setConfirmedRevenue(BigDecimal confirmedRevenue) {
        this.confirmedRevenue = confirmedRevenue;
        return this;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public DashboardSummaryResponse setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
        return this;
    }
}
