package com.secretariapay.api.dto.report;

import com.secretariapay.api.entity.enums.BookingStatus;
import com.secretariapay.api.entity.enums.TicketStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class FinancialBookingReportItemResponse {

    private UUID bookingId;
    private String bookingCode;
    private BookingStatus bookingStatus;

    private UUID ticketId;
    private String ticketCode;
    private TicketStatus ticketStatus;

    private UUID companyId;
    private String companyName;
    private String companyTradeName;

    private String passengerName;
    private String passengerDocument;
    private String passengerWhatsapp;

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

    private LocalDateTime expiresAt;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getBookingId() {
        return bookingId;
    }

    public FinancialBookingReportItemResponse setBookingId(UUID bookingId) {
        this.bookingId = bookingId;
        return this;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public FinancialBookingReportItemResponse setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
        return this;
    }

    public BookingStatus getBookingStatus() {
        return bookingStatus;
    }

    public FinancialBookingReportItemResponse setBookingStatus(BookingStatus bookingStatus) {
        this.bookingStatus = bookingStatus;
        return this;
    }

    public UUID getTicketId() {
        return ticketId;
    }

    public FinancialBookingReportItemResponse setTicketId(UUID ticketId) {
        this.ticketId = ticketId;
        return this;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public FinancialBookingReportItemResponse setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
        return this;
    }

    public TicketStatus getTicketStatus() {
        return ticketStatus;
    }

    public FinancialBookingReportItemResponse setTicketStatus(TicketStatus ticketStatus) {
        this.ticketStatus = ticketStatus;
        return this;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public FinancialBookingReportItemResponse setCompanyId(UUID companyId) {
        this.companyId = companyId;
        return this;
    }

    public String getCompanyName() {
        return companyName;
    }

    public FinancialBookingReportItemResponse setCompanyName(String companyName) {
        this.companyName = companyName;
        return this;
    }

    public String getCompanyTradeName() {
        return companyTradeName;
    }

    public FinancialBookingReportItemResponse setCompanyTradeName(String companyTradeName) {
        this.companyTradeName = companyTradeName;
        return this;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public FinancialBookingReportItemResponse setPassengerName(String passengerName) {
        this.passengerName = passengerName;
        return this;
    }

    public String getPassengerDocument() {
        return passengerDocument;
    }

    public FinancialBookingReportItemResponse setPassengerDocument(String passengerDocument) {
        this.passengerDocument = passengerDocument;
        return this;
    }

    public String getPassengerWhatsapp() {
        return passengerWhatsapp;
    }

    public FinancialBookingReportItemResponse setPassengerWhatsapp(String passengerWhatsapp) {
        this.passengerWhatsapp = passengerWhatsapp;
        return this;
    }

    public String getOriginCity() {
        return originCity;
    }

    public FinancialBookingReportItemResponse setOriginCity(String originCity) {
        this.originCity = originCity;
        return this;
    }

    public String getOriginState() {
        return originState;
    }

    public FinancialBookingReportItemResponse setOriginState(String originState) {
        this.originState = originState;
        return this;
    }

    public String getOriginTerminal() {
        return originTerminal;
    }

    public FinancialBookingReportItemResponse setOriginTerminal(String originTerminal) {
        this.originTerminal = originTerminal;
        return this;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public FinancialBookingReportItemResponse setDestinationCity(String destinationCity) {
        this.destinationCity = destinationCity;
        return this;
    }

    public String getDestinationState() {
        return destinationState;
    }

    public FinancialBookingReportItemResponse setDestinationState(String destinationState) {
        this.destinationState = destinationState;
        return this;
    }

    public String getDestinationTerminal() {
        return destinationTerminal;
    }

    public FinancialBookingReportItemResponse setDestinationTerminal(String destinationTerminal) {
        this.destinationTerminal = destinationTerminal;
        return this;
    }

    public LocalDateTime getDepartureAt() {
        return departureAt;
    }

    public FinancialBookingReportItemResponse setDepartureAt(LocalDateTime departureAt) {
        this.departureAt = departureAt;
        return this;
    }

    public LocalDateTime getArrivalAt() {
        return arrivalAt;
    }

    public FinancialBookingReportItemResponse setArrivalAt(LocalDateTime arrivalAt) {
        this.arrivalAt = arrivalAt;
        return this;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public FinancialBookingReportItemResponse setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
        return this;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public FinancialBookingReportItemResponse setAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public FinancialBookingReportItemResponse setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public FinancialBookingReportItemResponse setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public FinancialBookingReportItemResponse setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
        return this;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public FinancialBookingReportItemResponse setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public FinancialBookingReportItemResponse setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public FinancialBookingReportItemResponse setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}
