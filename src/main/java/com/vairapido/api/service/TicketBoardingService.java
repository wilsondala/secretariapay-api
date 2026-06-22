package com.vairapido.api.service;

import com.vairapido.api.dto.boarding.TicketBoardingResponse;
import com.vairapido.api.entity.Booking;
import com.vairapido.api.entity.Passenger;
import com.vairapido.api.entity.Ticket;
import com.vairapido.api.entity.TransportCompany;
import com.vairapido.api.entity.TravelRoute;
import com.vairapido.api.entity.Trip;
import com.vairapido.api.entity.enums.BookingStatus;
import com.vairapido.api.entity.enums.PassengerDocumentType;
import com.vairapido.api.entity.enums.TicketAuditAction;
import com.vairapido.api.entity.enums.TicketStatus;
import com.vairapido.api.exception.NotFoundException;
import com.vairapido.api.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TicketBoardingService {

    private static final String DOCUMENT_CHECK_MESSAGE = """
            Confira o documento oficial do passageiro antes de liberar o embarque.
            O nome e o número do documento precisam ser os mesmos do bilhete.
            """.trim();

    private final TicketRepository ticketRepository;
    private final TicketAuditLogService ticketAuditLogService;
    private final DocumentValidatorService documentValidatorService;

    public TicketBoardingService(
            TicketRepository ticketRepository,
            TicketAuditLogService ticketAuditLogService,
            DocumentValidatorService documentValidatorService
    ) {
        this.ticketRepository = ticketRepository;
        this.ticketAuditLogService = ticketAuditLogService;
        this.documentValidatorService = documentValidatorService;
    }

    @Transactional(readOnly = true)
    public TicketBoardingResponse previewByTicketCode(String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new NotFoundException("Bilhete não encontrado."));

        try {
            validateTicketForBoarding(ticket);

            return toBoardingResponse(
                    ticket,
                    false,
                    true,
                    "Bilhete liberado para embarque.",
                    null
            );
        } catch (RuntimeException exception) {
            return toBoardingResponse(
                    ticket,
                    false,
                    false,
                    exception.getMessage(),
                    null
            );
        }
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
                    false,
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
            boolean canBoard,
            String message,
            LocalDateTime boardedAt
    ) {
        Booking booking = ticket.getBooking();
        Trip trip = booking.getTrip();
        Passenger passenger = booking.getPassenger();
        TransportCompany company = trip.getTransportCompany();
        TravelRoute route = trip.getRoute();

        PassengerDocumentType documentType = resolvePassengerDocumentType(passenger);
        String documentNumber = documentValidatorService.normalize(documentType, passenger.getDocumentNumber());
        String documentMasked = documentValidatorService.mask(documentType, passenger.getDocumentNumber());
        String documentLabel = documentValidatorService.label(documentType);

        String companyDisplayName = resolveCompanyDisplayName(company);
        String originLabel = formatLocationLabel(
                route.getOriginCity(),
                route.getOriginState(),
                route.getOriginTerminal()
        );
        String destinationLabel = formatLocationLabel(
                route.getDestinationCity(),
                route.getDestinationState(),
                route.getDestinationTerminal()
        );

        String routeLabel = originLabel + " → " + destinationLabel;
        String seatLabel = booking.getSeatNumber() != null
                ? "Poltrona " + booking.getSeatNumber()
                : "Poltrona não informada";

        TicketBoardingResponse response = new TicketBoardingResponse()
                .setBoarded(boarded)
                .setCanBoard(canBoard)
                .setMessage(message)
                .setDocumentCheckMessage(DOCUMENT_CHECK_MESSAGE)

                .setTicketCode(ticket.getTicketCode())
                .setTicketStatus(ticket.getStatus())

                .setBookingCode(booking.getBookingCode())
                .setBookingStatus(booking.getStatus())

                .setPassengerName(passenger.getFullName())
                .setPassengerDocumentType(documentType)
                .setPassengerDocumentLabel(documentLabel)
                .setPassengerDocument(documentNumber)
                .setPassengerDocumentMasked(documentMasked)
                .setPassengerWhatsapp(passenger.getWhatsapp())

                .setCompanyName(company.getName())
                .setCompanyTradeName(company.getTradeName())
                .setCompanyDisplayName(companyDisplayName)

                .setOriginCity(route.getOriginCity())
                .setOriginState(route.getOriginState())
                .setOriginTerminal(route.getOriginTerminal())
                .setOriginLabel(originLabel)

                .setDestinationCity(route.getDestinationCity())
                .setDestinationState(route.getDestinationState())
                .setDestinationTerminal(route.getDestinationTerminal())
                .setDestinationLabel(destinationLabel)

                .setRouteLabel(routeLabel)

                .setDepartureAt(trip.getDepartureAt())
                .setArrivalAt(trip.getArrivalAt())

                .setSeatNumber(booking.getSeatNumber())
                .setSeatLabel(seatLabel)

                .setIssuedAt(ticket.getIssuedAt())
                .setUsedAt(ticket.getUsedAt())
                .setBoardedAt(boardedAt);

        applyBoardingStatus(response, ticket, boarded, canBoard, message);

        return response;
    }

    private void applyBoardingStatus(
            TicketBoardingResponse response,
            Ticket ticket,
            boolean boarded,
            boolean canBoard,
            String message
    ) {
        if (boarded) {
            response
                    .setBoardingStatusIcon("✅")
                    .setBoardingStatusTitle("Embarque confirmado")
                    .setBoardingStatusDescription("O passageiro foi marcado como embarcado.")
                    .setRequiredAction("Nenhuma ação pendente. Este bilhete já foi utilizado.");

            return;
        }

        if (canBoard) {
            response
                    .setBoardingStatusIcon("🟢")
                    .setBoardingStatusTitle("Liberado para embarque")
                    .setBoardingStatusDescription("Bilhete válido. Confira o documento oficial antes de confirmar o embarque.")
                    .setRequiredAction("Conferir documento oficial e clicar em Marcar embarque.");

            return;
        }

        if (ticket.getStatus() == TicketStatus.USED) {
            response
                    .setBoardingStatusIcon("⚠️")
                    .setBoardingStatusTitle("Bilhete já utilizado")
                    .setBoardingStatusDescription(message)
                    .setRequiredAction("Não permitir novo embarque com este bilhete.");

            return;
        }

        response
                .setBoardingStatusIcon("🔴")
                .setBoardingStatusTitle("Embarque bloqueado")
                .setBoardingStatusDescription(message)
                .setRequiredAction("Não permitir embarque. Oriente o passageiro a procurar atendimento.");
    }

    private PassengerDocumentType resolvePassengerDocumentType(Passenger passenger) {
        if (passenger != null && passenger.getDocumentType() != null) {
            return passenger.getDocumentType();
        }

        return documentValidatorService.defaultDocumentType();
    }

    private String resolveCompanyDisplayName(TransportCompany company) {
        if (company == null) {
            return "-";
        }

        if (company.getTradeName() != null && !company.getTradeName().isBlank()) {
            return company.getTradeName();
        }

        if (company.getName() != null && !company.getName().isBlank()) {
            return company.getName();
        }

        return "-";
    }

    private String formatLocationLabel(
            String city,
            String state,
            String terminal
    ) {
        StringBuilder builder = new StringBuilder();

        if (city != null && !city.isBlank()) {
            builder.append(city.trim());
        }

        if (state != null && !state.isBlank()) {
            if (!builder.isEmpty()) {
                builder.append(" - ");
            }

            builder.append(state.trim());
        }

        if (terminal != null && !terminal.isBlank()) {
            if (!builder.isEmpty()) {
                builder.append(" | ");
            }

            builder.append(terminal.trim());
        }

        if (builder.isEmpty()) {
            return "-";
        }

        return builder.toString();
    }
}