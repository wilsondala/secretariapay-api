package com.secretariapay.api.dto.publicticket;

import com.secretariapay.api.entity.enums.BookingStatus;
import com.secretariapay.api.entity.enums.TicketStatus;

import java.time.LocalDateTime;

public class TicketValidationResponse {

    private Boolean valid;
    private String message;

    private String ticketCode;
    private TicketStatus ticketStatus;

    private String bookingCode;
    private BookingStatus bookingStatus;

    private String passengerName;
    private String passengerDocument;

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
    private String validationUrl;

    private LocalDateTime issuedAt;
    private LocalDateTime usedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime validatedAt;

    public Boolean getValid() {
        return valid;
    }

    public TicketValidationResponse setValid(Boolean valid) {
        this.valid = valid;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public TicketValidationResponse setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public TicketValidationResponse setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
        return this;
    }

    public TicketStatus getTicketStatus() {
        return ticketStatus;
    }

    public TicketValidationResponse setTicketStatus(TicketStatus ticketStatus) {
        this.ticketStatus = ticketStatus;
        return this;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public TicketValidationResponse setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
        return this;
    }

    public BookingStatus getBookingStatus() {
        return bookingStatus;
    }

    public TicketValidationResponse setBookingStatus(BookingStatus bookingStatus) {
        this.bookingStatus = bookingStatus;
        return this;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public TicketValidationResponse setPassengerName(String passengerName) {
        this.passengerName = passengerName;
        return this;
    }

    public String getPassengerDocument() {
        return passengerDocument;
    }

    public TicketValidationResponse setPassengerDocument(String passengerDocument) {
        this.passengerDocument = passengerDocument;
        return this;
    }

    public String getCompanyName() {
        return companyName;
    }

    public TicketValidationResponse setCompanyName(String companyName) {
        this.companyName = companyName;
        return this;
    }

    public String getCompanyTradeName() {
        return companyTradeName;
    }

    public TicketValidationResponse setCompanyTradeName(String companyTradeName) {
        this.companyTradeName = companyTradeName;
        return this;
    }

    public String getOriginCity() {
        return originCity;
    }

    public TicketValidationResponse setOriginCity(String originCity) {
        this.originCity = originCity;
        return this;
    }

    public String getOriginState() {
        return originState;
    }

    public TicketValidationResponse setOriginState(String originState) {
        this.originState = originState;
        return this;
    }

    public String getOriginTerminal() {
        return originTerminal;
    }

    public TicketValidationResponse setOriginTerminal(String originTerminal) {
        this.originTerminal = originTerminal;
        return this;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public TicketValidationResponse setDestinationCity(String destinationCity) {
        this.destinationCity = destinationCity;
        return this;
    }

    public String getDestinationState() {
        return destinationState;
    }

    public TicketValidationResponse setDestinationState(String destinationState) {
        this.destinationState = destinationState;
        return this;
    }

    public String getDestinationTerminal() {
        return destinationTerminal;
    }

    public TicketValidationResponse setDestinationTerminal(String destinationTerminal) {
        this.destinationTerminal = destinationTerminal;
        return this;
    }

    public LocalDateTime getDepartureAt() {
        return departureAt;
    }

    public TicketValidationResponse setDepartureAt(LocalDateTime departureAt) {
        this.departureAt = departureAt;
        return this;
    }

    public LocalDateTime getArrivalAt() {
        return arrivalAt;
    }

    public TicketValidationResponse setArrivalAt(LocalDateTime arrivalAt) {
        this.arrivalAt = arrivalAt;
        return this;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public TicketValidationResponse setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
        return this;
    }

    public String getValidationUrl() {
        return validationUrl;
    }

    public TicketValidationResponse setValidationUrl(String validationUrl) {
        this.validationUrl = validationUrl;
        return this;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public TicketValidationResponse setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
        return this;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public TicketValidationResponse setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
        return this;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public TicketValidationResponse setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
        return this;
    }

    public LocalDateTime getValidatedAt() {
        return validatedAt;
    }

    public TicketValidationResponse setValidatedAt(LocalDateTime validatedAt) {
        this.validatedAt = validatedAt;
        return this;
    }
}
