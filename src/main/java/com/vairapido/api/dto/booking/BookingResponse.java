package com.vairapido.api.dto.booking;

import com.vairapido.api.entity.enums.BookingStatus;
import com.vairapido.api.entity.enums.PassengerFareType;
import com.vairapido.api.entity.enums.TripSegmentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class BookingResponse {

    private UUID id;
    private String bookingCode;

    private UUID tripId;
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

    private UUID passengerId;
    private String passengerName;
    private String passengerDocument;
    private String passengerEmail;
    private String passengerWhatsapp;

    private Integer seatNumber;
    private PassengerFareType passengerFareType;
    private Integer passengerAge;
    private BigDecimal farePercentage;
    private TripSegmentType tripSegmentType;
    private String childGuardianName;
    private String childGuardianPhone;

    private String minorGuardianName;
    private String minorGuardianPhone;
    private String minorPickupResponsibleName;
    private String minorPickupResponsiblePhone;
    private BookingStatus status;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime expiresAt;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public BookingResponse setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public BookingResponse setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
        return this;
    }

    public UUID getTripId() {
        return tripId;
    }

    public BookingResponse setTripId(UUID tripId) {
        this.tripId = tripId;
        return this;
    }

    public String getCompanyName() {
        return companyName;
    }

    public BookingResponse setCompanyName(String companyName) {
        this.companyName = companyName;
        return this;
    }

    public String getCompanyTradeName() {
        return companyTradeName;
    }

    public BookingResponse setCompanyTradeName(String companyTradeName) {
        this.companyTradeName = companyTradeName;
        return this;
    }

    public String getOriginCity() {
        return originCity;
    }

    public BookingResponse setOriginCity(String originCity) {
        this.originCity = originCity;
        return this;
    }

    public String getOriginState() {
        return originState;
    }

    public BookingResponse setOriginState(String originState) {
        this.originState = originState;
        return this;
    }

    public String getOriginTerminal() {
        return originTerminal;
    }

    public BookingResponse setOriginTerminal(String originTerminal) {
        this.originTerminal = originTerminal;
        return this;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public BookingResponse setDestinationCity(String destinationCity) {
        this.destinationCity = destinationCity;
        return this;
    }

    public String getDestinationState() {
        return destinationState;
    }

    public BookingResponse setDestinationState(String destinationState) {
        this.destinationState = destinationState;
        return this;
    }

    public String getDestinationTerminal() {
        return destinationTerminal;
    }

    public BookingResponse setDestinationTerminal(String destinationTerminal) {
        this.destinationTerminal = destinationTerminal;
        return this;
    }

    public LocalDateTime getDepartureAt() {
        return departureAt;
    }

    public BookingResponse setDepartureAt(LocalDateTime departureAt) {
        this.departureAt = departureAt;
        return this;
    }

    public LocalDateTime getArrivalAt() {
        return arrivalAt;
    }

    public BookingResponse setArrivalAt(LocalDateTime arrivalAt) {
        this.arrivalAt = arrivalAt;
        return this;
    }

    public UUID getPassengerId() {
        return passengerId;
    }

    public BookingResponse setPassengerId(UUID passengerId) {
        this.passengerId = passengerId;
        return this;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public BookingResponse setPassengerName(String passengerName) {
        this.passengerName = passengerName;
        return this;
    }

    public String getPassengerDocument() {
        return passengerDocument;
    }

    public BookingResponse setPassengerDocument(String passengerDocument) {
        this.passengerDocument = passengerDocument;
        return this;
    }

    public String getPassengerEmail() {
        return passengerEmail;
    }

    public BookingResponse setPassengerEmail(String passengerEmail) {
        this.passengerEmail = passengerEmail;
        return this;
    }

    public String getPassengerWhatsapp() {
        return passengerWhatsapp;
    }

    public BookingResponse setPassengerWhatsapp(String passengerWhatsapp) {
        this.passengerWhatsapp = passengerWhatsapp;
        return this;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public BookingResponse setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
        return this;
    }

    public PassengerFareType getPassengerFareType() {
        return passengerFareType;
    }

    public BookingResponse setPassengerFareType(PassengerFareType passengerFareType) {
        this.passengerFareType = passengerFareType;
        return this;
    }

    public Integer getPassengerAge() {
        return passengerAge;
    }

    public BookingResponse setPassengerAge(Integer passengerAge) {
        this.passengerAge = passengerAge;
        return this;
    }

    public BigDecimal getFarePercentage() {
        return farePercentage;
    }

    public BookingResponse setFarePercentage(BigDecimal farePercentage) {
        this.farePercentage = farePercentage;
        return this;
    }

    public String getChildGuardianName() {
        return childGuardianName;
    }

    public BookingResponse setChildGuardianName(String childGuardianName) {
        this.childGuardianName = childGuardianName;
        return this;
    }

    public String getChildGuardianPhone() {
        return childGuardianPhone;
    }

    public BookingResponse setChildGuardianPhone(String childGuardianPhone) {
        this.childGuardianPhone = childGuardianPhone;
        return this;
    }

    public TripSegmentType getTripSegmentType() {
        return tripSegmentType;
    }

    public BookingResponse setTripSegmentType(TripSegmentType tripSegmentType) {
        this.tripSegmentType = tripSegmentType;
        return this;
    }

    public String getMinorGuardianName() {
        return minorGuardianName;
    }

    public BookingResponse setMinorGuardianName(String minorGuardianName) {
        this.minorGuardianName = minorGuardianName;
        return this;
    }

    public String getMinorGuardianPhone() {
        return minorGuardianPhone;
    }

    public BookingResponse setMinorGuardianPhone(String minorGuardianPhone) {
        this.minorGuardianPhone = minorGuardianPhone;
        return this;
    }

    public String getMinorPickupResponsibleName() {
        return minorPickupResponsibleName;
    }

    public BookingResponse setMinorPickupResponsibleName(String minorPickupResponsibleName) {
        this.minorPickupResponsibleName = minorPickupResponsibleName;
        return this;
    }

    public String getMinorPickupResponsiblePhone() {
        return minorPickupResponsiblePhone;
    }

    public BookingResponse setMinorPickupResponsiblePhone(String minorPickupResponsiblePhone) {
        this.minorPickupResponsiblePhone = minorPickupResponsiblePhone;
        return this;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public BookingResponse setStatus(BookingStatus status) {
        this.status = status;
        return this;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BookingResponse setAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public BookingResponse setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public BookingResponse setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public BookingResponse setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
        return this;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public BookingResponse setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public BookingResponse setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public BookingResponse setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}