package com.secretariapay.api.dto.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TripReportResponse {

    private UUID tripId;

    private UUID companyId;
    private String companyName;
    private String companyTradeName;

    private String originCity;
    private String originState;
    private String originTerminal;

    private String destinationCity;
    private String destinationState;
    private String destinationTerminal;

    private LocalDateTime departureAt;
    private LocalDateTime arrivalAt;

    private Integer totalSeats;
    private Integer availableSeats;
    private Integer occupiedSeats;

    private String currency;

    private long totalBookings;
    private long paidBookings;
    private long pendingBookings;
    private long cancelledBookings;
    private long expiredBookings;

    private long issuedTickets;
    private long validTickets;
    private long usedTickets;
    private long cancelledTickets;

    private BigDecimal totalRevenue;
    private BigDecimal averageTicketAmount;
    private BigDecimal occupancyRatePercentage;
    private BigDecimal checkInRatePercentage;

    private LocalDateTime generatedAt;

    public UUID getTripId() {
        return tripId;
    }

    public TripReportResponse setTripId(UUID tripId) {
        this.tripId = tripId;
        return this;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public TripReportResponse setCompanyId(UUID companyId) {
        this.companyId = companyId;
        return this;
    }

    public String getCompanyName() {
        return companyName;
    }

    public TripReportResponse setCompanyName(String companyName) {
        this.companyName = companyName;
        return this;
    }

    public String getCompanyTradeName() {
        return companyTradeName;
    }

    public TripReportResponse setCompanyTradeName(String companyTradeName) {
        this.companyTradeName = companyTradeName;
        return this;
    }

    public String getOriginCity() {
        return originCity;
    }

    public TripReportResponse setOriginCity(String originCity) {
        this.originCity = originCity;
        return this;
    }

    public String getOriginState() {
        return originState;
    }

    public TripReportResponse setOriginState(String originState) {
        this.originState = originState;
        return this;
    }

    public String getOriginTerminal() {
        return originTerminal;
    }

    public TripReportResponse setOriginTerminal(String originTerminal) {
        this.originTerminal = originTerminal;
        return this;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public TripReportResponse setDestinationCity(String destinationCity) {
        this.destinationCity = destinationCity;
        return this;
    }

    public String getDestinationState() {
        return destinationState;
    }

    public TripReportResponse setDestinationState(String destinationState) {
        this.destinationState = destinationState;
        return this;
    }

    public String getDestinationTerminal() {
        return destinationTerminal;
    }

    public TripReportResponse setDestinationTerminal(String destinationTerminal) {
        this.destinationTerminal = destinationTerminal;
        return this;
    }

    public LocalDateTime getDepartureAt() {
        return departureAt;
    }

    public TripReportResponse setDepartureAt(LocalDateTime departureAt) {
        this.departureAt = departureAt;
        return this;
    }

    public LocalDateTime getArrivalAt() {
        return arrivalAt;
    }

    public TripReportResponse setArrivalAt(LocalDateTime arrivalAt) {
        this.arrivalAt = arrivalAt;
        return this;
    }

    public Integer getTotalSeats() {
        return totalSeats;
    }

    public TripReportResponse setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats;
        return this;
    }

    public Integer getAvailableSeats() {
        return availableSeats;
    }

    public TripReportResponse setAvailableSeats(Integer availableSeats) {
        this.availableSeats = availableSeats;
        return this;
    }

    public Integer getOccupiedSeats() {
        return occupiedSeats;
    }

    public TripReportResponse setOccupiedSeats(Integer occupiedSeats) {
        this.occupiedSeats = occupiedSeats;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public TripReportResponse setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public long getTotalBookings() {
        return totalBookings;
    }

    public TripReportResponse setTotalBookings(long totalBookings) {
        this.totalBookings = totalBookings;
        return this;
    }

    public long getPaidBookings() {
        return paidBookings;
    }

    public TripReportResponse setPaidBookings(long paidBookings) {
        this.paidBookings = paidBookings;
        return this;
    }

    public long getPendingBookings() {
        return pendingBookings;
    }

    public TripReportResponse setPendingBookings(long pendingBookings) {
        this.pendingBookings = pendingBookings;
        return this;
    }

    public long getCancelledBookings() {
        return cancelledBookings;
    }

    public TripReportResponse setCancelledBookings(long cancelledBookings) {
        this.cancelledBookings = cancelledBookings;
        return this;
    }

    public long getExpiredBookings() {
        return expiredBookings;
    }

    public TripReportResponse setExpiredBookings(long expiredBookings) {
        this.expiredBookings = expiredBookings;
        return this;
    }

    public long getIssuedTickets() {
        return issuedTickets;
    }

    public TripReportResponse setIssuedTickets(long issuedTickets) {
        this.issuedTickets = issuedTickets;
        return this;
    }

    public long getValidTickets() {
        return validTickets;
    }

    public TripReportResponse setValidTickets(long validTickets) {
        this.validTickets = validTickets;
        return this;
    }

    public long getUsedTickets() {
        return usedTickets;
    }

    public TripReportResponse setUsedTickets(long usedTickets) {
        this.usedTickets = usedTickets;
        return this;
    }

    public long getCancelledTickets() {
        return cancelledTickets;
    }

    public TripReportResponse setCancelledTickets(long cancelledTickets) {
        this.cancelledTickets = cancelledTickets;
        return this;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public TripReportResponse setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
        return this;
    }

    public BigDecimal getAverageTicketAmount() {
        return averageTicketAmount;
    }

    public TripReportResponse setAverageTicketAmount(BigDecimal averageTicketAmount) {
        this.averageTicketAmount = averageTicketAmount;
        return this;
    }

    public BigDecimal getOccupancyRatePercentage() {
        return occupancyRatePercentage;
    }

    public TripReportResponse setOccupancyRatePercentage(BigDecimal occupancyRatePercentage) {
        this.occupancyRatePercentage = occupancyRatePercentage;
        return this;
    }

    public BigDecimal getCheckInRatePercentage() {
        return checkInRatePercentage;
    }

    public TripReportResponse setCheckInRatePercentage(BigDecimal checkInRatePercentage) {
        this.checkInRatePercentage = checkInRatePercentage;
        return this;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public TripReportResponse setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
        return this;
    }
}
