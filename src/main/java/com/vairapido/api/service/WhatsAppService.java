package com.vairapido.api.service;

import com.vairapido.api.dto.whatsapp.WhatsAppBackfillResponse;
import com.vairapido.api.dto.whatsapp.WhatsAppCloudSendResult;
import com.vairapido.api.dto.whatsapp.WhatsAppCloudStatusResponse;
import com.vairapido.api.dto.whatsapp.WhatsAppMessageResponse;
import com.vairapido.api.entity.Booking;
import com.vairapido.api.entity.Passenger;
import com.vairapido.api.entity.Ticket;
import com.vairapido.api.entity.TransportCompany;
import com.vairapido.api.entity.TravelRoute;
import com.vairapido.api.entity.Trip;
import com.vairapido.api.entity.WhatsAppMessage;
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
    private final WhatsAppCloudClient whatsAppCloudClient;

    public WhatsAppService(
            WhatsAppMessageRepository whatsAppMessageRepository,
            BookingRepository bookingRepository,
            TicketRepository ticketRepository,
            WhatsAppCloudClient whatsAppCloudClient
    ) {
        this.whatsAppMessageRepository = whatsAppMessageRepository;
        this.bookingRepository = bookingRepository;
        this.ticketRepository = ticketRepository;
        this.whatsAppCloudClient = whatsAppCloudClient;
    }

    @Transactional
    public WhatsAppMessageResponse sendPaymentInstructions(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Reserva não encontrada."));

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new IllegalArgumentException("Só é possível enviar instruções de pagamento para reserva pendente.");
        }

        return whatsAppMessageRepository
                .findFirstByBooking_IdAndMessageType(
                        booking.getId(),
                        WhatsAppMessageType.PAYMENT_INSTRUCTIONS
                )
                .map(this::toResponse)
                .orElseGet(() -> toResponse(createPaymentInstructionsMessage(booking)));
    }

    @Transactional
    public WhatsAppMessageResponse sendTicket(UUID ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Bilhete não encontrado."));

        return whatsAppMessageRepository
                .findFirstByTicket_IdAndMessageType(
                        ticket.getId(),
                        WhatsAppMessageType.TICKET_ISSUED
                )
                .map(this::toResponse)
                .orElseGet(() -> toResponse(createTicketIssuedMessage(ticket)));
    }

    @Transactional
    public WhatsAppBackfillResponse backfillMissingMessages() {
        int paymentMessagesCreated = 0;
        int ticketMessagesCreated = 0;
        int skippedWithoutWhatsapp = 0;
        int skippedAlreadyExisting = 0;

        List<Booking> bookings = bookingRepository.findAll();

        for (Booking booking : bookings) {
            if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
                continue;
            }

            if (!hasPassengerWhatsapp(booking.getPassenger())) {
                skippedWithoutWhatsapp++;
                continue;
            }

            boolean alreadyExists = whatsAppMessageRepository.existsByBooking_IdAndMessageType(
                    booking.getId(),
                    WhatsAppMessageType.PAYMENT_INSTRUCTIONS
            );

            if (alreadyExists) {
                skippedAlreadyExisting++;
                continue;
            }

            createPaymentInstructionsMessage(booking);
            paymentMessagesCreated++;
        }

        List<Ticket> tickets = ticketRepository.findAll();

        for (Ticket ticket : tickets) {
            Booking booking = ticket.getBooking();

            if (!hasPassengerWhatsapp(booking.getPassenger())) {
                skippedWithoutWhatsapp++;
                continue;
            }

            boolean alreadyExists = whatsAppMessageRepository.existsByTicket_IdAndMessageType(
                    ticket.getId(),
                    WhatsAppMessageType.TICKET_ISSUED
            );

            if (alreadyExists) {
                skippedAlreadyExisting++;
                continue;
            }

            createTicketIssuedMessage(ticket);
            ticketMessagesCreated++;
        }

        int totalCreated = paymentMessagesCreated + ticketMessagesCreated;

        return new WhatsAppBackfillResponse()
                .setPaymentInstructionMessagesCreated(paymentMessagesCreated)
                .setTicketIssuedMessagesCreated(ticketMessagesCreated)
                .setSkippedWithoutWhatsapp(skippedWithoutWhatsapp)
                .setSkippedAlreadyExisting(skippedAlreadyExisting)
                .setProcessedAt(LocalDateTime.now())
                .setMessage(totalCreated == 1
                        ? "1 mensagem WhatsApp ausente foi criada."
                        : totalCreated + " mensagens WhatsApp ausentes foram criadas.");
    }

    @Transactional(readOnly = true)
    public WhatsAppCloudStatusResponse getCloudStatus() {
        return whatsAppCloudClient.getStatus();
    }

    @Transactional
    public WhatsAppMessageResponse sendRealMessage(UUID id) {
        if (!whatsAppCloudClient.isReady()) {
            throw new IllegalStateException("WhatsApp Cloud API não está configurado para envio real.");
        }

        WhatsAppMessage message = findEntityById(id);

        if (message.getStatus() == WhatsAppMessageStatus.SENT) {
            return toResponse(message);
        }

        WhatsAppCloudSendResult result = whatsAppCloudClient.sendTextMessage(
                message.getToPhone(),
                message.getMessageBody()
        );

        if (Boolean.TRUE.equals(result.getSuccess())) {
            message
                    .setStatus(WhatsAppMessageStatus.SENT)
                    .setSentAt(LocalDateTime.now())
                    .setFailedAt(null)
                    .setErrorMessage(null)
                    .setProviderName(WhatsAppCloudClient.PROVIDER_NAME)
                    .setProviderMessageId(result.getProviderMessageId());
        } else {
            message
                    .setStatus(WhatsAppMessageStatus.FAILED)
                    .setFailedAt(LocalDateTime.now())
                    .setErrorMessage(result.getErrorMessage())
                    .setProviderName(WhatsAppCloudClient.PROVIDER_NAME);
        }

        return toResponse(whatsAppMessageRepository.save(message));
    }

    @Transactional
    public List<WhatsAppMessageResponse> sendPendingRealMessages() {
        if (!whatsAppCloudClient.isReady()) {
            throw new IllegalStateException("WhatsApp Cloud API não está configurado para envio real.");
        }

        return whatsAppMessageRepository.findByStatus(WhatsAppMessageStatus.PENDING)
                .stream()
                .map(message -> {
                    WhatsAppCloudSendResult result = whatsAppCloudClient.sendTextMessage(
                            message.getToPhone(),
                            message.getMessageBody()
                    );

                    if (Boolean.TRUE.equals(result.getSuccess())) {
                        message
                                .setStatus(WhatsAppMessageStatus.SENT)
                                .setSentAt(LocalDateTime.now())
                                .setFailedAt(null)
                                .setErrorMessage(null)
                                .setProviderName(WhatsAppCloudClient.PROVIDER_NAME)
                                .setProviderMessageId(result.getProviderMessageId());
                    } else {
                        message
                                .setStatus(WhatsAppMessageStatus.FAILED)
                                .setFailedAt(LocalDateTime.now())
                                .setErrorMessage(result.getErrorMessage())
                                .setProviderName(WhatsAppCloudClient.PROVIDER_NAME);
                    }

                    return toResponse(whatsAppMessageRepository.save(message));
                })
                .toList();
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

    private WhatsAppMessage createPaymentInstructionsMessage(Booking booking) {
        Passenger passenger = booking.getPassenger();

        if (!hasPassengerWhatsapp(passenger)) {
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

        return whatsAppMessageRepository.save(message);
    }

    private WhatsAppMessage createTicketIssuedMessage(Ticket ticket) {
        Booking booking = ticket.getBooking();
        Passenger passenger = booking.getPassenger();

        if (!hasPassengerWhatsapp(passenger)) {
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

        return whatsAppMessageRepository.save(message);
    }

    private boolean hasPassengerWhatsapp(Passenger passenger) {
        return passenger != null
                && passenger.getWhatsapp() != null
                && !passenger.getWhatsapp().isBlank();
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
