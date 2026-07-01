package com.secretariapay.api.dto.payment;

import com.secretariapay.api.entity.enums.BookingStatus;
import com.secretariapay.api.entity.enums.PaymentMethod;
import com.secretariapay.api.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class PaymentResponse {

    private UUID id;
    private String paymentCode;

    private UUID bookingId;
    private String bookingCode;
    private BookingStatus bookingStatus;

    private String passengerName;
    private String passengerDocument;
    private String passengerWhatsapp;

    private String companyTradeName;
    private String originCity;
    private String destinationCity;
    private LocalDateTime departureAt;
    private Integer seatNumber;

    private PaymentMethod method;
    private PaymentStatus status;
    private BigDecimal amount;
    private String currency;
    private String pixCopyPaste;
    private String pixQrCodeUrl;
    private String gatewayName;
    private String gatewayTransactionId;
    private LocalDateTime expiresAt;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public PaymentResponse setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getPaymentCode() {
        return paymentCode;
    }

    public PaymentResponse setPaymentCode(String paymentCode) {
        this.paymentCode = paymentCode;
        return this;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public PaymentResponse setBookingId(UUID bookingId) {
        this.bookingId = bookingId;
        return this;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public PaymentResponse setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
        return this;
    }

    public BookingStatus getBookingStatus() {
        return bookingStatus;
    }

    public PaymentResponse setBookingStatus(BookingStatus bookingStatus) {
        this.bookingStatus = bookingStatus;
        return this;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public PaymentResponse setPassengerName(String passengerName) {
        this.passengerName = passengerName;
        return this;
    }

    public String getPassengerDocument() {
        return passengerDocument;
    }

    public PaymentResponse setPassengerDocument(String passengerDocument) {
        this.passengerDocument = passengerDocument;
        return this;
    }

    public String getPassengerWhatsapp() {
        return passengerWhatsapp;
    }

    public PaymentResponse setPassengerWhatsapp(String passengerWhatsapp) {
        this.passengerWhatsapp = passengerWhatsapp;
        return this;
    }

    public String getCompanyTradeName() {
        return companyTradeName;
    }

    public PaymentResponse setCompanyTradeName(String companyTradeName) {
        this.companyTradeName = companyTradeName;
        return this;
    }

    public String getOriginCity() {
        return originCity;
    }

    public PaymentResponse setOriginCity(String originCity) {
        this.originCity = originCity;
        return this;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public PaymentResponse setDestinationCity(String destinationCity) {
        this.destinationCity = destinationCity;
        return this;
    }

    public LocalDateTime getDepartureAt() {
        return departureAt;
    }

    public PaymentResponse setDepartureAt(LocalDateTime departureAt) {
        this.departureAt = departureAt;
        return this;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public PaymentResponse setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
        return this;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public PaymentResponse setMethod(PaymentMethod method) {
        this.method = method;
        return this;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public PaymentResponse setStatus(PaymentStatus status) {
        this.status = status;
        return this;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentResponse setAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentResponse setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public String getPixCopyPaste() {
        return pixCopyPaste;
    }

    public PaymentResponse setPixCopyPaste(String pixCopyPaste) {
        this.pixCopyPaste = pixCopyPaste;
        return this;
    }

    public String getPixQrCodeUrl() {
        return pixQrCodeUrl;
    }

    public PaymentResponse setPixQrCodeUrl(String pixQrCodeUrl) {
        this.pixQrCodeUrl = pixQrCodeUrl;
        return this;
    }

    public String getGatewayName() {
        return gatewayName;
    }

    public PaymentResponse setGatewayName(String gatewayName) {
        this.gatewayName = gatewayName;
        return this;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public PaymentResponse setGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
        return this;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public PaymentResponse setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public PaymentResponse setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
        return this;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public PaymentResponse setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public PaymentResponse setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public PaymentResponse setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}
