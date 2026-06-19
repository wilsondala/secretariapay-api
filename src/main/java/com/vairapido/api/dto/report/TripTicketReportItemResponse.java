package com.vairapido.api.dto.report;

import com.vairapido.api.entity.enums.BookingStatus;
import com.vairapido.api.entity.enums.TicketStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TripTicketReportItemResponse {

    private UUID ticketId;
    private String ticketCode;
    private TicketStatus ticketStatus;

    private UUID bookingId;
    private String bookingCode;
    private BookingStatus bookingStatus;

    private String passengerName;
    private String passengerDocument;
    private String passengerWhatsapp;

    private Integer seatNumber;

    private BigDecimal amount;
    private String currency;

    private LocalDateTime issuedAt;
    private LocalDateTime usedAt;
    private LocalDateTime cancelledAt;

    public UUID getTicketId() {
        return ticketId;
    }

    public TripTicketReportItemResponse setTicketId(UUID ticketId) {
        this.ticketId = ticketId;
        return this;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public TripTicketReportItemResponse setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
        return this;
    }

    public TicketStatus getTicketStatus() {
        return ticketStatus;
    }

    public TripTicketReportItemResponse setTicketStatus(TicketStatus ticketStatus) {
        this.ticketStatus = ticketStatus;
        return this;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public TripTicketReportItemResponse setBookingId(UUID bookingId) {
        this.bookingId = bookingId;
        return this;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public TripTicketReportItemResponse setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
        return this;
    }

    public BookingStatus getBookingStatus() {
        return bookingStatus;
    }

    public TripTicketReportItemResponse setBookingStatus(BookingStatus bookingStatus) {
        this.bookingStatus = bookingStatus;
        return this;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public TripTicketReportItemResponse setPassengerName(String passengerName) {
        this.passengerName = passengerName;
        return this;
    }

    public String getPassengerDocument() {
        return passengerDocument;
    }

    public TripTicketReportItemResponse setPassengerDocument(String passengerDocument) {
        this.passengerDocument = passengerDocument;
        return this;
    }

    public String getPassengerWhatsapp() {
        return passengerWhatsapp;
    }

    public TripTicketReportItemResponse setPassengerWhatsapp(String passengerWhatsapp) {
        this.passengerWhatsapp = passengerWhatsapp;
        return this;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public TripTicketReportItemResponse setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
        return this;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TripTicketReportItemResponse setAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public TripTicketReportItemResponse setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public TripTicketReportItemResponse setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
        return this;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public TripTicketReportItemResponse setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
        return this;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public TripTicketReportItemResponse setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
        return this;
    }
}