package com.vairapido.api.service;

import com.vairapido.api.dto.booking.BookingRequest;
import com.vairapido.api.dto.booking.BookingResponse;
import com.vairapido.api.dto.dashboard.DashboardSummaryResponse;
import com.vairapido.api.dto.payment.PaymentRequest;
import com.vairapido.api.dto.payment.PaymentResponse;
import com.vairapido.api.dto.publicticket.TicketValidationResponse;
import com.vairapido.api.dto.ticket.TicketRequest;
import com.vairapido.api.dto.ticket.TicketResponse;
import com.vairapido.api.dto.whatsappcommand.WhatsappCommandResult;
import com.vairapido.api.dto.whatsappsession.WhatsappSessionResponse;
import com.vairapido.api.entity.Booking;
import com.vairapido.api.entity.Passenger;
import com.vairapido.api.entity.TransportCompany;
import com.vairapido.api.entity.TravelRoute;
import com.vairapido.api.entity.Trip;
import com.vairapido.api.entity.User;
import com.vairapido.api.entity.WhatsappSession;
import com.vairapido.api.entity.enums.BookingStatus;
import com.vairapido.api.entity.enums.PassengerDocumentType;
import com.vairapido.api.entity.enums.PaymentMethod;
import com.vairapido.api.entity.enums.TripStatus;
import com.vairapido.api.entity.enums.UserRole;
import com.vairapido.api.entity.enums.UserStatus;
import com.vairapido.api.entity.enums.WhatsappConversationStep;
import com.vairapido.api.entity.enums.WhatsappSessionType;
import com.vairapido.api.repository.BookingRepository;
import com.vairapido.api.repository.PassengerRepository;
import com.vairapido.api.repository.TicketRepository;
import com.vairapido.api.repository.TripRepository;
import com.vairapido.api.repository.UserRepository;
import com.vairapido.api.repository.WhatsappSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WhatsappCommandService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Pattern TICKET_CODE_PATTERN = Pattern.compile("(VRTK-[A-Z0-9\\-]+|VRTK\\s*[A-Z0-9\\-]+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern TRIP_SEARCH_DATE_PATTERN = Pattern.compile("(\\d{2}[/-]\\d{2}[/-]\\d{4})");

    private static final Pattern TRIP_OPTION_PATTERN = Pattern.compile("\\b(?:viagem|opcao|opção)\\s*(\\d+)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern BOOKING_CODE_PATTERN = Pattern.compile("\\bVR\\d{6,}\\b", Pattern.CASE_INSENSITIVE);

    private static final List<BookingStatus> ACTIVE_SEAT_STATUSES = List.of(
            BookingStatus.PENDING_PAYMENT,
            BookingStatus.PAID,
            BookingStatus.TICKET_ISSUED);

    private final UserRepository userRepository;
    private final DashboardService dashboardService;
    private final PublicTicketValidationService publicTicketValidationService;
    private final TripRepository tripRepository;
    private final WhatsappSessionRepository whatsappSessionRepository;
    private final PassengerRepository passengerRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final TicketService ticketService;
    private final TicketRepository ticketRepository;
    private final WhatsappFaqAnswerService whatsappFaqAnswerService;
    private final DocumentValidatorService documentValidatorService;

    public WhatsappCommandService(
            UserRepository userRepository,
            DashboardService dashboardService,
            PublicTicketValidationService publicTicketValidationService,
            TripRepository tripRepository,
            WhatsappSessionRepository whatsappSessionRepository,
            PassengerRepository passengerRepository,
            BookingRepository bookingRepository,
            BookingService bookingService,
            PaymentService paymentService,
            TicketService ticketService,
            TicketRepository ticketRepository,
            WhatsappFaqAnswerService whatsappFaqAnswerService,
            DocumentValidatorService documentValidatorService) {
        this.userRepository = userRepository;
        this.dashboardService = dashboardService;
        this.publicTicketValidationService = publicTicketValidationService;
        this.tripRepository = tripRepository;
        this.whatsappSessionRepository = whatsappSessionRepository;
        this.passengerRepository = passengerRepository;
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.ticketService = ticketService;
        this.ticketRepository = ticketRepository;
        this.whatsappFaqAnswerService = whatsappFaqAnswerService;
        this.documentValidatorService = documentValidatorService;
    }

    @Transactional
    public WhatsappCommandResult handleCommand(
            WhatsappSessionResponse session,
            String messageText) {
        String normalizedMessage = normalizeText(messageText);

        if (normalizedMessage.isBlank()) {
            return defaultHelp(session);
        }

        if (containsAny(normalizedMessage, "menu", "ajuda", "inicio", "começar", "comecar")) {
            return menu(session);
        }

        if (WhatsappSessionType.PASSENGER.equals(session.getSessionType())
                && WhatsappConversationStep.CONFIRMING_SAVED_PASSENGER.equals(session.getCurrentStep())) {
            return handleSavedPassengerConfirmation(session, normalizedMessage);
        }

        if (WhatsappSessionType.PASSENGER.equals(session.getSessionType())
                && WhatsappConversationStep.CONFIRMING_PASSENGER_DATA.equals(session.getCurrentStep())) {
            return handlePassengerDataConfirmation(session, normalizedMessage);
        }

        if (WhatsappSessionType.PASSENGER.equals(session.getSessionType())
                && WhatsappConversationStep.ASKING_FULL_NAME.equals(session.getCurrentStep())) {
            return handlePassengerNameAnswer(session, messageText);
        }

        if (WhatsappSessionType.PASSENGER.equals(session.getSessionType())
                && WhatsappConversationStep.ASKING_DOCUMENT.equals(session.getCurrentStep())) {
            return handlePassengerDocumentAnswer(session, messageText);
        }

        if (WhatsappSessionType.PASSENGER.equals(session.getSessionType())
                && isIssueTicketCommand(normalizedMessage)) {
            return issueTicketFromWhatsapp(session, messageText);
        }

        if (WhatsappSessionType.USER.equals(session.getSessionType())
                && isTicketValidationCommand(normalizedMessage)) {
            return validateTicket(session, messageText);
        }

        if (isDashboardCommand(normalizedMessage)) {
            return dashboard(session);
        }

        if (WhatsappSessionType.PASSENGER.equals(session.getSessionType())
                && isPaymentCommand(normalizedMessage)) {
            return payBookingFromWhatsapp(session, messageText);
        }

        if (WhatsappSessionType.PASSENGER.equals(session.getSessionType())
                && WhatsappConversationStep.CHOOSING_TRIP.equals(session.getCurrentStep())
                && isTripOptionSelection(messageText)) {
            return createBookingFromSelectedTrip(session, messageText);
        }

        if (WhatsappSessionType.PASSENGER.equals(session.getSessionType())
                && WhatsappConversationStep.WAITING_PAYMENT.equals(session.getCurrentStep())
                && isOptionOne(normalizedMessage)) {
            return payBookingFromWhatsapp(session, "Pagar reserva");
        }

        if (WhatsappSessionType.PASSENGER.equals(session.getSessionType())
                && WhatsappConversationStep.CONFIRMING_BOOKING.equals(session.getCurrentStep())
                && isOptionOne(normalizedMessage)) {
            return issueTicketFromWhatsapp(session, "Emitir bilhete");
        }

        if (isBuyTicketCommand(normalizedMessage)) {
            return buyTicket(session);
        }

        if (WhatsappSessionType.PASSENGER.equals(session.getSessionType())) {
            TripSearchInput tripSearchInput = parseTripSearch(messageText);

            if (tripSearchInput != null) {
                return searchTripsForPassenger(session, tripSearchInput);
            }
        }

        return fallback(session, messageText);
    }

    private WhatsappCommandResult validateTicket(
            WhatsappSessionResponse session,
            String originalMessage) {
        if (!isAllowedToValidateTickets(session)) {
            return denied(
                    "VALIDATE_TICKET",
                    "Acesso negado. Este número não está autorizado para validar bilhetes.");
        }

        String ticketCode = extractTicketCode(originalMessage);

        if (ticketCode == null || ticketCode.isBlank()) {
            return allowed(
                    "VALIDATE_TICKET",
                    "Envie o código do bilhete para validação.\n\nExemplo:\nValidar bilhete VRTK-ABC123");
        }

        TicketValidationResponse validation = publicTicketValidationService.validateByCode(ticketCode);

        StringBuilder reply = new StringBuilder();

        if (Boolean.TRUE.equals(validation.getValid())) {
            reply.append("✅ Bilhete válido para embarque.\n\n");
        } else {
            reply.append("⚠️ Bilhete não liberado.\n\n");
        }

        reply.append("Mensagem: ").append(safe(validation.getMessage())).append("\n");
        reply.append("Bilhete: ").append(safe(validation.getTicketCode())).append("\n");

        if (validation.getTicketStatus() != null) {
            reply.append("Status do bilhete: ").append(validation.getTicketStatus()).append("\n");
        }

        if (validation.getBookingStatus() != null) {
            reply.append("Status da reserva: ").append(validation.getBookingStatus()).append("\n");
        }

        if (validation.getPassengerName() != null) {
            reply.append("\nPassageiro: ").append(validation.getPassengerName()).append("\n");
        }

        if (validation.getSeatNumber() != null) {
            reply.append("Poltrona: ").append(validation.getSeatNumber()).append("\n");
        }

        if (validation.getOriginCity() != null && validation.getDestinationCity() != null) {
            reply.append("Trecho: ")
                    .append(validation.getOriginCity())
                    .append(" → ")
                    .append(validation.getDestinationCity())
                    .append("\n");
        }

        if (validation.getDepartureAt() != null) {
            reply.append("Saída: ")
                    .append(validation.getDepartureAt().format(DATE_TIME_FORMATTER))
                    .append("\n");
        }

        if (Boolean.TRUE.equals(validation.getValid())) {
            reply.append("\nPara confirmar embarque, use o painel ou o endpoint de embarque.");
        }

        return allowed("VALIDATE_TICKET", reply.toString());
    }

    private WhatsappCommandResult dashboard(WhatsappSessionResponse session) {
        Optional<User> optionalUser = findActiveUserBySession(session);

        if (optionalUser.isEmpty()) {
            return denied(
                    "DASHBOARD",
                    "Acesso negado. Este número não está vinculado a um usuário ativo.");
        }

        User user = optionalUser.get();

        DashboardSummaryResponse summary;

        if (UserRole.ADMIN.equals(user.getRole())) {
            summary = dashboardService.getSummary();
        } else if (UserRole.COMPANY_ADMIN.equals(user.getRole())
                && user.getTransportCompany() != null) {
            summary = dashboardService.getCompanySummary(
                    user.getTransportCompany().getId());
        } else {
            return denied(
                    "DASHBOARD",
                    "Acesso negado. Este perfil não possui acesso ao dashboard pelo WhatsApp.");
        }

        String companyLine = "";
        if (user.getTransportCompany() != null) {
            String companyName = user.getTransportCompany().getTradeName() != null
                    ? user.getTransportCompany().getTradeName()
                    : user.getTransportCompany().getName();

            companyLine = "Empresa: " + companyName + "\n";
        }

        String reply = """
                📊 Resumo VaiRápido

                %sViagens: %d
                Reservas: %d
                Reservas pagas: %d
                Bilhetes emitidos: %d
                Bilhetes usados: %d
                Receita confirmada: %s

                Atualizado em: %s
                """.formatted(
                companyLine,
                summary.getTotalTrips(),
                summary.getTotalBookings(),
                summary.getPaidBookings(),
                summary.getIssuedTicketBookings(),
                summary.getUsedTickets(),
                formatMoney(summary.getConfirmedRevenue()),
                summary.getGeneratedAt() != null
                        ? summary.getGeneratedAt().format(DATE_TIME_FORMATTER)
                        : "-");

        return allowed("DASHBOARD", reply.trim());
    }

    private WhatsappCommandResult buyTicket(WhatsappSessionResponse session) {
        if (!WhatsappSessionType.PASSENGER.equals(session.getSessionType())) {
            return allowed(
                    "BUY_TICKET",
                    "Este comando é destinado ao passageiro.\n\nPara operação, use:\n- Validar bilhete VRTK-...\n- Resumo de hoje");
        }

        updateSessionStep(
                session,
                WhatsappConversationStep.PASSENGER_IDENTIFICATION,
                "buy_ticket_started=true");

        String nameLine = session.getPassengerFullName() != null
                ? "Olá, " + session.getPassengerFullName() + ".\n"
                : "Olá. Vamos iniciar sua compra.\n";

        String reply = nameLine + """

                Para comprar sua passagem, preciso destes dados:

                1. Cidade de origem
                2. Cidade de destino
                3. Data da viagem
                4. Documento do passageiro

                Envie no formato:
                Origem: São Paulo
                Destino: Rio de Janeiro
                Data: 25/06/2026

                Ou envie em duas linhas:
                São Paulo
                Rio de Janeiro 25/06/2026
                """;

        return allowed("BUY_TICKET", reply.trim());
    }

    private WhatsappCommandResult searchTripsForPassenger(
            WhatsappSessionResponse session,
            TripSearchInput input) {
        LocalDateTime startDateTime = input.date().atStartOfDay();
        LocalDateTime endDateTime = input.date().plusDays(1).atStartOfDay();

        List<Trip> trips = tripRepository.searchAvailableTrips(
                input.origin(),
                input.destination(),
                startDateTime,
                endDateTime,
                TripStatus.SCHEDULED);

        List<Trip> options = trips.stream()
                .sorted(Comparator.comparing(Trip::getDepartureAt))
                .limit(5)
                .toList();

        if (options.isEmpty()) {
            updateSessionStep(
                    session,
                    WhatsappConversationStep.PASSENGER_IDENTIFICATION,
                    buildTripSearchMetadata(input, options));

            String reply = """
                    Não encontrei viagens disponíveis para:

                    Origem: %s
                    Destino: %s
                    Data: %s

                    Tente novamente com outro destino, outra data ou envie no formato:

                    Origem: São Paulo
                    Destino: Rio de Janeiro
                    Data: 25/06/2026
                    """.formatted(
                    input.origin(),
                    input.destination(),
                    input.date().format(DATE_FORMATTER));

            return allowed("SEARCH_TRIPS", reply.trim());
        }

        updateSessionStep(
                session,
                WhatsappConversationStep.CHOOSING_TRIP,
                buildTripSearchMetadata(input, options));

        StringBuilder reply = new StringBuilder();

        reply.append("🚌 Encontrei viagens disponíveis\n\n");

        for (int i = 0; i < options.size(); i++) {
            Trip trip = options.get(i);
            reply.append(formatTripOption(i + 1, trip)).append("\n\n");
        }

        reply.append("Escolha uma opção respondendo apenas com o número.\n\n");
        reply.append("Exemplo: 1\n\n");
        reply.append("Para mudar a busca, envie novamente origem, destino e data.");

        return allowed("SEARCH_TRIPS", reply.toString().trim());
    }

    private WhatsappCommandResult createBookingFromSelectedTrip(
            WhatsappSessionResponse session,
            String messageText) {
        if (!WhatsappSessionType.PASSENGER.equals(session.getSessionType())) {
            return denied(
                    "CREATE_BOOKING",
                    "Este comando é destinado ao passageiro.");
        }

        Integer optionNumber = extractSelectedOptionNumber(messageText);

        if (optionNumber == null || optionNumber < 1) {
            return allowed(
                    "CREATE_BOOKING",
                    "Não consegui identificar a viagem escolhida.\n\nResponda no formato:\nViagem 1");
        }

        UUID tripId = extractTripIdFromMetadata(session.getMetadata(), optionNumber);

        if (tripId == null) {
            return allowed(
                    "CREATE_BOOKING",
                    "Não encontrei esta opção na sua sessão atual.\n\nFaça uma nova busca enviando origem, destino e data.");
        }

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Viagem não encontrada."));

        String metadata = appendMetadata(
                session.getMetadata(),
                "selected_option=" + optionNumber,
                "selected_trip_id=" + trip.getId());

        Optional<Passenger> optionalPassenger = findRealPassengerForWhatsapp(session);

        if (optionalPassenger.isPresent()) {
            Passenger passenger = optionalPassenger.get();

            updateSessionStep(
                    session,
                    WhatsappConversationStep.CONFIRMING_SAVED_PASSENGER,
                    metadata);

            PassengerDocumentType documentType = getPassengerDocumentType(passenger);
            String firstName = extractFirstName(passenger.getFullName());
            String documentLabel = documentValidatorService.label(documentType);
            String maskedDocument = documentValidatorService.mask(documentType, passenger.getDocumentNumber());

            String reply = """
                    Olá %s, encontrei seus dados salvos:

                    Passageiro: %s
                    %s: %s

                    Essa compra é para esse passageiro?

                    1. Sim, continuar
                    2. Não, comprar para outra pessoa
                    3. Alterar meus dados
                    """.formatted(
                    firstName,
                    passenger.getFullName(),
                    documentLabel,
                    maskedDocument);

            return allowed("CONFIRM_PASSENGER", reply.trim());
        }

        updateSessionStep(
                session,
                WhatsappConversationStep.ASKING_FULL_NAME,
                metadata);

        return allowed(
                "ASK_PASSENGER_NAME",
                """
                        Antes de finalizar sua reserva, preciso dos dados do passageiro.

                        Qual é o nome completo do passageiro?
                        """.trim());
    }

    private WhatsappCommandResult payBookingFromWhatsapp(
            WhatsappSessionResponse session,
            String messageText) {
        if (!WhatsappSessionType.PASSENGER.equals(session.getSessionType())) {
            return denied(
                    "PAY_BOOKING",
                    "Este comando é destinado ao passageiro.");
        }

        String bookingCode = extractBookingCode(messageText);

        if (bookingCode == null || bookingCode.isBlank()) {
            bookingCode = extractMetadataValue(session.getMetadata(), "booking_code");
        }

        if (bookingCode == null || bookingCode.isBlank()) {
            return allowed(
                    "PAY_BOOKING",
                    """
                            Não encontrei uma reserva pendente na sua sessão.

                            Envie o código da reserva no formato:
                            Pagar reserva VR123456

                            Ou faça uma nova busca de viagem.
                            """);
        }

        try {
            Booking booking = bookingRepository.findByBookingCode(bookingCode)
                    .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada."));

            if (booking.getPassenger() != null
                    && booking.getPassenger().getWhatsapp() != null
                    && session.getPhoneNumber() != null
                    && !booking.getPassenger().getWhatsapp().equals(session.getPhoneNumber())) {
                return denied(
                        "PAY_BOOKING",
                        "Esta reserva pertence a outro passageiro.");
            }

            if (BookingStatus.PAID.equals(booking.getStatus())
                    || BookingStatus.TICKET_ISSUED.equals(booking.getStatus())) {
                String reply = """
                        Esta reserva já está paga.

                        Código da reserva: %s
                        Status: %s

                        Próximo passo:
                        Envie "Emitir bilhete %s".
                        """.formatted(
                        booking.getBookingCode(),
                        booking.getStatus(),
                        booking.getBookingCode());

                return allowed("PAY_BOOKING", reply.trim());
            }

            if (!BookingStatus.PENDING_PAYMENT.equals(booking.getStatus())) {
                String reply = """
                        Não é possível pagar esta reserva agora.

                        Código da reserva: %s
                        Status atual: %s

                        Faça uma nova busca ou fale com o suporte.
                        """.formatted(
                        booking.getBookingCode(),
                        booking.getStatus());

                return allowed("PAY_BOOKING", reply.trim());
            }

            if (booking.getExpiresAt() != null && booking.getExpiresAt().isBefore(LocalDateTime.now())) {
                String reply = """
                        Esta reserva já passou do prazo de pagamento.

                        Código da reserva: %s
                        Expirou em: %s

                        Faça uma nova busca de viagem para criar outra reserva.
                        """.formatted(
                        booking.getBookingCode(),
                        booking.getExpiresAt().format(DATE_TIME_FORMATTER));

                return allowed("PAY_BOOKING", reply.trim());
            }

            PaymentRequest paymentRequest = new PaymentRequest()
                    .setBookingId(booking.getId())
                    .setMethod(PaymentMethod.PIX);

            PaymentResponse createdPayment = paymentService.create(paymentRequest);
            PaymentResponse confirmedPayment = paymentService.confirm(createdPayment.getId());

            String metadata = appendMetadata(
                    session.getMetadata(),
                    "payment_id=" + confirmedPayment.getId(),
                    "payment_code=" + confirmedPayment.getPaymentCode(),
                    "payment_method=" + confirmedPayment.getMethod(),
                    "payment_status=" + confirmedPayment.getStatus(),
                    "booking_status=" + confirmedPayment.getBookingStatus());

            updateSessionStep(
                    session,
                    WhatsappConversationStep.CONFIRMING_BOOKING,
                    metadata);

            String reply = """
                    💳 Pagamento confirmado

                    🎫 Reserva: %s
                    💰 Valor: %s %s
                    ✅ Status: %s
                    📍 Trecho: %s → %s
                    🕒 Saída: %s
                    💺 Poltrona: %d

                    Escolha uma opção:
                    1️⃣ Emitir bilhete agora
                    2️⃣ Fazer nova busca
                    """.formatted(
                    confirmedPayment.getBookingCode(),
                    confirmedPayment.getCurrency(),
                    confirmedPayment.getAmount() != null
                            ? confirmedPayment.getAmount().toPlainString()
                            : "0.00",
                    confirmedPayment.getBookingStatus(),
                    confirmedPayment.getOriginCity(),
                    confirmedPayment.getDestinationCity(),
                    confirmedPayment.getDepartureAt() != null
                            ? confirmedPayment.getDepartureAt().format(DATE_TIME_FORMATTER)
                            : "-",
                    confirmedPayment.getSeatNumber());

            return allowed("PAY_BOOKING", reply.trim());

        } catch (Exception exception) {
            return allowed(
                    "PAY_BOOKING",
                    "Não foi possível confirmar o pagamento agora.\n\nMotivo: "
                            + exception.getMessage()
                            + "\n\nTente novamente ou envie uma nova busca.");
        }
    }

    private WhatsappCommandResult issueTicketFromWhatsapp(
            WhatsappSessionResponse session,
            String messageText) {
        if (!WhatsappSessionType.PASSENGER.equals(session.getSessionType())) {
            return denied(
                    "ISSUE_TICKET",
                    "Este comando é destinado ao passageiro.");
        }

        String bookingCode = extractBookingCode(messageText);

        if (bookingCode == null || bookingCode.isBlank()) {
            bookingCode = extractMetadataValue(session.getMetadata(), "booking_code");
        }

        if (bookingCode == null || bookingCode.isBlank()) {
            return allowed(
                    "ISSUE_TICKET",
                    """
                            Não encontrei uma reserva paga na sua sessão.

                            Envie o código da reserva no formato:
                            Emitir bilhete VR123456

                            Ou faça uma nova compra.
                            """);
        }

        try {
            Booking booking = bookingRepository.findByBookingCode(bookingCode)
                    .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada."));

            if (booking.getPassenger() != null
                    && booking.getPassenger().getWhatsapp() != null
                    && session.getPhoneNumber() != null
                    && !booking.getPassenger().getWhatsapp().equals(session.getPhoneNumber())) {
                return denied(
                        "ISSUE_TICKET",
                        "Esta reserva pertence a outro passageiro.");
            }

            if (BookingStatus.TICKET_ISSUED.equals(booking.getStatus())) {
                Optional<TicketResponse> optionalExistingTicket = ticketRepository
                        .findByBooking_Id(booking.getId())
                        .map(ticket -> ticketService.findById(ticket.getId()));

                if (optionalExistingTicket.isPresent()) {
                    TicketResponse existingTicket = optionalExistingTicket.get();

                    String metadata = appendMetadata(
                            session.getMetadata(),
                            "ticket_id=" + existingTicket.getId(),
                            "ticket_code=" + existingTicket.getTicketCode(),
                            "ticket_status=" + existingTicket.getStatus(),
                            "ticket_pdf_url=" + buildTicketPdfUrl(existingTicket.getId()));

                    updateSessionStep(
                            session,
                            WhatsappConversationStep.TICKET_ISSUED,
                            metadata);

                    return allowed(
                            "ISSUE_TICKET",
                            formatTicketIssuedReply(existingTicket, true));
                }

                return allowed(
                        "ISSUE_TICKET",
                        "Esta reserva já está marcada como bilhete emitido, mas não encontrei o bilhete vinculado. Fale com o suporte.");
            }

            if (BookingStatus.PENDING_PAYMENT.equals(booking.getStatus())) {
                String reply = """
                        Esta reserva ainda está pendente de pagamento.

                        Código da reserva: %s
                        Status: %s

                        Primeiro envie:
                        Pagar reserva %s
                        """.formatted(
                        booking.getBookingCode(),
                        booking.getStatus(),
                        booking.getBookingCode());

                return allowed("ISSUE_TICKET", reply.trim());
            }

            if (!BookingStatus.PAID.equals(booking.getStatus())) {
                String reply = """
                        Não é possível emitir bilhete para esta reserva agora.

                        Código da reserva: %s
                        Status atual: %s

                        Faça uma nova compra ou fale com o suporte.
                        """.formatted(
                        booking.getBookingCode(),
                        booking.getStatus());

                return allowed("ISSUE_TICKET", reply.trim());
            }

            TicketRequest ticketRequest = new TicketRequest()
                    .setBookingId(booking.getId());

            TicketResponse ticket = ticketService.issue(ticketRequest);

            String metadata = appendMetadata(
                    session.getMetadata(),
                    "ticket_id=" + ticket.getId(),
                    "ticket_code=" + ticket.getTicketCode(),
                    "ticket_status=" + ticket.getStatus(),
                    "ticket_pdf_url=" + buildTicketPdfUrl(ticket.getId()));

            updateSessionStep(
                    session,
                    WhatsappConversationStep.TICKET_ISSUED,
                    metadata);

            return allowed(
                    "ISSUE_TICKET",
                    formatTicketIssuedReply(ticket, false));

        } catch (Exception exception) {
            return allowed(
                    "ISSUE_TICKET",
                    "Não foi possível emitir o bilhete agora.\n\nMotivo: "
                            + exception.getMessage()
                            + "\n\nTente novamente ou fale com o suporte.");
        }
    }

    private String formatTicketIssuedReply(TicketResponse ticket, boolean alreadyIssued) {
        String title = alreadyIssued
                ? "🎫 Bilhete já emitido para esta reserva."
                : "🎫 Bilhete emitido com sucesso.";

        return """
                %s

                Código do bilhete: %s
                Reserva: %s
                Status do bilhete: %s
                Passageiro: %s
                Trecho: %s → %s
                Saída: %s
                Poltrona: %d
                Valor: %s %s

                Validação:
                %s

                PDF:
                %s
                """.formatted(
                title,
                ticket.getTicketCode(),
                ticket.getBookingCode(),
                ticket.getStatus(),
                ticket.getPassengerName(),
                ticket.getOriginCity(),
                ticket.getDestinationCity(),
                ticket.getDepartureAt() != null
                        ? ticket.getDepartureAt().format(DATE_TIME_FORMATTER)
                        : "-",
                ticket.getSeatNumber(),
                ticket.getCurrency(),
                ticket.getAmount() != null ? ticket.getAmount().toPlainString() : "0.00",
                ticket.getValidationUrl(),
                buildTicketPdfUrl(ticket.getId())).trim();
    }

    private String buildTicketPdfUrl(UUID ticketId) {
        if (ticketId == null) {
            return "-";
        }

        return "https://api-vairapido.triacompany.com/api/v1/public/tickets/"
                + ticketId
                + "/pdf";
    }

    private String formatTripOption(int optionNumber, Trip trip) {
        TravelRoute route = trip.getRoute();
        TransportCompany company = trip.getTransportCompany();

        String companyName = "-";

        if (company != null) {
            companyName = company.getTradeName() != null && !company.getTradeName().isBlank()
                    ? company.getTradeName()
                    : company.getName();
        }

        String origin = route != null ? route.getOriginCity() : "-";
        String destination = route != null ? route.getDestinationCity() : "-";
        String departure = trip.getDepartureAt() != null
                ? trip.getDepartureAt().format(DATE_TIME_FORMATTER)
                : "-";

        String currency = trip.getCurrency() != null && !trip.getCurrency().isBlank()
                ? trip.getCurrency()
                : documentValidatorService.defaultCurrency();

        String price = trip.getPrice() != null
                ? trip.getPrice().toPlainString()
                : "0.00";

        Integer availableSeats = trip.getAvailableSeats() != null
                ? trip.getAvailableSeats()
                : 0;

        return """
                %d️⃣ %s
                📍 %s → %s
                🕒 Saída: %s
                💰 Valor: %s %s
                💺 Lugares: %d
                """.formatted(
                optionNumber,
                companyName,
                origin,
                destination,
                departure,
                currency,
                price,
                availableSeats).trim();
    }

    private WhatsappCommandResult menu(WhatsappSessionResponse session) {
        if (WhatsappSessionType.USER.equals(session.getSessionType())) {
            return allowed(
                    "MENU",
                    """
                            Menu VaiRápido Operacional

                            Comandos disponíveis:
                            1. Validar bilhete VRTK-...
                            2. Resumo de hoje
                            3. Ajuda
                            """);
        }

        return allowed(
                "MENU",
                """
                        Menu VaiRápido

                        Comandos disponíveis:
                        1. Comprar passagem
                        2. Consultar bilhete
                        3. Ajuda
                        """);
    }

    private WhatsappCommandResult defaultHelp(WhatsappSessionResponse session) {
        return menu(session);
    }

    private WhatsappCommandResult fallback(WhatsappSessionResponse session, String messageText) {
        Optional<String> faqAnswer = whatsappFaqAnswerService.answer(
                messageText,
                session != null ? session.getSessionType() : null);

        if (faqAnswer.isPresent()) {
            return allowed("FAQ", faqAnswer.get());
        }
        if (WhatsappSessionType.USER.equals(session.getSessionType())) {
            return allowed(
                    "FALLBACK",
                    """
                            Não entendi o comando.

                            Tente uma destas opções:
                            - Validar bilhete VRTK-...
                            - Resumo de hoje
                            - Menu
                            """);
        }

        return allowed(
                "FALLBACK",
                """
                        Não entendi sua mensagem.

                        Para comprar passagem, envie:
                        Comprar passagem

                        Ou envie direto no formato:
                        Origem: São Paulo
                        Destino: Rio de Janeiro
                        Data: 25/06/2026
                        """);
    }

    private TripSearchInput parseTripSearch(String messageText) {
        if (messageText == null || messageText.isBlank()) {
            return null;
        }

        LocalDate date = extractSearchDate(messageText);

        if (date == null) {
            return null;
        }

        String origin = extractLabeledValue(messageText, "origem");
        String destination = extractLabeledValue(messageText, "destino");

        if (isFilled(origin) && isFilled(destination)) {
            return new TripSearchInput(
                    cleanSearchTerm(origin),
                    cleanSearchTerm(destination),
                    date);
        }

        List<String> lines = extractMeaningfulLinesWithoutDate(messageText);

        if (lines.size() < 2) {
            return null;
        }

        origin = cleanSearchTerm(lines.get(0));
        destination = cleanSearchTerm(lines.get(1));

        if (!isFilled(origin) || !isFilled(destination)) {
            return null;
        }

        return new TripSearchInput(origin, destination, date);
    }

    private LocalDate extractSearchDate(String messageText) {
        Matcher matcher = TRIP_SEARCH_DATE_PATTERN.matcher(messageText);

        if (!matcher.find()) {
            return null;
        }

        String rawDate = matcher.group(1).replace("-", "/");

        try {
            return LocalDate.parse(rawDate, DATE_FORMATTER);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String extractLabeledValue(String messageText, String label) {
        for (String line : splitLines(messageText)) {
            String normalizedLine = normalizeText(line);

            if (!normalizedLine.startsWith(label)) {
                continue;
            }

            String value = line.replaceFirst("(?iu)^\\s*" + label + "\\s*:?\\s*", "");

            if (isFilled(value)) {
                return value.trim();
            }
        }

        return null;
    }

    private List<String> extractMeaningfulLinesWithoutDate(String messageText) {
        String withoutDate = TRIP_SEARCH_DATE_PATTERN
                .matcher(messageText)
                .replaceAll("");

        List<String> lines = new ArrayList<>();

        for (String line : splitLines(withoutDate)) {
            String cleaned = line
                    .replaceFirst("(?iu)^\\s*origem\\s*:?\\s*", "")
                    .replaceFirst("(?iu)^\\s*destino\\s*:?\\s*", "")
                    .replaceFirst("(?iu)^\\s*data\\s*:?\\s*", "")
                    .trim();

            if (isFilled(cleaned)) {
                lines.add(cleaned);
            }
        }

        return lines;
    }

    private String[] splitLines(String text) {
        return text
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .split("\n");
    }

    private String cleanSearchTerm(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace(";", "")
                .replace("\"", "")
                .trim();
    }

    private boolean isPaymentCommand(String normalizedMessage) {
        return normalizedMessage.contains("pagar reserva")
                || normalizedMessage.contains("paguei")
                || normalizedMessage.contains("confirmar pagamento")
                || normalizedMessage.matches(".*\\bpagar\\s+vr\\d{6,}.*")
                || normalizedMessage.matches(".*\\bpagamento\\s+vr\\d{6,}.*");
    }

    private boolean isIssueTicketCommand(String normalizedMessage) {
        return normalizedMessage.contains("emitir bilhete")
                || normalizedMessage.contains("gerar bilhete")
                || normalizedMessage.contains("emissao bilhete")
                || normalizedMessage.contains("emissão bilhete")
                || normalizedMessage.contains("emitir ticket")
                || normalizedMessage.contains("gerar ticket")
                || normalizedMessage.contains("emitir passagem")
                || normalizedMessage.contains("gerar passagem");
    }

    private String extractBookingCode(String messageText) {
        if (messageText == null || messageText.isBlank()) {
            return null;
        }

        Matcher matcher = BOOKING_CODE_PATTERN.matcher(messageText);

        if (!matcher.find()) {
            return null;
        }

        return matcher.group(0)
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private String extractMetadataValue(String metadata, String key) {
        if (metadata == null || metadata.isBlank() || key == null || key.isBlank()) {
            return null;
        }

        String prefix = key + "=";
        String foundValue = null;

        for (String line : splitLines(metadata)) {
            if (line.startsWith(prefix)) {
                String value = line.substring(prefix.length()).trim();
                foundValue = value.isBlank() ? null : value;
            }
        }

        return foundValue;
    }

    private boolean isTripOptionSelection(String messageText) {
        return extractSelectedOptionNumber(messageText) != null;
    }

    private Integer extractSelectedOptionNumber(String messageText) {
        if (messageText == null || messageText.isBlank()) {
            return null;
        }

        String cleaned = messageText.trim();

        if (cleaned.matches("\\d+")) {
            try {
                return Integer.parseInt(cleaned);
            } catch (NumberFormatException exception) {
                return null;
            }
        }

        Matcher matcher = TRIP_OPTION_PATTERN.matcher(messageText);

        if (!matcher.find()) {
            return null;
        }

        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private UUID extractTripIdFromMetadata(String metadata, int optionNumber) {
        if (metadata == null || metadata.isBlank()) {
            return null;
        }

        String key = "option_" + optionNumber + "=";

        for (String line : splitLines(metadata)) {
            if (!line.startsWith(key)) {
                continue;
            }

            String rawId = line.substring(key.length()).trim();

            try {
                return UUID.fromString(rawId);
            } catch (IllegalArgumentException exception) {
                return null;
            }
        }

        return null;
    }

    private WhatsappCommandResult handleSavedPassengerConfirmation(
            WhatsappSessionResponse session,
            String normalizedMessage) {
        Optional<Passenger> optionalPassenger = findRealPassengerForWhatsapp(session);

        if (isOptionOne(normalizedMessage) || containsAny(normalizedMessage, "sim", "confirmar", "continuar")) {
            if (optionalPassenger.isEmpty()) {
                updateSessionStep(
                        session,
                        WhatsappConversationStep.ASKING_FULL_NAME,
                        session.getMetadata());

                return allowed(
                        "ASK_PASSENGER_NAME",
                        "Não encontrei mais seus dados salvos.\n\nQual é o nome completo do passageiro?");
            }

            return createBookingWithPassenger(session, optionalPassenger.get());
        }

        if (isOptionTwo(normalizedMessage)
                || containsAny(normalizedMessage, "nao", "não", "outro", "outra pessoa")) {
            String metadata = appendMetadata(
                    session.getMetadata(),
                    "passenger_mode=other");

            updateSessionStep(
                    session,
                    WhatsappConversationStep.ASKING_FULL_NAME,
                    metadata);

            return allowed(
                    "ASK_OTHER_PASSENGER_NAME",
                    """
                            Perfeito. Vamos comprar para outro passageiro.

                            Qual é o nome completo do passageiro?
                            """.trim());
        }

        if (isOptionThree(normalizedMessage)
                || containsAny(normalizedMessage, "alterar", "atualizar", "corrigir")) {
            String metadata = appendMetadata(
                    session.getMetadata(),
                    "passenger_mode=update");

            updateSessionStep(
                    session,
                    WhatsappConversationStep.ASKING_FULL_NAME,
                    metadata);

            return allowed(
                    "ASK_UPDATE_PASSENGER_NAME",
                    """
                            Vamos atualizar seus dados.

                            Qual é o nome completo do passageiro?
                            """.trim());
        }

        return allowed(
                "CONFIRM_PASSENGER",
                """
                        Não consegui entender sua resposta.

                        Essa compra é para o passageiro salvo?

                        1. Sim, continuar
                        2. Não, comprar para outra pessoa
                        3. Alterar meus dados
                        """.trim());
    }

    private WhatsappCommandResult handlePassengerNameAnswer(
            WhatsappSessionResponse session,
            String messageText) {
        String fullName = messageText == null ? "" : messageText.trim();

        if (fullName.length() < 5 || !fullName.contains(" ")) {
            return allowed(
                    "ASK_PASSENGER_NAME",
                    """
                            Informe o nome completo do passageiro.

                            Exemplo:
                            Wilson Dala
                            """.trim());
        }

        String metadata = appendMetadata(
                session.getMetadata(),
                "pending_passenger_name=" + fullName);

        updateSessionStep(
                session,
                WhatsappConversationStep.ASKING_DOCUMENT,
                metadata);

        PassengerDocumentType documentType = resolveDocumentTypeFromMetadata(metadata);
        String firstName = extractFirstName(fullName);
        String documentLabel = documentValidatorService.label(documentType);

        String reply = """
                Obrigado, %s.

                Agora informe o %s do passageiro.
                """.formatted(firstName, documentLabel);

        return allowed("ASK_PASSENGER_DOCUMENT", reply.trim());
    }

    private WhatsappCommandResult handlePassengerDocumentAnswer(
            WhatsappSessionResponse session,
            String messageText) {
        String fullName = extractMetadataValue(session.getMetadata(), "pending_passenger_name");

        if (fullName == null || fullName.isBlank()) {
            updateSessionStep(
                    session,
                    WhatsappConversationStep.ASKING_FULL_NAME,
                    session.getMetadata());

            return allowed(
                    "ASK_PASSENGER_NAME",
                    "Não encontrei o nome do passageiro na sessão.\n\nQual é o nome completo do passageiro?");
        }

        PassengerDocumentType documentType = resolveDocumentTypeFromMetadata(session.getMetadata());
        String documentNumber = documentValidatorService.normalize(documentType, messageText);
        String documentLabel = documentValidatorService.label(documentType);

        if (!documentValidatorService.isValid(documentType, documentNumber)) {
            String example = PassengerDocumentType.BI.equals(documentType)
                    ? "Exemplo: 006543219LA042"
                    : "Exemplo: 52998224725";

            return allowed(
                    "ASK_PASSENGER_DOCUMENT",
                    """
                            %s inválido. Verifique os dados e envie novamente.

                            %s
                            """.formatted(documentLabel, example).trim());
        }

        String passengerMode = extractMetadataValue(session.getMetadata(), "passenger_mode");

        Passenger passenger;

        if ("update".equalsIgnoreCase(passengerMode)) {
            passenger = passengerRepository.findByDocumentNumber(documentNumber)
                    .orElseGet(() -> findPassengerForWhatsapp(session).orElseGet(Passenger::new));

            passenger
                    .setFullName(fullName)
                    .setDocumentType(documentType)
                    .setDocumentNumber(documentNumber)
                    .setPhone(session.getPhoneNumber())
                    .setWhatsapp(session.getPhoneNumber());

            passenger = passengerRepository.save(passenger);
        } else if ("other".equalsIgnoreCase(passengerMode)) {
            passenger = passengerRepository.findByDocumentNumber(documentNumber)
                    .orElseGet(Passenger::new);

            passenger
                    .setFullName(fullName)
                    .setDocumentType(documentType)
                    .setDocumentNumber(documentNumber)
                    .setPhone(session.getPhoneNumber());

            passenger = passengerRepository.save(passenger);
        } else {
            passenger = passengerRepository.findByDocumentNumber(documentNumber)
                    .orElseGet(() -> findPassengerForWhatsapp(session).orElseGet(Passenger::new));

            passenger
                    .setFullName(fullName)
                    .setDocumentType(documentType)
                    .setDocumentNumber(documentNumber)
                    .setPhone(session.getPhoneNumber())
                    .setWhatsapp(session.getPhoneNumber());

            passenger = passengerRepository.save(passenger);
        }

        updateSessionPassenger(session, passenger);

        String maskedDocument = documentValidatorService.mask(
                documentType,
                passenger.getDocumentNumber());

        String confirmationMetadata = appendMetadata(
                session.getMetadata(),
                "confirmed_passenger_id=" + passenger.getId(),
                "confirmed_passenger_name=" + passenger.getFullName(),
                "confirmed_document_type=" + documentType,
                "confirmed_document_number=" + passenger.getDocumentNumber());
        updateSessionStep(
                session,
                WhatsappConversationStep.CONFIRMING_PASSENGER_DATA,
                confirmationMetadata);

        String reply = """
                🧾 Confirme os dados do passageiro

                🧍 Passageiro: %s
                🪪 %s: %s

                Você confirma a emissão da passagem para este passageiro?

                1️⃣ Sim, confirmar
                2️⃣ Corrigir dados
                """.formatted(
                passenger.getFullName(),
                documentLabel,
                maskedDocument);

        return allowed("CONFIRM_PASSENGER_DATA", reply.trim());
    }

    private WhatsappCommandResult handlePassengerDataConfirmation(
            WhatsappSessionResponse session,
            String normalizedMessage) {
        if (isOptionOne(normalizedMessage)
                || containsAny(normalizedMessage, "sim", "confirmo", "confirmar", "continuar")) {

            String passengerId = extractMetadataValue(session.getMetadata(), "confirmed_passenger_id");

            if (passengerId == null || passengerId.isBlank()) {
                updateSessionStep(
                        session,
                        WhatsappConversationStep.ASKING_FULL_NAME,
                        session.getMetadata());

                return allowed(
                        "ASK_PASSENGER_NAME",
                        "Não encontrei os dados confirmados do passageiro.\n\nQual é o nome completo do passageiro?");
            }

            try {
                Passenger passenger = passengerRepository.findById(UUID.fromString(passengerId))
                        .orElseThrow(() -> new IllegalArgumentException("Passageiro não encontrado."));

                return createBookingWithPassenger(session, passenger);

            } catch (Exception exception) {
                updateSessionStep(
                        session,
                        WhatsappConversationStep.ASKING_FULL_NAME,
                        session.getMetadata());

                return allowed(
                        "ASK_PASSENGER_NAME",
                        "Não consegui confirmar o passageiro informado.\n\nQual é o nome completo do passageiro?");
            }
        }

        if (isOptionTwo(normalizedMessage)
                || containsAny(normalizedMessage, "corrigir", "alterar", "nao", "não")) {
            String metadata = appendMetadata(
                    session.getMetadata(),
                    "passenger_correction_requested=true");

            updateSessionStep(
                    session,
                    WhatsappConversationStep.ASKING_FULL_NAME,
                    metadata);

            return allowed(
                    "ASK_PASSENGER_NAME",
                    """
                            Sem problema. Vamos corrigir os dados do passageiro.

                            Qual é o nome completo do passageiro?
                            """.trim());
        }

        return allowed(
                "CONFIRM_PASSENGER_DATA",
                """
                        Não consegui entender sua resposta.

                        Você confirma a emissão da passagem para este passageiro?

                        1. Sim, confirmar
                        2. Corrigir dados
                        """.trim());
    }

    private WhatsappCommandResult createBookingWithPassenger(
            WhatsappSessionResponse session,
            Passenger passenger) {
        UUID tripId = extractSelectedTripIdFromMetadata(session.getMetadata());

        if (tripId == null) {
            updateSessionStep(
                    session,
                    WhatsappConversationStep.PASSENGER_IDENTIFICATION,
                    session.getMetadata());

            return allowed(
                    "CREATE_BOOKING",
                    "Não encontrei a viagem escolhida na sessão.\n\nFaça uma nova busca enviando origem, destino e data.");
        }

        try {
            Trip trip = tripRepository.findById(tripId)
                    .orElseThrow(() -> new IllegalArgumentException("Viagem não encontrada."));

            Integer seatNumber = findFirstAvailableSeat(trip);

            BookingRequest request = new BookingRequest()
                    .setTripId(trip.getId())
                    .setPassengerId(passenger.getId())
                    .setSeatNumber(seatNumber);

            BookingResponse booking = bookingService.create(request);

            PassengerDocumentType documentType = getPassengerDocumentType(passenger);
            String documentLabel = documentValidatorService.label(documentType);
            String maskedDocument = documentValidatorService.mask(documentType, passenger.getDocumentNumber());

            String metadata = appendMetadata(
                    session.getMetadata(),
                    "booking_id=" + booking.getId(),
                    "booking_code=" + booking.getBookingCode(),
                    "seat_number=" + booking.getSeatNumber(),
                    "passenger_id=" + passenger.getId(),
                    "passenger_name=" + passenger.getFullName(),
                    "passenger_document_type=" + documentType,
                    "passenger_document=" + passenger.getDocumentNumber());

            updateSessionStep(
                    session,
                    WhatsappConversationStep.WAITING_PAYMENT,
                    metadata);

            String reply = """
                    ✅ Reserva criada

                    🎫 Código: %s
                    🧍 Passageiro: %s
                    🪪 %s: %s
                    📍 Trecho: %s → %s
                    🕒 Saída: %s
                    💺 Poltrona: %d
                    💰 Valor: %s %s
                    ⏳ Expira em: %s

                    Escolha uma opção:
                    1️⃣ Pagar agora
                    2️⃣ Fazer nova busca
                    """.formatted(
                    booking.getBookingCode(),
                    booking.getPassengerName(),
                    documentLabel,
                    maskedDocument,
                    booking.getOriginCity(),
                    booking.getDestinationCity(),
                    booking.getDepartureAt() != null
                            ? booking.getDepartureAt().format(DATE_TIME_FORMATTER)
                            : "-",
                    booking.getSeatNumber(),
                    booking.getCurrency(),
                    booking.getAmount() != null ? booking.getAmount().toPlainString() : "0.00",
                    booking.getExpiresAt() != null
                            ? booking.getExpiresAt().format(DATE_TIME_FORMATTER)
                            : "-");

            return allowed("CREATE_BOOKING", reply.trim());

        } catch (Exception exception) {
            return allowed(
                    "CREATE_BOOKING",
                    "Não foi possível criar a reserva agora.\n\nMotivo: "
                            + exception.getMessage()
                            + "\n\nFaça uma nova busca ou tente novamente.");
        }
    }

    private Optional<Passenger> findPassengerForWhatsapp(WhatsappSessionResponse session) {
        if (session == null) {
            return Optional.empty();
        }

        if (session.getPassengerId() != null) {
            Optional<Passenger> byId = passengerRepository.findById(session.getPassengerId());

            if (byId.isPresent()) {
                return byId;
            }
        }

        if (session.getPhoneNumber() == null || session.getPhoneNumber().isBlank()) {
            return Optional.empty();
        }

        return passengerRepository.findByWhatsapp(session.getPhoneNumber());
    }

    private Optional<Passenger> findRealPassengerForWhatsapp(WhatsappSessionResponse session) {
        return findPassengerForWhatsapp(session)
                .filter(passenger -> passenger.getFullName() != null)
                .filter(passenger -> passenger.getDocumentNumber() != null)
                .filter(passenger -> !passenger.getFullName().startsWith("Passageiro WhatsApp"))
                .filter(passenger -> !passenger.getDocumentNumber().startsWith("WPP"));
    }

    private void updateSessionPassenger(
            WhatsappSessionResponse session,
            Passenger passenger) {
        if (session == null || session.getId() == null || passenger == null) {
            return;
        }

        whatsappSessionRepository.findById(session.getId())
                .ifPresent(whatsappSession -> {
                    whatsappSession.setPassenger(passenger);
                    whatsappSessionRepository.save(whatsappSession);
                });
    }

    private UUID extractSelectedTripIdFromMetadata(String metadata) {
        String selectedTripId = extractMetadataValue(metadata, "selected_trip_id");

        if (selectedTripId != null && !selectedTripId.isBlank()) {
            try {
                return UUID.fromString(selectedTripId);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        String selectedOption = extractMetadataValue(metadata, "selected_option");

        if (selectedOption == null || selectedOption.isBlank()) {
            return null;
        }

        try {
            return extractTripIdFromMetadata(metadata, Integer.parseInt(selectedOption));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Passenger ensurePassengerForWhatsapp(WhatsappSessionResponse session) {
        if (session.getPassengerId() != null) {
            Optional<Passenger> byId = passengerRepository.findById(session.getPassengerId());

            if (byId.isPresent()) {
                return byId.get();
            }
        }

        Optional<Passenger> byWhatsapp = passengerRepository.findByWhatsapp(session.getPhoneNumber());

        if (byWhatsapp.isPresent()) {
            return byWhatsapp.get();
        }

        String documentNumber = buildWhatsappDocumentNumber(session.getPhoneNumber());

        Optional<Passenger> byDocument = passengerRepository.findByDocumentNumber(documentNumber);

        if (byDocument.isPresent()) {
            Passenger passenger = byDocument.get();

            passenger
                    .setPhone(session.getPhoneNumber())
                    .setWhatsapp(session.getPhoneNumber());

            return passengerRepository.save(passenger);
        }

        String fullName = session.getPassengerFullName() != null
                && !session.getPassengerFullName().isBlank()
                        ? session.getPassengerFullName()
                        : "Passageiro WhatsApp " + session.getPhoneNumber();

        Passenger passenger = new Passenger()
                .setFullName(fullName)
                .setDocumentType(documentValidatorService.defaultDocumentType())
                .setDocumentNumber(documentNumber)
                .setPhone(session.getPhoneNumber())
                .setWhatsapp(session.getPhoneNumber());

        return passengerRepository.save(passenger);
    }

    private String buildWhatsappDocumentNumber(String phoneNumber) {
        String digits = phoneNumber == null
                ? String.valueOf(System.currentTimeMillis())
                : phoneNumber.replaceAll("\\D", "");

        if (digits.isBlank()) {
            digits = String.valueOf(System.currentTimeMillis());
        }

        String document = "WPP" + digits;

        if (document.length() > 30) {
            document = document.substring(0, 30);
        }

        return document;
    }

    private Integer findFirstAvailableSeat(Trip trip) {
        if (trip.getTotalSeats() == null || trip.getTotalSeats() <= 0) {
            throw new IllegalArgumentException("Viagem sem configuração válida de assentos.");
        }

        for (int seatNumber = 1; seatNumber <= trip.getTotalSeats(); seatNumber++) {
            boolean taken = bookingRepository.existsByTrip_IdAndSeatNumberAndStatusIn(
                    trip.getId(),
                    seatNumber,
                    ACTIVE_SEAT_STATUSES);

            if (!taken) {
                return seatNumber;
            }
        }

        throw new IllegalArgumentException("Não há poltronas disponíveis para esta viagem.");
    }

    private void updateSessionStep(
            WhatsappSessionResponse session,
            WhatsappConversationStep step,
            String metadata) {
        if (session == null || session.getId() == null) {
            return;
        }

        Optional<WhatsappSession> optionalSession = whatsappSessionRepository.findById(session.getId());

        if (optionalSession.isEmpty()) {
            return;
        }

        WhatsappSession whatsappSession = optionalSession.get();

        whatsappSession
                .setCurrentStep(step)
                .setMetadata(metadata);

        whatsappSessionRepository.save(whatsappSession);
    }

    private String buildTripSearchMetadata(
            TripSearchInput input,
            List<Trip> options) {
        StringBuilder metadata = new StringBuilder();

        metadata.append("flow=BUY_TICKET\n");
        metadata.append("origin=").append(input.origin()).append("\n");
        metadata.append("destination=").append(input.destination()).append("\n");
        metadata.append("date=").append(input.date().format(DATE_FORMATTER)).append("\n");

        for (int i = 0; i < options.size(); i++) {
            metadata
                    .append("option_")
                    .append(i + 1)
                    .append("=")
                    .append(options.get(i).getId())
                    .append("\n");
        }

        return metadata.toString().trim();
    }

    private String appendMetadata(String currentMetadata, String... linesToAppend) {
        StringBuilder metadata = new StringBuilder();

        if (currentMetadata != null && !currentMetadata.isBlank()) {
            metadata.append(currentMetadata.trim()).append("\n");
        }

        for (String line : linesToAppend) {
            if (line != null && !line.isBlank()) {
                metadata.append(line.trim()).append("\n");
            }
        }

        return metadata.toString().trim();
    }

    private PassengerDocumentType resolveDocumentTypeFromMetadata(String metadata) {
        String rawType = extractMetadataValue(metadata, "document_type");

        if (rawType == null || rawType.isBlank()) {
            return documentValidatorService.defaultDocumentType();
        }

        try {
            return PassengerDocumentType.valueOf(rawType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return documentValidatorService.defaultDocumentType();
        }
    }

    private PassengerDocumentType getPassengerDocumentType(Passenger passenger) {
        if (passenger == null || passenger.getDocumentType() == null) {
            return PassengerDocumentType.CPF;
        }

        return passenger.getDocumentType();
    }

    private boolean isAllowedToValidateTickets(WhatsappSessionResponse session) {
        if (session == null || session.getUserRole() == null) {
            return false;
        }

        return UserRole.ADMIN.name().equals(session.getUserRole())
                || UserRole.OPERATOR.name().equals(session.getUserRole());
    }

    private boolean isTicketValidationCommand(String normalizedMessage) {
        return normalizedMessage.contains("validar")
                || normalizedMessage.contains("bilhete")
                || normalizedMessage.contains("ticket")
                || normalizedMessage.contains("embarque");
    }

    private boolean isDashboardCommand(String normalizedMessage) {
        return normalizedMessage.contains("resumo")
                || normalizedMessage.contains("dashboard")
                || normalizedMessage.contains("relatorio")
                || normalizedMessage.contains("financeiro")
                || normalizedMessage.contains("minha empresa");
    }

    private boolean isBuyTicketCommand(String normalizedMessage) {
        return normalizedMessage.contains("comprar")
                || normalizedMessage.contains("compra")
                || normalizedMessage.contains("passagem")
                || normalizedMessage.contains("viajar")
                || normalizedMessage.contains("viagem");
    }

    private Optional<User> findActiveUserBySession(WhatsappSessionResponse session) {
        if (session == null || session.getPhoneNumber() == null) {
            return Optional.empty();
        }

        return userRepository.findByWhatsappAndStatus(
                session.getPhoneNumber(),
                UserStatus.ACTIVE);
    }

    private String extractTicketCode(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        Matcher matcher = TICKET_CODE_PATTERN.matcher(message);

        if (!matcher.find()) {
            return null;
        }

        return matcher.group(1)
                .replace(" ", "")
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private boolean isOptionOne(String normalizedMessage) {
        return "1".equals(normalizedMessage)
                || normalizedMessage.startsWith("1 ");
    }

    private boolean isOptionTwo(String normalizedMessage) {
        return "2".equals(normalizedMessage)
                || normalizedMessage.startsWith("2 ");
    }

    private boolean isOptionThree(String normalizedMessage) {
        return "3".equals(normalizedMessage)
                || normalizedMessage.startsWith("3 ");
    }

    private String extractFirstName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "passageiro";
        }

        return fullName.trim().split("\\s+")[0];
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }

        return false;
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return normalized
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private boolean isFilled(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return "0,00";
        }

        return value.toPlainString();
    }

    private WhatsappCommandResult allowed(
            String commandName,
            String replyMessage) {
        return new WhatsappCommandResult()
                .setProcessed(true)
                .setAllowed(true)
                .setCommandName(commandName)
                .setReplyMessage(replyMessage);
    }

    private WhatsappCommandResult denied(
            String commandName,
            String replyMessage) {
        return new WhatsappCommandResult()
                .setProcessed(true)
                .setAllowed(false)
                .setCommandName(commandName)
                .setReplyMessage(replyMessage);
    }

    private record TripSearchInput(
            String origin,
            String destination,
            LocalDate date) {
    }
}