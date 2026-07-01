package com.secretariapay.api.service;

import com.secretariapay.api.dto.ticket.TicketRequest;
import com.secretariapay.api.dto.ticket.TicketResponse;
import com.secretariapay.api.entity.*;
import com.secretariapay.api.entity.enums.BookingStatus;
import com.secretariapay.api.entity.enums.TicketStatus;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.BookingRepository;
import com.secretariapay.api.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final BookingRepository bookingRepository;

    public TicketService(
            TicketRepository ticketRepository,
            BookingRepository bookingRepository
    ) {
        this.ticketRepository = ticketRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public TicketResponse issue(TicketRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new NotFoundException("Reserva não encontrada."));

        if (booking.getStatus() != BookingStatus.PAID) {
            throw new IllegalArgumentException("A reserva precisa estar paga para emitir o bilhete.");
        }

        if (ticketRepository.existsByBooking_Id(booking.getId())) {
            throw new IllegalArgumentException("Esta reserva já possui bilhete emitido.");
        }

        String ticketCode = generateTicketCode(booking);
        String validationUrl = generateValidationUrl(ticketCode);
        String qrCodeUrl = generateQrCodeUrl(validationUrl);

        Ticket ticket = new Ticket()
                .setBooking(booking)
                .setTicketCode(ticketCode)
                .setValidationUrl(validationUrl)
                .setQrCodeUrl(qrCodeUrl)
                .setStatus(TicketStatus.VALID)
                .setIssuedAt(LocalDateTime.now());

        booking.setStatus(BookingStatus.TICKET_ISSUED);
        bookingRepository.save(booking);

        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> findAll() {
        return ticketRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse findById(UUID id) {
        return toResponse(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public TicketResponse findByCode(String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new NotFoundException("Bilhete não encontrado."));

        return toResponse(ticket);
    }

    @Transactional
    public TicketResponse useTicket(UUID id) {
        Ticket ticket = findEntityById(id);

        if (ticket.getStatus() != TicketStatus.VALID) {
            throw new IllegalArgumentException("Este bilhete não está válido para uso.");
        }

        ticket
                .setStatus(TicketStatus.USED)
                .setUsedAt(LocalDateTime.now());

        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse cancel(UUID id) {
        Ticket ticket = findEntityById(id);

        if (ticket.getStatus() == TicketStatus.USED) {
            throw new IllegalArgumentException("Não é possível cancelar um bilhete já utilizado.");
        }

        ticket
                .setStatus(TicketStatus.CANCELLED)
                .setCancelledAt(LocalDateTime.now());

        Booking booking = ticket.getBooking();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        bookingRepository.save(booking);

        return toResponse(ticketRepository.save(ticket));
    }

    private Ticket findEntityById(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Bilhete não encontrado."));
    }

    private String generateTicketCode(Booking booking) {
        String code;

        do {
            code = "VRTK-" + booking.getBookingCode() + "-" + System.currentTimeMillis();
        } while (ticketRepository.findByTicketCode(code).isPresent());

        return code;
    }

    private String generateValidationUrl(String ticketCode) {
        return "https://api-vairapido.triacompany.com/api/v1/public/tickets/validate/" + ticketCode + "/page";
    }

    private String generateQrCodeUrl(String validationUrl) {
        String encoded = URLEncoder.encode(validationUrl, StandardCharsets.UTF_8);
        return "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=" + encoded;
    }

    private TicketResponse toResponse(Ticket ticket) {
        Booking booking = ticket.getBooking();
        Trip trip = booking.getTrip();
        Passenger passenger = booking.getPassenger();
        TransportCompany company = trip.getTransportCompany();
        TravelRoute route = trip.getRoute();

        return new TicketResponse()
                .setId(ticket.getId())
                .setTicketCode(ticket.getTicketCode())
                .setQrCodeUrl(ticket.getQrCodeUrl())
                .setValidationUrl(ticket.getValidationUrl())
                .setStatus(ticket.getStatus())

                .setBookingId(booking.getId())
                .setBookingCode(booking.getBookingCode())
                .setBookingStatus(booking.getStatus())

                .setPassengerName(passenger.getFullName())
                .setPassengerDocument(passenger.getDocumentNumber())
                .setPassengerWhatsapp(passenger.getWhatsapp())
                .setPassengerFareType(booking.getPassengerFareType())
                .setPassengerAge(booking.getPassengerAge())
                .setFarePercentage(booking.getFarePercentage())
                .setChildGuardianName(booking.getChildGuardianName())
                .setChildGuardianPhone(booking.getChildGuardianPhone())
                .setMinorGuardianName(booking.getMinorGuardianName())
                .setMinorGuardianPhone(booking.getMinorGuardianPhone())
                .setMinorPickupResponsibleName(booking.getMinorPickupResponsibleName())
                .setMinorPickupResponsiblePhone(booking.getMinorPickupResponsiblePhone())

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
                .setAmount(booking.getAmount())
                .setCurrency(booking.getCurrency())

                .setIssuedAt(ticket.getIssuedAt())
                .setUsedAt(ticket.getUsedAt())
                .setCancelledAt(ticket.getCancelledAt())
                .setCreatedAt(ticket.getCreatedAt())
                .setUpdatedAt(ticket.getUpdatedAt());
    }
}

