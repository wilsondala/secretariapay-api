package com.secretariapay.api.dto.route;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class RouteRequest {

    @NotBlank(message = "A cidade de origem é obrigatória.")
    @Size(max = 120, message = "A cidade de origem deve ter no máximo 120 caracteres.")
    private String originCity;

    @Size(max = 80, message = "O estado/província de origem deve ter no máximo 80 caracteres.")
    private String originState;

    @Size(max = 160, message = "O terminal de origem deve ter no máximo 160 caracteres.")
    private String originTerminal;

    @NotBlank(message = "A cidade de destino é obrigatória.")
    @Size(max = 120, message = "A cidade de destino deve ter no máximo 120 caracteres.")
    private String destinationCity;

    @Size(max = 80, message = "O estado/província de destino deve ter no máximo 80 caracteres.")
    private String destinationState;

    @Size(max = 160, message = "O terminal de destino deve ter no máximo 160 caracteres.")
    private String destinationTerminal;

    @DecimalMin(value = "0.1", message = "A distância deve ser maior que zero.")
    private BigDecimal distanceKm;

    @Min(value = 1, message = "A duração estimada deve ser maior que zero.")
    private Integer estimatedDurationMinutes;

    public String getOriginCity() {
        return originCity;
    }

    public RouteRequest setOriginCity(String originCity) {
        this.originCity = originCity;
        return this;
    }

    public String getOriginState() {
        return originState;
    }

    public RouteRequest setOriginState(String originState) {
        this.originState = originState;
        return this;
    }

    public String getOriginTerminal() {
        return originTerminal;
    }

    public RouteRequest setOriginTerminal(String originTerminal) {
        this.originTerminal = originTerminal;
        return this;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public RouteRequest setDestinationCity(String destinationCity) {
        this.destinationCity = destinationCity;
        return this;
    }

    public String getDestinationState() {
        return destinationState;
    }

    public RouteRequest setDestinationState(String destinationState) {
        this.destinationState = destinationState;
        return this;
    }

    public String getDestinationTerminal() {
        return destinationTerminal;
    }

    public RouteRequest setDestinationTerminal(String destinationTerminal) {
        this.destinationTerminal = destinationTerminal;
        return this;
    }

    public BigDecimal getDistanceKm() {
        return distanceKm;
    }

    public RouteRequest setDistanceKm(BigDecimal distanceKm) {
        this.distanceKm = distanceKm;
        return this;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public RouteRequest setEstimatedDurationMinutes(Integer estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        return this;
    }
}
