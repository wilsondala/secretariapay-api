package com.vairapido.api.dto.boarding;

import com.vairapido.api.entity.enums.BookingStatus;
import com.vairapido.api.entity.enums.PassengerDocumentType;
import com.vairapido.api.entity.enums.TicketStatus;

import java.time.LocalDateTime;

public class TicketBoardingResponse {

    private Boolean boarded;
    private Boolean canBoard;
    private String message;

    private String boardingStatusTitle;
    private String boardingStatusDescription;
    private String boardingStatusIcon;
    private String requiredAction;
    private String documentCheckMessage;

    private String ticketCode;
    private TicketStatus ticketStatus;

    private String bookingCode;
    private BookingStatus bookingStatus;

    private String passengerName;
    private PassengerDocumentType passengerDocumentType;
    private String passengerDocumentLabel;
    private String passengerDocument;
    private String passengerDocumentMasked;
    private String passengerWhatsapp;

    private String companyName;
    private String companyTradeName;
    private String companyDisplayName;

    private String originCity;
    private String originState;
    private String originTerminal;
    private String originLabel;

    private String destinationCity;
    private String destinationState;
    private String destinationTerminal;
    private String destinationLabel;

    private String routeLabel;

    private LocalDateTime departureAt;
    private LocalDateTime arrivalAt;

    private Integer seatNumber;
    private String seatLabel;

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

    public Boolean getCanBoard() {
        return canBoard;
    }

    public TicketBoardingResponse setCanBoard(Boolean canBoard) {
        this.canBoard = canBoard;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public TicketBoardingResponse setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getBoardingStatusTitle() {
        return boardingStatusTitle;
    }

    public TicketBoardingResponse setBoardingStatusTitle(String boardingStatusTitle) {
        this.boardingStatusTitle = boardingStatusTitle;
        return this;
    }

    public String getBoardingStatusDescription() {
        return boardingStatusDescription;
    }

    public TicketBoardingResponse setBoardingStatusDescription(String boardingStatusDescription) {
        this.boardingStatusDescription = boardingStatusDescription;
        return this;
    }

    public String getBoardingStatusIcon() {
        return boardingStatusIcon;
    }

    public TicketBoardingResponse setBoardingStatusIcon(String boardingStatusIcon) {
        this.boardingStatusIcon = boardingStatusIcon;
        return this;
    }

    public String getRequiredAction() {
        return requiredAction;
    }

    public TicketBoardingResponse setRequiredAction(String requiredAction) {
        this.requiredAction = requiredAction;
        return this;
    }

    public String getDocumentCheckMessage() {
        return documentCheckMessage;
    }

    public TicketBoardingResponse setDocumentCheckMessage(String documentCheckMessage) {
        this.documentCheckMessage = documentCheckMessage;
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

    public PassengerDocumentType getPassengerDocumentType() {
        return passengerDocumentType;
    }

    public TicketBoardingResponse setPassengerDocumentType(PassengerDocumentType passengerDocumentType) {
        this.passengerDocumentType = passengerDocumentType;
        return this;
    }

    public String getPassengerDocumentLabel() {
        return passengerDocumentLabel;
    }

    public TicketBoardingResponse setPassengerDocumentLabel(String passengerDocumentLabel) {
        this.passengerDocumentLabel = passengerDocumentLabel;
        return this;
    }

    public String getPassengerDocument() {
        return passengerDocument;
    }

    public TicketBoardingResponse setPassengerDocument(String passengerDocument) {
        this.passengerDocument = passengerDocument;
        return this;
    }

    public String getPassengerDocumentMasked() {
        return passengerDocumentMasked;
    }

    public TicketBoardingResponse setPassengerDocumentMasked(String passengerDocumentMasked) {
        this.passengerDocumentMasked = passengerDocumentMasked;
        return this;
    }

    public String getPassengerWhatsapp() {
        return passengerWhatsapp;
    }

    public TicketBoardingResponse setPassengerWhatsapp(String passengerWhatsapp) {
        this.passengerWhatsapp = passengerWhatsapp;
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

    public String getCompanyDisplayName() {
        return companyDisplayName;
    }

    public TicketBoardingResponse setCompanyDisplayName(String companyDisplayName) {
        this.companyDisplayName = companyDisplayName;
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

    public String getOriginLabel() {
        return originLabel;
    }

    public TicketBoardingResponse setOriginLabel(String originLabel) {
        this.originLabel = originLabel;
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

    public String getDestinationLabel() {
        return destinationLabel;
    }

    public TicketBoardingResponse setDestinationLabel(String destinationLabel) {
        this.destinationLabel = destinationLabel;
        return this;
    }

    public String getRouteLabel() {
        return routeLabel;
    }

    public TicketBoardingResponse setRouteLabel(String routeLabel) {
        this.routeLabel = routeLabel;
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

    public String getSeatLabel() {
        return seatLabel;
    }

    public TicketBoardingResponse setSeatLabel(String seatLabel) {
        this.seatLabel = seatLabel;
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