package com.vairapido.api.entity;

import com.vairapido.api.entity.enums.BookingStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;

    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    @Column(name = "booking_code", nullable = false, unique = true, length = 40)
    private String bookingCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BookingStatus status = BookingStatus.PENDING_PAYMENT;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency = "BRL";

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

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
            this.status = BookingStatus.PENDING_PAYMENT;
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

    public Trip getTrip() {
        return trip;
    }

    public Booking setTrip(Trip trip) {
        this.trip = trip;
        return this;
    }

    public Passenger getPassenger() {
        return passenger;
    }

    public Booking setPassenger(Passenger passenger) {
        this.passenger = passenger;
        return this;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public Booking setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
        return this;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public Booking setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
        return this;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public Booking setStatus(BookingStatus status) {
        this.status = status;
        return this;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Booking setAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public Booking setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public Booking setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public Booking setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
        return this;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public Booking setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}