package com.vairapido.api.dto.boarding;

import com.vairapido.api.entity.enums.BookingStatus;
import com.vairapido.api.entity.enums.TicketStatus;

import java.time.LocalDateTime;

public class TicketBoardingResponse {

    private Boolean boarded;
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

    private LocalDateTime issuedAt;
    private LocalDateTime usedAt;
    private LocalDateTime boardedAt;

    public Boolean getBoarded() {
        return boarded;
    }

    public TicketBoardingResponse setBoarded(Boolean boarded) {
        this.boarded = boarded;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public TicketBoardingResponse setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public TicketBoardingResponse setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
        return this;
    }

    public TicketStatus getTicketStatus() {
        return ticketStatus;
    }

    public TicketBoardingResponse setTicketStatus(TicketStatus ticketStatus) {
        this.ticketStatus = ticketStatus;
        return this;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public TicketBoardingResponse setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
        return this;
    }

    public BookingStatus getBookingStatus() {
        return bookingStatus;
    }

    public TicketBoardingResponse setBookingStatus(BookingStatus bookingStatus) {
        this.bookingStatus = bookingStatus;
        return this;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public TicketBoardingResponse setPassengerName(String passengerName) {
        this.passengerName = passengerName;
        return this;
    }

    public String getPassengerDocument() {
        return passengerDocument;
    }

    public TicketBoardingResponse setPassengerDocument(String passengerDocument) {
        this.passengerDocument = passengerDocument;
        return this;
    }

    public String getCompanyName() {
        return companyName;
    }

    public TicketBoardingResponse setCompanyName(String companyName) {
        this.companyName = companyName;
        return this;
    }

    public String getCompanyTradeName() {
        return companyTradeName;
    }

    public TicketBoardingResponse setCompanyTradeName(String companyTradeName) {
        this.companyTradeName = companyTradeName;
        return this;
    }

    public String getOriginCity() {
        return originCity;
    }

    public TicketBoardingResponse setOriginCity(String originCity) {
        this.originCity = originCity;
        return this;
    }

    public String getOriginState() {
        return originState;
    }

    public TicketBoardingResponse setOriginState(String originState) {
        this.originState = originState;
        return this;
    }

    public String getOriginTerminal() {
        return originTerminal;
    }

    public TicketBoardingResponse setOriginTerminal(String originTerminal) {
        this.originTerminal = originTerminal;
        return this;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public TicketBoardingResponse setDestinationCity(String destinationCity) {
        this.destinationCity = destinationCity;
        return this;
    }

    public String getDestinationState() {
        return destinationState;
    }

    public TicketBoardingResponse setDestinationState(String destinationState) {
        this.destinationState = destinationState;
        return this;
    }

    public String getDestinationTerminal() {
        return destinationTerminal;
    }

    public TicketBoardingResponse setDestinationTerminal(String destinationTerminal) {
        this.destinationTerminal = destinationTerminal;
        return this;
    }

    public LocalDateTime getDepartureAt() {
        return departureAt;
    }

    public TicketBoardingResponse setDepartureAt(LocalDateTime departureAt) {
        this.departureAt = departureAt;
        return this;
    }

    public LocalDateTime getArrivalAt() {
        return arrivalAt;
    }

    public TicketBoardingResponse setArrivalAt(LocalDateTime arrivalAt) {
        this.arrivalAt = arrivalAt;
        return this;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public TicketBoardingResponse setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
        return this;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public TicketBoardingResponse setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
        return this;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public TicketBoardingResponse setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
        return this;
    }

    public LocalDateTime getBoardedAt() {
        return boardedAt;
    }

    public TicketBoardingResponse setBoardedAt(LocalDateTime boardedAt) {
        this.boardedAt = boardedAt;
        return this;
    }
}