package com.secretariapay.api.dto.booking;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import com.secretariapay.api.entity.enums.PassengerFareType;
import com.secretariapay.api.entity.enums.TripSegmentType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class BookingRequest {

    @NotNull(message = "A viagem é obrigatória.")
    private UUID tripId;

    @NotNull(message = "O passageiro é obrigatório.")
    private UUID passengerId;

    @NotNull(message = "O número da poltrona é obrigatório.")
    @Min(value = 1, message = "A poltrona deve ser maior que zero.")
    @Max(value = 99, message = "A poltrona deve ter no máximo 99.")
    private Integer seatNumber;

    private PassengerFareType passengerFareType;
    private TripSegmentType tripSegmentType;

    private String childGuardianName;
    private String childGuardianPhone;

    private String minorGuardianName;
    private String minorGuardianPhone;
    private String minorPickupResponsibleName;
    private String minorPickupResponsiblePhone;

    public String getChildGuardianName() {
        return childGuardianName;
    }

    public BookingRequest setChildGuardianName(String childGuardianName) {
        this.childGuardianName = childGuardianName;
        return this;
    }

    public String getChildGuardianPhone() {
        return childGuardianPhone;
    }

    public BookingRequest setChildGuardianPhone(String childGuardianPhone) {
        this.childGuardianPhone = childGuardianPhone;
        return this;
    }

    public TripSegmentType getTripSegmentType() {
        return tripSegmentType;
    }

    public BookingRequest setTripSegmentType(TripSegmentType tripSegmentType) {
        this.tripSegmentType = tripSegmentType;
        return this;
    }

    public String getMinorGuardianName() {
        return minorGuardianName;
    }

    public BookingRequest setMinorGuardianName(String minorGuardianName) {
        this.minorGuardianName = minorGuardianName;
        return this;
    }

    public String getMinorGuardianPhone() {
        return minorGuardianPhone;
    }

    public BookingRequest setMinorGuardianPhone(String minorGuardianPhone) {
        this.minorGuardianPhone = minorGuardianPhone;
        return this;
    }

    public String getMinorPickupResponsibleName() {
        return minorPickupResponsibleName;
    }

    public BookingRequest setMinorPickupResponsibleName(String minorPickupResponsibleName) {
        this.minorPickupResponsibleName = minorPickupResponsibleName;
        return this;
    }

    public String getMinorPickupResponsiblePhone() {
        return minorPickupResponsiblePhone;
    }

    public BookingRequest setMinorPickupResponsiblePhone(String minorPickupResponsiblePhone) {
        this.minorPickupResponsiblePhone = minorPickupResponsiblePhone;
        return this;
    }

    public UUID getTripId() {
        return tripId;
    }

    public BookingRequest setTripId(UUID tripId) {
        this.tripId = tripId;
        return this;
    }

    public UUID getPassengerId() {
        return passengerId;
    }

    public BookingRequest setPassengerId(UUID passengerId) {
        this.passengerId = passengerId;
        return this;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public PassengerFareType getPassengerFareType() {
        return passengerFareType;
    }

    public BookingRequest setPassengerFareType(PassengerFareType passengerFareType) {
        this.passengerFareType = passengerFareType;
        return this;
    }

    public BookingRequest setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
        return this;
    }
}
