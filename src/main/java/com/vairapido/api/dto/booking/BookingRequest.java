package com.vairapido.api.dto.booking;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    public BookingRequest setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
        return this;
    }
}