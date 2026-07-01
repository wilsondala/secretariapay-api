package com.secretariapay.api.dto.report;

import com.secretariapay.api.entity.enums.BookingStatus;
import com.secretariapay.api.entity.enums.TicketStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class OperationalTicketReportItemResponse {

    private UUID ticketId;
    private String ticketCode;
    private TicketStatus ticketStatus;

    private UUID bookingId;
    private String bookingCode;
    private BookingStatus bookingStatus;

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

    private LocalDateTime issuedAt;
    private LocalDateTime usedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getTicketId() {
        return ticketId;
    }

    public OperationalTicketReportItemResponse setTicketId(UUID ticketId) {
        this.ticketId = ticketId;
        return this;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public OperationalTicketReportItemResponse setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
        return this;
    }

    public TicketStatus getTicketStatus() {
        return ticketStatus;
    }

    public OperationalTicketReportItemResponse setTicketStatus(TicketStatus ticketStatus) {
        this.ticketStatus = ticketStatus;
        return this;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public OperationalTicketReportItemResponse setBookingId(UUID bookingId) {
        this.bookingId = bookingId;
        return this;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public OperationalTicketReportItemResponse setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
        return this;
    }

    public BookingStatus getBookingStatus() {
        return bookingStatus;
    }

    public OperationalTicketReportItemResponse setBookingStatus(BookingStatus bookingStatus) {
        this.bookingStatus = bookingStatus;
        return this;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public OperationalTicketReportItemResponse setCompanyId(UUID companyId) {
        this.companyId = companyId;
        return this;
    }

    public String getCompanyName() {
        return companyName;
    }

    public OperationalTicketReportItemResponse setCompanyName(String companyName) {
        this.companyName = companyName;
        return this;
    }

    public String getCompanyTradeName() {
        return companyTradeName;
    }

    public OperationalTicketReportItemResponse setCompanyTradeName(String companyTradeName) {
        this.companyTradeName = companyTradeName;
        return this;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public OperationalTicketReportItemResponse setPassengerName(String passengerName) {
        this.passengerName = passengerName;
        return this;
    }

    public String getPassengerDocument() {
        return passengerDocument;
    }

    public OperationalTicketReportItemResponse setPassengerDocument(String passengerDocument) {
        this.passengerDocument = passengerDocument;
        return this;
    }

    public String getPassengerWhatsapp() {
        return passengerWhatsapp;
    }

    public OperationalTicketReportItemResponse setPassengerWhatsapp(String passengerWhatsapp) {
        this.passengerWhatsapp = passengerWhatsapp;
        return this;
    }

    public String getOriginCity() {
        return originCity;
    }

    public OperationalTicketReportItemResponse setOriginCity(String originCity) {
        this.originCity = originCity;
        return this;
    }

    public String getOriginState() {
        return originState;
    }

    public OperationalTicketReportItemResponse setOriginState(String originState) {
        this.originState = originState;
        return this;
    }

    public String getOriginTerminal() {
        return originTerminal;
    }

    public OperationalTicketReportItemResponse setOriginTerminal(String originTerminal) {
        this.originTerminal = originTerminal;
        return this;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public OperationalTicketReportItemResponse setDestinationCity(String destinationCity) {
        this.destinationCity = destinationCity;
        return this;
    }

    public String getDestinationState() {
        return destinationState;
    }

    public OperationalTicketReportItemResponse setDestinationState(String destinationState) {
        this.destinationState = destinationState;
        return this;
    }

    public String getDestinationTerminal() {
        return destinationTerminal;
    }

    public OperationalTicketReportItemResponse setDestinationTerminal(String destinationTerminal) {
        this.destinationTerminal = destinationTerminal;
        return this;
    }

    public LocalDateTime getDepartureAt() {
        return departureAt;
    }

    public OperationalTicketReportItemResponse setDepartureAt(LocalDateTime departureAt) {
        this.departureAt = departureAt;
        return this;
    }

    public LocalDateTime getArrivalAt() {
        return arrivalAt;
    }

    public OperationalTicketReportItemResponse setArrivalAt(LocalDateTime arrivalAt) {
        this.arrivalAt = arrivalAt;
        return this;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public OperationalTicketReportItemResponse setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
        return this;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public OperationalTicketReportItemResponse setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
        return this;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public OperationalTicketReportItemResponse setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
        return this;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public OperationalTicketReportItemResponse setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public OperationalTicketReportItemResponse setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OperationalTicketReportItemResponse setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}
