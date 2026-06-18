package com.vairapido.api.dto.trip;

import com.vairapido.api.entity.enums.TripStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TripResponse {

    private UUID id;

    private UUID transportCompanyId;
    private String transportCompanyName;
    private String transportCompanyTradeName;

    private UUID routeId;
    private String originCity;
    private String originState;
    private String originTerminal;
    private String destinationCity;
    private String destinationState;
    private String destinationTerminal;

    private LocalDateTime departureAt;
    private LocalDateTime arrivalAt;
    private BigDecimal price;
    private String currency;
    private Integer totalSeats;
    private Integer availableSeats;
    private String busPlate;
    private String vehicleDescription;
    private TripStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public TripResponse setId(UUID id) {
        this.id = id;
        return this;
    }

    public UUID getTransportCompanyId() {
        return transportCompanyId;
    }

    public TripResponse setTransportCompanyId(UUID transportCompanyId) {
        this.transportCompanyId = transportCompanyId;
        return this;
    }

    public String getTransportCompanyName() {
        return transportCompanyName;
    }

    public TripResponse setTransportCompanyName(String transportCompanyName) {
        this.transportCompanyName = transportCompanyName;
        return this;
    }

    public String getTransportCompanyTradeName() {
        return transportCompanyTradeName;
    }

    public TripResponse setTransportCompanyTradeName(String transportCompanyTradeName) {
        this.transportCompanyTradeName = transportCompanyTradeName;
        return this;
    }

    public UUID getRouteId() {
        return routeId;
    }

    public TripResponse setRouteId(UUID routeId) {
        this.routeId = routeId;
        return this;
    }

    public String getOriginCity() {
        return originCity;
    }

    public TripResponse setOriginCity(String originCity) {
        this.originCity = originCity;
        return this;
    }

    public String getOriginState() {
        return originState;
    }

    public TripResponse setOriginState(String originState) {
        this.originState = originState;
        return this;
    }

    public String getOriginTerminal() {
        return originTerminal;
    }

    public TripResponse setOriginTerminal(String originTerminal) {
        this.originTerminal = originTerminal;
        return this;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public TripResponse setDestinationCity(String destinationCity) {
        this.destinationCity = destinationCity;
        return this;
    }

    public String getDestinationState() {
        return destinationState;
    }

    public TripResponse setDestinationState(String destinationState) {
        this.destinationState = destinationState;
        return this;
    }

    public String getDestinationTerminal() {
        return destinationTerminal;
    }

    public TripResponse setDestinationTerminal(String destinationTerminal) {
        this.destinationTerminal = destinationTerminal;
        return this;
    }

    public LocalDateTime getDepartureAt() {
        return departureAt;
    }

    public TripResponse setDepartureAt(LocalDateTime departureAt) {
        this.departureAt = departureAt;
        return this;
    }

    public LocalDateTime getArrivalAt() {
        return arrivalAt;
    }

    public TripResponse setArrivalAt(LocalDateTime arrivalAt) {
        this.arrivalAt = arrivalAt;
        return this;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public TripResponse setPrice(BigDecimal price) {
        this.price = price;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public TripResponse setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public Integer getTotalSeats() {
        return totalSeats;
    }

    public TripResponse setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats;
        return this;
    }

    public Integer getAvailableSeats() {
        return availableSeats;
    }

    public TripResponse setAvailableSeats(Integer availableSeats) {
        this.availableSeats = availableSeats;
        return this;
    }

    public String getBusPlate() {
        return busPlate;
    }

    public TripResponse setBusPlate(String busPlate) {
        this.busPlate = busPlate;
        return this;
    }

    public String getVehicleDescription() {
        return vehicleDescription;
    }

    public TripResponse setVehicleDescription(String vehicleDescription) {
        this.vehicleDescription = vehicleDescription;
        return this;
    }

    public TripStatus getStatus() {
        return status;
    }

    public TripResponse setStatus(TripStatus status) {
        this.status = status;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public TripResponse setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public TripResponse setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}