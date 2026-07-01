package com.secretariapay.api.service;

import com.secretariapay.api.dto.publicticket.TicketValidationResponse;
import com.secretariapay.api.entity.Booking;
import com.secretariapay.api.entity.Passenger;
import com.secretariapay.api.entity.Ticket;
import com.secretariapay.api.entity.TransportCompany;
import com.secretariapay.api.entity.TravelRoute;
import com.secretariapay.api.entity.Trip;
import com.secretariapay.api.entity.enums.BookingStatus;
import com.secretariapay.api.entity.enums.TicketAuditAction;
import com.secretariapay.api.entity.enums.TicketStatus;
import com.secretariapay.api.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PublicTicketValidationService {

    private final TicketRepository ticketRepository;
    private final TicketAuditLogService ticketAuditLogService;

    public PublicTicketValidationService(
            TicketRepository ticketRepository,
            TicketAuditLogService ticketAuditLogService
    ) {
        this.ticketRepository = ticketRepository;
        this.ticketAuditLogService = ticketAuditLogService;
    }

    @Transactional
    public TicketValidationResponse validateByCode(String ticketCode) {
        return validateByCode(ticketCode, null, null);
    }

    @Transactional
    public TicketValidationResponse validateByCode(
            String ticketCode,
            String ipAddress,
            String userAgent
    ) {
        Optional<Ticket> optionalTicket = ticketRepository.findByTicketCode(ticketCode);

        TicketValidationResponse response = optionalTicket
                .map(this::toValidationResponse)
                .orElseGet(() -> invalidTicket(ticketCode));

        Ticket ticket = optionalTicket.orElse(null);

        ticketAuditLogService.log(
                TicketAuditAction.PUBLIC_VALIDATION,
                ticket,
                ticketCode,
                Boolean.TRUE.equals(response.getValid()),
                response.getMessage(),
                response.getTicketStatus(),
                response.getBookingStatus(),
                ipAddress,
                userAgent
        );

        return response;
    }

    private TicketValidationResponse toValidationResponse(Ticket ticket) {
        Booking booking = ticket.getBooking();
        Trip trip = booking.getTrip();
        Passenger passenger = booking.getPassenger();
        TransportCompany company = trip.getTransportCompany();
        TravelRoute route = trip.getRoute();

        boolean valid = ticket.getStatus() == TicketStatus.VALID
                && booking.getStatus() == BookingStatus.TICKET_ISSUED;

        String message = resolveMessage(ticket, booking, valid);

        return new TicketValidationResponse()
                .setValid(valid)
                .setMessage(message)

                .setTicketCode(ticket.getTicketCode())
                .setTicketStatus(ticket.getStatus())

                .setBookingCode(booking.getBookingCode())
                .setBookingStatus(booking.getStatus())

                .setPassengerName(passenger.getFullName())
                .setPassengerDocument(passenger.getDocumentNumber())

                .setCompanyName(company.getName())
                .setCompanyTradeName(company.getTradeName())

                .setOriginCity(route.getOriginCity())
                .setOriginState(route.getOriginState())
                .setOriginTerminal(route.getOriginTerminal())

                .setDestinationCity(route.getDestinationCity())
                .setDestinationState(route.getDestinationState())
                .setDestinationTerminal(route.getDestinationTerminal())

                .setDepartureAt(trip.getDepartureAt())
                .setArrivalAt(trip.getArrivalAt())

                .setSeatNumber(booking.getSeatNumber())
                .setValidationUrl(ticket.getValidationUrl())
                .setIssuedAt(ticket.getIssuedAt())
                .setUsedAt(ticket.getUsedAt())
                .setCancelledAt(ticket.getCancelledAt())
                .setValidatedAt(LocalDateTime.now());
    }

    private TicketValidationResponse invalidTicket(String ticketCode) {
        return new TicketValidationResponse()
                .setValid(false)
                .setMessage("Bilhete não encontrado ou código inválido.")
                .setTicketCode(ticketCode)
                .setValidatedAt(LocalDateTime.now());
    }

    private String resolveMessage(Ticket ticket, Booking booking, boolean valid) {
        if (valid) {
            return "Bilhete válido para embarque.";
        }

        if (ticket.getStatus() == TicketStatus.USED) {
            return "Bilhete já utilizado no embarque.";
        }

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            return "Bilhete cancelado.";
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return "Reserva cancelada.";
        }

        if (booking.getStatus() == BookingStatus.EXPIRED) {
            return "Reserva expirada.";
        }

        return "Bilhete não está válido para embarque.";
    }
}
