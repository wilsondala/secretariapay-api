package com.vairapido.api.service;

import com.vairapido.api.dto.publicticket.TicketValidationResponse;
import com.vairapido.api.entity.Booking;
import com.vairapido.api.entity.Passenger;
import com.vairapido.api.entity.Ticket;
import com.vairapido.api.entity.TransportCompany;
import com.vairapido.api.entity.TravelRoute;
import com.vairapido.api.entity.Trip;
import com.vairapido.api.entity.enums.BookingStatus;
import com.vairapido.api.entity.enums.TicketStatus;
import com.vairapido.api.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PublicTicketValidationService {

    private final TicketRepository ticketRepository;

    public PublicTicketValidationService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Transactional(readOnly = true)
    public TicketValidationResponse validateByCode(String ticketCode) {
        return ticketRepository.findByTicketCode(ticketCode)
                .map(this::toValidationResponse)
                .orElseGet(() -> invalidTicket(ticketCode));
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