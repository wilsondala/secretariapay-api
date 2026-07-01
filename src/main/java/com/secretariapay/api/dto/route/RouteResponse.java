package com.secretariapay.api.dto.route;

import com.secretariapay.api.entity.enums.RouteStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class RouteResponse {

    private UUID id;
    private String originCity;
    private String originState;
    private String originTerminal;
    private String destinationCity;
    private String destinationState;
    private String destinationTerminal;
    private BigDecimal distanceKm;
    private Integer estimatedDurationMinutes;
    private RouteStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public RouteResponse setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getOriginCity() {
        return originCity;
    }

    public RouteResponse setOriginCity(String originCity) {
        this.originCity = originCity;
        return this;
    }

    public String getOriginState() {
        return originState;
    }

    public RouteResponse setOriginState(String originState) {
        this.originState = originState;
        return this;
    }

    public String getOriginTerminal() {
        return originTerminal;
    }

    public RouteResponse setOriginTerminal(String originTerminal) {
        this.originTerminal = originTerminal;
        return this;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public RouteResponse setDestinationCity(String destinationCity) {
        this.destinationCity = destinationCity;
        return this;
    }

    public String getDestinationState() {
        return destinationState;
    }

    public RouteResponse setDestinationState(String destinationState) {
        this.destinationState = destinationState;
        return this;
    }

    public String getDestinationTerminal() {
        return destinationTerminal;
    }

    public RouteResponse setDestinationTerminal(String destinationTerminal) {
        this.destinationTerminal = destinationTerminal;
        return this;
    }

    public BigDecimal getDistanceKm() {
        return distanceKm;
    }

    public RouteResponse setDistanceKm(BigDecimal distanceKm) {
        this.distanceKm = distanceKm;
        return this;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public RouteResponse setEstimatedDurationMinutes(Integer estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        return this;
    }

    public RouteStatus getStatus() {
        return status;
    }

    public RouteResponse setStatus(RouteStatus status) {
        this.status = status;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public RouteResponse setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public RouteResponse setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}
