package com.vairapido.api.dto.ticket;

import com.vairapido.api.entity.enums.BookingStatus;
import com.vairapido.api.entity.enums.TicketStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TicketResponse {

    private UUID id;
    private String ticketCode;
    private String qrCodeUrl;
    private String validationUrl;
    private TicketStatus status;

    private UUID bookingId;
    private String bookingCode;
    private BookingStatus bookingStatus;

    private String passengerName;
    private String passengerDocument;
    private String passengerWhatsapp;

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
    private Integer seatNumber;
    private BigDecimal amount;
    private String currency;

    private LocalDateTime issuedAt;
    private LocalDateTime usedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public TicketResponse setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public TicketResponse setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
        return this;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public TicketResponse setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
        return this;
    }

    public String getValidationUrl() {
        return validationUrl;
    }

    public TicketResponse setValidationUrl(String validationUrl) {
        this.validationUrl = validationUrl;
        return this;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public TicketResponse setStatus(TicketStatus status) {
        this.status = status;
        return this;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public TicketResponse setBookingId(UUID bookingId) {
        this.bookingId = bookingId;
        return this;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public TicketResponse setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
        return this;
    }

    public BookingStatus getBookingStatus() {
        return bookingStatus;
    }

    public TicketResponse setBookingStatus(BookingStatus bookingStatus) {
        this.bookingStatus = bookingStatus;
        return this;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public TicketResponse setPassengerName(String passengerName) {
        this.passengerName = passengerName;
        return this;
    }

    public String getPassengerDocument() {
        return passengerDocument;
    }

    public TicketResponse setPassengerDocument(String passengerDocument) {
        this.passengerDocument = passengerDocument;
        return this;
    }

    public String getPassengerWhatsapp() {
        return passengerWhatsapp;
    }

    public TicketResponse setPassengerWhatsapp(String passengerWhatsapp) {
        this.passengerWhatsapp = passengerWhatsapp;
        return this;
    }

    public String getCompanyName() {
        return companyName;
    }

    public TicketResponse setCompanyName(String companyName) {
        this.companyName = companyName;
        return this;
    }

    public String getCompanyTradeName() {
        return companyTradeName;
    }

    public TicketResponse setCompanyTradeName(String companyTradeName) {
        this.companyTradeName = companyTradeName;
        return this;
    }

    public String getOriginCity() {
        return originCity;
    }

    public TicketResponse setOriginCity(String originCity) {
        this.originCity = originCity;
        return this;
    }

    public String getOriginState() {
        return originState;
    }

    public TicketResponse setOriginState(String originState) {
        this.originState = originState;
        return this;
    }

    public String getOriginTerminal() {
        return originTerminal;
    }

    public TicketResponse setOriginTerminal(String originTerminal) {
        this.originTerminal = originTerminal;
        return this;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public TicketResponse setDestinationCity(String destinationCity) {
        this.destinationCity = destinationCity;
        return this;
    }

    public String getDestinationState() {
        return destinationState;
    }

    public TicketResponse setDestinationState(String destinationState) {
        this.destinationState = destinationState;
        return this;
    }

    public String getDestinationTerminal() {
        return destinationTerminal;
    }

    public TicketResponse setDestinationTerminal(String destinationTerminal) {
        this.destinationTerminal = destinationTerminal;
        return this;
    }

    public LocalDateTime getDepartureAt() {
        return departureAt;
    }

    public TicketResponse setDepartureAt(LocalDateTime departureAt) {
        this.departureAt = departureAt;
        return this;
    }

    public LocalDateTime getArrivalAt() {
        return arrivalAt;
    }

    public TicketResponse setArrivalAt(LocalDateTime arrivalAt) {
        this.arrivalAt = arrivalAt;
        return this;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public TicketResponse setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
        return this;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TicketResponse setAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public TicketResponse setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public TicketResponse setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
        return this;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public TicketResponse setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
        return this;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public TicketResponse setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public TicketResponse setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public TicketResponse setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}