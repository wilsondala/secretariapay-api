package com.vairapido.api.service;

import com.vairapido.api.dto.boarding.TicketBoardingResponse;
import com.vairapido.api.entity.Booking;
import com.vairapido.api.entity.Passenger;
import com.vairapido.api.entity.Ticket;
import com.vairapido.api.entity.TransportCompany;
import com.vairapido.api.entity.TravelRoute;
import com.vairapido.api.entity.Trip;
import com.vairapido.api.entity.enums.BookingStatus;
import com.vairapido.api.entity.enums.TicketAuditAction;
import com.vairapido.api.entity.enums.TicketStatus;
import com.vairapido.api.exception.NotFoundException;
import com.vairapido.api.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TicketBoardingService {

    private final TicketRepository ticketRepository;
    private final TicketAuditLogService ticketAuditLogService;

    public TicketBoardingService(
            TicketRepository ticketRepository,
            TicketAuditLogService ticketAuditLogService
    ) {
        this.ticketRepository = ticketRepository;
        this.ticketAuditLogService = ticketAuditLogService;
    }

    @Transactional
    public TicketBoardingResponse boardByTicketCode(String ticketCode) {
        return boardByTicketCode(ticketCode, null, null);
    }

    @Transactional
    public TicketBoardingResponse boardByTicketCode(
            String ticketCode,
            String ipAddress,
            String userAgent
    ) {
        Ticket ticket = null;

        try {
            ticket = ticketRepository.findByTicketCode(ticketCode)
                    .orElseThrow(() -> new NotFoundException("Bilhete não encontrado."));

            validateTicketForBoarding(ticket);

            LocalDateTime now = LocalDateTime.now();

            ticket
                    .setStatus(TicketStatus.USED)
                    .setUsedAt(now);

            Ticket savedTicket = ticketRepository.save(ticket);

            TicketBoardingResponse response = toBoardingResponse(
                    savedTicket,
                    true,
                    "Embarque confirmado com sucesso.",
                    now
            );

            ticketAuditLogService.log(
                    TicketAuditAction.BOARDING,
                    savedTicket,
                    savedTicket.getTicketCode(),
                    true,
                    response.getMessage(),
                    savedTicket.getStatus(),
                    savedTicket.getBooking().getStatus(),
                    ipAddress,
                    userAgent
            );

            return response;
        } catch (RuntimeException exception) {
            BookingStatus bookingStatus = null;
            TicketStatus ticketStatus = null;

            if (ticket != null) {
                ticketStatus = ticket.getStatus();

                if (ticket.getBooking() != null) {
                    bookingStatus = ticket.getBooking().getStatus();
                }
            }

            ticketAuditLogService.log(
                    TicketAuditAction.BOARDING,
                    ticket,
                    ticketCode,
                    false,
                    exception.getMessage(),
                    ticketStatus,
                    bookingStatus,
                    ipAddress,
                    userAgent
            );

            throw exception;
        }
    }

    private void validateTicketForBoarding(Ticket ticket) {
        Booking booking = ticket.getBooking();

        if (ticket.getStatus() == TicketStatus.USED) {
            throw new IllegalArgumentException("Bilhete já utilizado no embarque.");
        }

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new IllegalArgumentException("Bilhete cancelado. Embarque não permitido.");
        }

        if (ticket.getStatus() != TicketStatus.VALID) {
            throw new IllegalArgumentException("Bilhete não está válido para embarque.");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Reserva cancelada. Embarque não permitido.");
        }

        if (booking.getStatus() == BookingStatus.EXPIRED) {
            throw new IllegalArgumentException("Reserva expirada. Embarque não permitido.");
        }

        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            throw new IllegalArgumentException("Reserva ainda está pendente de pagamento.");
        }

        if (booking.getStatus() == BookingStatus.PAID) {
            throw new IllegalArgumentException("Reserva paga, mas o bilhete ainda não foi emitido.");
        }

        if (booking.getStatus() != BookingStatus.TICKET_ISSUED) {
            throw new IllegalArgumentException("Reserva não está liberada para embarque.");
        }

        Trip trip = booking.getTrip();

        if (trip.getArrivalAt() != null && LocalDateTime.now().isAfter(trip.getArrivalAt())) {
            throw new IllegalArgumentException("Viagem já encerrada. Embarque não permitido.");
        }
    }

    private TicketBoardingResponse toBoardingResponse(
            Ticket ticket,
            boolean boarded,
            String message,
            LocalDateTime boardedAt
    ) {
        Booking booking = ticket.getBooking();
        Trip trip = booking.getTrip();
        Passenger passenger = booking.getPassenger();
        TransportCompany company = trip.getTransportCompany();
        TravelRoute route = trip.getRoute();

        return new TicketBoardingResponse()
                .setBoarded(boarded)
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

                .setIssuedAt(ticket.getIssuedAt())
                .setUsedAt(ticket.getUsedAt())
                .setBoardedAt(boardedAt);
    }
}