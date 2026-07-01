package com.secretariapay.api.entity;

import com.secretariapay.api.entity.enums.TripStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "trips")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transport_company_id", nullable = false)
    private TransportCompany transportCompany;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private TravelRoute route;

    @Column(name = "departure_at", nullable = false)
    private LocalDateTime departureAt;

    @Column(name = "arrival_at", nullable = false)
    private LocalDateTime arrivalAt;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 10)
    private String currency = "BRL";

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @Column(name = "available_seats", nullable = false)
    private Integer availableSeats;

    @Column(name = "bus_plate", length = 30)
    private String busPlate;

    @Column(name = "vehicle_description", length = 160)
    private String vehicleDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TripStatus status = TripStatus.SCHEDULED;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = TripStatus.SCHEDULED;
        }

        if (this.currency == null || this.currency.isBlank()) {
            this.currency = "BRL";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public TransportCompany getTransportCompany() {
        return transportCompany;
    }

    public Trip setTransportCompany(TransportCompany transportCompany) {
        this.transportCompany = transportCompany;
        return this;
    }

    public TravelRoute getRoute() {
        return route;
    }

    public Trip setRoute(TravelRoute route) {
        this.route = route;
        return this;
    }

    public LocalDateTime getDepartureAt() {
        return departureAt;
    }

    public Trip setDepartureAt(LocalDateTime departureAt) {
        this.departureAt = departureAt;
        return this;
    }

    public LocalDateTime getArrivalAt() {
        return arrivalAt;
    }

    public Trip setArrivalAt(LocalDateTime arrivalAt) {
        this.arrivalAt = arrivalAt;
        return this;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Trip setPrice(BigDecimal price) {
        this.price = price;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public Trip setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public Integer getTotalSeats() {
        return totalSeats;
    }

    public Trip setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats;
        return this;
    }

    public Integer getAvailableSeats() {
        return availableSeats;
    }

    public Trip setAvailableSeats(Integer availableSeats) {
        this.availableSeats = availableSeats;
        return this;
    }

    public String getBusPlate() {
        return busPlate;
    }

    public Trip setBusPlate(String busPlate) {
        this.busPlate = busPlate;
        return this;
    }

    public String getVehicleDescription() {
        return vehicleDescription;
    }

    public Trip setVehicleDescription(String vehicleDescription) {
        this.vehicleDescription = vehicleDescription;
        return this;
    }

    public TripStatus getStatus() {
        return status;
    }

    public Trip setStatus(TripStatus status) {
        this.status = status;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void decreaseAvailableSeats() {
        if (this.availableSeats == null || this.availableSeats <= 0) {
            throw new IllegalArgumentException("Não há assentos disponíveis para esta viagem.");
        }

        this.availableSeats = this.availableSeats - 1;
    }

    public void increaseAvailableSeats() {
        if (this.availableSeats == null) {
            this.availableSeats = 0;
        }

        if (this.totalSeats != null && this.availableSeats < this.totalSeats) {
            this.availableSeats = this.availableSeats + 1;
        }
    }
}
