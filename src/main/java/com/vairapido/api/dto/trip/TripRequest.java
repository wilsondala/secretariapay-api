package com.vairapido.api.dto.trip;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TripRequest {

    @NotNull(message = "A empresa de transporte é obrigatória.")
    private UUID transportCompanyId;

    @NotNull(message = "A rota é obrigatória.")
    private UUID routeId;

    @NotNull(message = "A data/hora de saída é obrigatória.")
    @Future(message = "A data/hora de saída deve estar no futuro.")
    private LocalDateTime departureAt;

    @NotNull(message = "A data/hora de chegada é obrigatória.")
    private LocalDateTime arrivalAt;

    @NotNull(message = "O preço é obrigatório.")
    @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero.")
    private BigDecimal price;

    @Size(max = 10, message = "A moeda deve ter no máximo 10 caracteres.")
    private String currency;

    @NotNull(message = "O total de assentos é obrigatório.")
    @Min(value = 1, message = "O total de assentos deve ser maior que zero.")
    private Integer totalSeats;

    @Size(max = 30, message = "A placa do veículo deve ter no máximo 30 caracteres.")
    private String busPlate;

    @Size(max = 160, message = "A descrição do veículo deve ter no máximo 160 caracteres.")
    private String vehicleDescription;

    public UUID getTransportCompanyId() {
        return transportCompanyId;
    }

    public TripRequest setTransportCompanyId(UUID transportCompanyId) {
        this.transportCompanyId = transportCompanyId;
        return this;
    }

    public UUID getRouteId() {
        return routeId;
    }

    public TripRequest setRouteId(UUID routeId) {
        this.routeId = routeId;
        return this;
    }

    public LocalDateTime getDepartureAt() {
        return departureAt;
    }

    public TripRequest setDepartureAt(LocalDateTime departureAt) {
        this.departureAt = departureAt;
        return this;
    }

    public LocalDateTime getArrivalAt() {
        return arrivalAt;
    }

    public TripRequest setArrivalAt(LocalDateTime arrivalAt) {
        this.arrivalAt = arrivalAt;
        return this;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public TripRequest setPrice(BigDecimal price) {
        this.price = price;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public TripRequest setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public Integer getTotalSeats() {
        return totalSeats;
    }

    public TripRequest setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats;
        return this;
    }

    public String getBusPlate() {
        return busPlate;
    }

    public TripRequest setBusPlate(String busPlate) {
        this.busPlate = busPlate;
        return this;
    }

    public String getVehicleDescription() {
        return vehicleDescription;
    }

    public TripRequest setVehicleDescription(String vehicleDescription) {
        this.vehicleDescription = vehicleDescription;
        return this;
    }
}