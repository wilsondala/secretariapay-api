package com.vairapido.api.service;

import com.vairapido.api.dto.whatsapp.WhatsAppMessageResponse;
import com.vairapido.api.entity.*;
import com.vairapido.api.entity.enums.BookingStatus;
import com.vairapido.api.entity.enums.WhatsAppMessageStatus;
import com.vairapido.api.entity.enums.WhatsAppMessageType;
import com.vairapido.api.exception.NotFoundException;
import com.vairapido.api.repository.BookingRepository;
import com.vairapido.api.repository.TicketRepository;
import com.vairapido.api.repository.WhatsAppMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class WhatsAppService {

    private final WhatsAppMessageRepository whatsAppMessageRepository;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;

    public WhatsAppService(
            WhatsAppMessageRepository whatsAppMessageRepository,
            BookingRepository bookingRepository,
            TicketRepository ticketRepository
    ) {
        this.whatsAppMessageRepository = whatsAppMessageRepository;
        this.bookingRepository = bookingRepository;
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public WhatsAppMessageResponse sendPaymentInstructions(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Reserva não encontrada."));

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new IllegalArgumentException("Só é possível enviar instruções de pagamento para reserva pendente.");
        }

        Passenger passenger = booking.getPassenger();

        if (passenger.getWhatsapp() == null || passenger.getWhatsapp().isBlank()) {
            throw new IllegalArgumentException("O passageiro não possui WhatsApp cadastrado.");
        }

        WhatsAppMessage message = new WhatsAppMessage()
                .setBooking(booking)
                .setMessageType(WhatsAppMessageType.PAYMENT_INSTRUCTIONS)
                .setStatus(WhatsAppMessageStatus.PENDING)
                .setToPhone(passenger.getWhatsapp())
                .setPassengerName(passenger.getFullName())
                .setReferenceCode(booking.getBookingCode())
                .setMessageBody(buildPaymentInstructionsMessage(booking))
                .setProviderName("WHATSAPP_SIMULADO");

        return toResponse(whatsAppMessageRepository.save(message));
    }

    @Transactional
    public WhatsAppMessageResponse sendTicket(UUID ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Bilhete não encontrado."));

        Booking booking = ticket.getBooking();
        Passenger passenger = booking.getPassenger();

        if (passenger.getWhatsapp() == null || passenger.getWhatsapp().isBlank()) {
            throw new IllegalArgumentException("O passageiro não possui WhatsApp cadastrado.");
        }

        WhatsAppMessage message = new WhatsAppMessage()
                .setBooking(booking)
                .setTicket(ticket)
                .setMessageType(WhatsAppMessageType.TICKET_ISSUED)
                .setStatus(WhatsAppMessageStatus.PENDING)
                .setToPhone(passenger.getWhatsapp())
                .setPassengerName(passenger.getFullName())
                .setReferenceCode(ticket.getTicketCode())
                .setMessageBody(buildTicketMessage(ticket))
                .setProviderName("WHATSAPP_SIMULADO");

        return toResponse(whatsAppMessageRepository.save(message));
    }

    @Transactional(readOnly = true)
    public List<WhatsAppMessageResponse> findAll() {
        return whatsAppMessageRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public WhatsAppMessageResponse findById(UUID id) {
        return toResponse(findEntityById(id));
    }

    @Transactional
    public WhatsAppMessageResponse markAsSent(UUID id) {
        WhatsAppMessage message = findEntityById(id);

        message
                .setStatus(WhatsAppMessageStatus.SENT)
                .setSentAt(LocalDateTime.now())
                .setErrorMessage(null)
                .setProviderMessageId("WPP-SIM-" + UUID.randomUUID());

        return toResponse(whatsAppMessageRepository.save(message));
    }

    @Transactional
    public WhatsAppMessageResponse markAsFailed(UUID id) {
        WhatsAppMessage message = findEntityById(id);

        message
                .setStatus(WhatsAppMessageStatus.FAILED)
                .setFailedAt(LocalDateTime.now())
                .setErrorMessage("Falha simulada no envio da mensagem WhatsApp.");

        return toResponse(whatsAppMessageRepository.save(message));
    }

    private WhatsAppMessage findEntityById(UUID id) {
        return whatsAppMessageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Mensagem WhatsApp não encontrada."));
    }

    private String buildPaymentInstructionsMessage(Booking booking) {
        Trip trip = booking.getTrip();
        TravelRoute route = trip.getRoute();
        TransportCompany company = trip.getTransportCompany();

        return """
                Olá, %s! 👋

                Sua reserva no VaiRápido foi criada com sucesso.

                Código da reserva: %s
                Empresa: %s
                Rota: %s → %s
                Data/Hora: %s
                Poltrona: %s
                Valor: %s %s

                Para concluir a compra, realize o pagamento dentro do prazo.
                Após a confirmação, o seu bilhete digital com QR Code será emitido automaticamente.

                VaiRápido — sua passagem em poucos minutos.
                """.formatted(
                booking.getPassenger().getFullName(),
                booking.getBookingCode(),
                company.getTradeName() != null ? company.getTradeName() : company.getName(),
                route.getOriginCity(),
                route.getDestinationCity(),
                formatDateTime(trip.getDepartureAt()),
                booking.getSeatNumber(),
                booking.getCurrency(),
                booking.getAmount()
        );
    }

    private String buildTicketMessage(Ticket ticket) {
        Booking booking = ticket.getBooking();
        Trip trip = booking.getTrip();
        TravelRoute route = trip.getRoute();
        TransportCompany company = trip.getTransportCompany();

        return """
                Olá, %s! ✅

                Seu bilhete VaiRápido foi emitido com sucesso.

                Código do bilhete: %s
                Código da reserva: %s
                Empresa: %s
                Rota: %s → %s
                Data/Hora: %s
                Poltrona: %s

                Link de validação:
                %s

                QR Code:
                %s

                Apresente este bilhete no embarque.

                Boa viagem!
                """.formatted(
                booking.getPassenger().getFullName(),
                ticket.getTicketCode(),
                booking.getBookingCode(),
                company.getTradeName() != null ? company.getTradeName() : company.getName(),
                route.getOriginCity(),
                route.getDestinationCity(),
                formatDateTime(trip.getDepartureAt()),
                booking.getSeatNumber(),
                ticket.getValidationUrl(),
                ticket.getQrCodeUrl()
        );
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private WhatsAppMessageResponse toResponse(WhatsAppMessage message) {
        return new WhatsAppMessageResponse()
                .setId(message.getId())
                .setBookingId(message.getBooking() != null ? message.getBooking().getId() : null)
                .setTicketId(message.getTicket() != null ? message.getTicket().getId() : null)
                .setMessageType(message.getMessageType())
                .setStatus(message.getStatus())
                .setToPhone(message.getToPhone())
                .setPassengerName(message.getPassengerName())
                .setReferenceCode(message.getReferenceCode())
                .setMessageBody(message.getMessageBody())
                .setProviderName(message.getProviderName())
                .setProviderMessageId(message.getProviderMessageId())
                .setErrorMessage(message.getErrorMessage())
                .setSentAt(message.getSentAt())
                .setFailedAt(message.getFailedAt())
                .setCreatedAt(message.getCreatedAt())
                .setUpdatedAt(message.getUpdatedAt());
    }
}