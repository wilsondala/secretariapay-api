package com.vairapido.api.service;

import com.vairapido.api.dto.multicountry.CountryResolutionResponse;
import com.vairapido.api.service.multicountry.CountryResolverService;
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
import com.vairapido.api.entity.enums.PassengerFareType;
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
    private final CountryResolverService countryResolverService;

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
            DocumentValidatorService documentValidatorService,
            CountryResolverService countryResolverService) {
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
        this.countryResolverService = countryResolverService;
    }
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public WhatsappCommandResult handleCommand(
            WhatsappSessionResponse session,
            String messageText) {
        String normalizedMessage = normalizeText(messageText);

        if (session == null) {
            return allowed(
                    "FALLBACK",
                    "Não consegui carregar sua sessão. Envie: Comprar passagem");
        }

        if (normalizedMessage.isBlank()) {
            return defaultHelp(session);
        }

        /*
         * Fluxo WhatsApp organizado por estado.
         * Regra principal: a resposta "1" só vale para o estado atual.
         * Isso impede que o mesmo "1" seja interpretado como aceite de termos,
         * pagamento, emissão de bilhete ou escolha de viagem ao mesmo tempo.
         */
        if (WhatsappSessionType.PASSENGER.equals(session.getSessionType())) {

            if (isGreetingNamePending(session.getMetadata())) {
                return handleGreetingNameAnswer(session, messageText);
            }

            if (containsAny(normalizedMessage, "menu", "ajuda", "inicio", "começar", "comecar")) {
                return menu(session);
            }

            if (isBuyTicketCommand(normalizedMessage)) {
                return restartPassengerPurchase(session, "buy_ticket_command");
            }

            if (isAutoResetDueToInactivity(session.getMetadata())) {
                return restartPassengerPurchase(session, "inactivity_timeout");
            }

            if (isTripTypeSelectionPending(session.getMetadata())) {
                return handleTripTypeSelection(session, normalizedMessage);
            }

            if (isOutboundTripSearchPending(session.getMetadata())) {
                return handleOutboundTripSearchAnswer(session, messageText);
            }

            if (isReturnTripSearchPending(session.getMetadata())) {
                return handleReturnTripSearchAnswer(session, messageText);
            }

            if (isPassengerFareTypeSelectionPending(session.getMetadata())) {
                return handlePassengerFareTypeSelection(session, normalizedMessage);
            }

            if (WhatsappConversationStep.CHOOSING_TRIP.equals(session.getCurrentStep())) {
                if (isReturnTripSelectionPending(session.getMetadata())) {
                    if (isTripOptionSelection(messageText)) {
                        return confirmReturnTripSelectionAndAskPassenger(session, messageText);
                    }

                    return allowed(
                            "CHOOSE_RETURN_TRIP",
                            "Não consegui identificar a viagem de volta escolhida.\n\nResponda apenas com o número da volta. Exemplo: 1");
                }

                if (isTripOptionSelection(messageText)) {
                    return createBookingFromSelectedTrip(session, messageText);
                }

                TripSearchInput newSearch = parseTripSearch(messageText);

                if (newSearch != null) {
                    return searchTripsForPassenger(session, newSearch);
                }

                return allowed(
                        "CHOOSE_TRIP",
                        "Não consegui identificar a viagem escolhida.\n\nResponda apenas com o número da viagem. Exemplo: 1");
            }

            if (WhatsappConversationStep.CONFIRMING_SAVED_PASSENGER.equals(session.getCurrentStep())) {
                return handleSavedPassengerConfirmation(session, normalizedMessage);
            }

            if (WhatsappConversationStep.ASKING_FULL_NAME.equals(session.getCurrentStep())) {
                return handlePassengerNameAnswer(session, messageText);
            }

            if (WhatsappConversationStep.ASKING_DOCUMENT.equals(session.getCurrentStep())) {
                return handlePassengerDocumentAnswer(session, messageText);
            }

            if (WhatsappConversationStep.CONFIRMING_PASSENGER_DATA.equals(session.getCurrentStep())) {
                return handlePassengerDataConfirmation(session, normalizedMessage);
            }

            if (WhatsappConversationStep.WAITING_PAYMENT.equals(session.getCurrentStep())) {
                if (isPaymentMethodSelectionPending(session.getMetadata())) {
                    if (isPaymentMethodOption(normalizedMessage)) {
                        return payBookingWithSelectedPaymentMethod(session, normalizedMessage);
                    }

                    String bookingCode = extractMetadataValue(session.getMetadata(), "booking_code");
                    Booking booking = bookingCode != null
                            ? bookingRepository.findByBookingCode(bookingCode).orElse(null)
                            : null;

                    return allowed(
                            "PAYMENT_METHOD",
                            "Opção de pagamento inválida.\n\n"
                                    + buildPaymentMethodOptionsCard(session.getMetadata(), booking));
                }

                if (isOptionOne(normalizedMessage) || isPaymentCommand(normalizedMessage)) {
                    return askPaymentMethodFromWhatsapp(session, "Pagar reserva");
                }

                TripSearchInput newSearch = parseTripSearch(messageText);

                if (newSearch != null) {
                    return searchTripsForPassenger(session, newSearch);
                }

                return allowed(
                        "WAITING_PAYMENT",
                        "Sua reserva está criada e aguardando pagamento.\n\nResponda:\n1️⃣ Pagar agora\n2️⃣ Fazer nova busca");
            }

            if (WhatsappConversationStep.CONFIRMING_BOOKING.equals(session.getCurrentStep())) {
                if (isPaidBookingSelectionPending(session.getMetadata())
                        && isIssueTicketSelection(normalizedMessage)) {
                    return issueTicketFromPaidBookingSelection(session, normalizedMessage);
                }

                if (isOptionOne(normalizedMessage) || isIssueTicketCommand(normalizedMessage)) {
                    return issueTicketFromWhatsapp(session, "Emitir bilhete");
                }

                return allowed(
                        "CONFIRMING_BOOKING",
                        "Pagamento confirmado.\n\nResponda:\n1️⃣ Emitir bilhete agora\n2️⃣ Fazer nova busca");
            }

            if (isSmartGreeting(normalizedMessage)) {
                if (hasCustomerName(session.getMetadata())) {
                    return handleKnownCustomerGreeting(session, normalizedMessage);
                }

                return askCustomerNameFromGreeting(session, normalizedMessage);
            }

            if (isRecoverTicketCommand(normalizedMessage)) {
                return recoverPaidTicketFromWhatsapp(session);
            }

            if (isIssueTicketCommand(normalizedMessage)) {
                return issueTicketFromWhatsapp(session, messageText);
            }

            if (isPaymentCommand(normalizedMessage)) {
                return payBookingFromWhatsapp(session, messageText);
            }

            TripSearchInput tripSearchInput = parseTripSearch(messageText);

            if (tripSearchInput != null) {
                return searchTripsForPassenger(session, tripSearchInput);
            }
        }

        if (containsAny(normalizedMessage, "menu", "ajuda", "inicio", "começar", "comecar")) {
            return menu(session);
        }

        if (WhatsappSessionType.USER.equals(session.getSessionType())
                && isTicketValidationCommand(normalizedMessage)) {
            return validateTicket(session, messageText);
        }

        if (isDashboardCommand(normalizedMessage)) {
            return dashboard(session);
        }

        if (isBuyTicketCommand(normalizedMessage)) {
            return buyTicket(session);
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

            private WhatsappCommandResult askCustomerNameFromGreeting(
            WhatsappSessionResponse session,
            String normalizedMessage) {
        String greeting = resolveGreetingLabel(normalizedMessage);

        String metadata = appendMetadata(
                session.getMetadata(),
                "greeting_name_pending=true",
                "last_greeting=" + greeting);

        updateSessionStep(
                session,
                WhatsappConversationStep.START,
                metadata);

        String reply = """
                %s. Bem-vindo ao VaiRápido. 👋

                Antes de continuar, como posso te chamar?
                """.formatted(greeting);

        return allowed("ASK_CUSTOMER_NAME", reply.trim());
    }

    private WhatsappCommandResult handleKnownCustomerGreeting(
            WhatsappSessionResponse session,
            String normalizedMessage) {
        String greeting = resolveGreetingLabel(normalizedMessage);
        String customerName = extractMetadataValue(session.getMetadata(), "customer_name");

        if (customerName == null || customerName.isBlank()) {
            return askCustomerNameFromGreeting(session, normalizedMessage);
        }

        String reply = """
                %s, %s. 😊

                Bem-vindo novamente ao VaiRápido.

                Posso ajudar você a comprar passagem pelo WhatsApp no Brasil e em Angola.

                %s

                O que deseja fazer agora?

                1️⃣ Comprar passagem
                2️⃣ Consultar horários
                3️⃣ Recuperar/Reemitir meu bilhete
                4️⃣ Ver formas de pagamento

                Você também pode enviar direto:
                Origem: Luanda
                Destino: Benguela
                Data: 25/06/2026
                """.formatted(
                greeting,
                customerName,
                buildSupportedCountriesCard());

        return allowed("GREETING_MENU", reply.trim());
    }
    private WhatsappCommandResult handleGreetingNameAnswer(
            WhatsappSessionResponse session,
            String messageText) {
        String name = sanitizeCustomerName(messageText);

        if (name == null || name.isBlank() || name.length() < 2) {
            return allowed(
                    "ASK_CUSTOMER_NAME",
                    """
                            Não consegui identificar seu nome.

                            Por favor, envie apenas seu nome.
                            Exemplo:
                            Wilson
                            """.trim());
        }

        String firstName = extractFirstName(name);

        String metadata = appendMetadata(
                session.getMetadata(),
                "greeting_name_pending=false",
                "customer_name=" + firstName);

        updateSessionStep(
                session,
                WhatsappConversationStep.PASSENGER_IDENTIFICATION,
                metadata);

        String reply = """
                Prazer, %s. 😊

                Sou o assistente do VaiRápido.

                Posso ajudar você a comprar passagem pelo WhatsApp no Brasil e em Angola.

                %s

                O que deseja fazer agora?

                1️⃣ Comprar passagem
                2️⃣ Consultar horários
                3️⃣ Recuperar/Reemitir meu bilhete
                4️⃣ Ver formas de pagamento

                Você também pode enviar direto:
                Origem: Luanda
                Destino: Benguela
                Data: 25/06/2026
                """.formatted(
                firstName,
                buildSupportedCountriesCard());

        return allowed("GREETING_MENU", reply.trim());
    }

        private boolean isSmartGreeting(String normalizedMessage) {
        if (normalizedMessage == null || normalizedMessage.isBlank()) {
            return false;
        }

        String text = normalizedMessage.trim().toLowerCase();

        return text.equals("oi")
                || text.equals("ola")
                || text.equals("olá")
                || text.equals("hello")
                || text.equals("hi")
                || text.equals("bom dia")
                || text.equals("boa dia")
                || text.equals("boa manha")
                || text.equals("boa manhã")
                || text.equals("manha")
                || text.equals("manhã")
                || text.equals("boa tarde")
                || text.equals("tarde")
                || text.equals("boa noite")
                || text.equals("noite")
                || text.equals("bom noite")
                || text.equals("boa madrugada")
                || text.contains("bom dia")
                || text.contains("boa tarde")
                || text.contains("boa noite")
                || text.contains("boa manha")
                || text.contains("boa manhã");
    }


    private boolean isGreetingNamePending(String metadata) {
        String pending = extractMetadataValue(metadata, "greeting_name_pending");
        return "true".equalsIgnoreCase(pending);
    }

    private boolean hasCustomerName(String metadata) {
        String customerName = extractMetadataValue(metadata, "customer_name");
        return customerName != null && !customerName.isBlank();
    }

        private String resolveGreetingLabel(String normalizedMessage) {
        if (normalizedMessage == null) {
            return "Olá";
        }

        String text = normalizedMessage.trim().toLowerCase();

        if (text.contains("tarde")) {
            return "Boa tarde";
        }

        if (text.contains("noite") || text.contains("madrugada")) {
            return "Boa noite";
        }

        if (text.contains("manha") || text.contains("manhã") || text.contains("bom dia") || text.contains("boa dia")) {
            return "Bom dia";
        }

        return "Olá";
    }


    private String sanitizeCustomerName(String value) {
        if (value == null) {
            return "";
        }

        String cleaned = value
                .trim()
                .replaceAll("[0-9]", "")
                .replaceAll("[^\\p{L}\\s'-]", "")
                .replaceAll("\\s+", " ");

        if (cleaned.length() > 60) {
            cleaned = cleaned.substring(0, 60).trim();
        }

        return cleaned;
    }
    
    private boolean isAutoResetDueToInactivity(String metadata) {
        String autoReset = extractMetadataValue(metadata, "auto_reset_due_to_inactivity");
        return "true".equalsIgnoreCase(autoReset);
    }

    private WhatsappCommandResult restartPassengerPurchase(
            WhatsappSessionResponse session,
            String reason) {
        String metadata = appendMetadata(
                null,
                "conversation_reset_at=" + LocalDateTime.now(),
                "conversation_reset_reason=" + reason);

        updateSessionStep(
                session,
                WhatsappConversationStep.PASSENGER_IDENTIFICATION,
                metadata);

        session.setMetadata(metadata);

        return buyTicket(session);
    }

private WhatsappCommandResult buyTicket(WhatsappSessionResponse session) {
        if (!WhatsappSessionType.PASSENGER.equals(session.getSessionType())) {
            return allowed(
                    "BUY_TICKET",
                    "Este comando é destinado ao passageiro.\n\nPara operação, use:\n- Validar bilhete VRTK-...\n- Resumo de hoje");
        }

        String metadata = appendMetadata(
                session.getMetadata(),
                "buy_ticket_started=true",
                "trip_type_selection_pending=true",
                "outbound_search_pending=false",
                "return_search_pending=false");

        updateSessionStep(
                session,
                WhatsappConversationStep.PASSENGER_IDENTIFICATION,
                metadata);

        String nameLine = session.getPassengerFullName() != null
                ? "Olá, " + session.getPassengerFullName() + ".\n"
                : "Olá. Vamos iniciar sua compra.\n";

        String reply = String.join("\n",
                nameLine,
                "Posso ajudar você a comprar sua passagem pelo WhatsApp.",
                "",
                "Como será sua viagem?",
                "",
                "1. Só ida",
                "2. Ida e volta",
                "",
                "Você pode responder com o número ou escrevendo:",
                "- só ida",
                "- ida e volta",
                "",
                buildSupportedCountriesCard()
        );

        return allowed("BUY_TICKET", reply.trim());
    }

    private boolean isTripTypeSelectionPending(String metadata) {
        String pending = extractMetadataValue(metadata, "trip_type_selection_pending");
        return "true".equalsIgnoreCase(pending);
    }

    private boolean isOutboundTripSearchPending(String metadata) {
        String pending = extractMetadataValue(metadata, "outbound_search_pending");
        return "true".equalsIgnoreCase(pending);
    }

    private boolean isReturnTripSearchPending(String metadata) {
        String pending = extractMetadataValue(metadata, "return_search_pending");
        return "true".equalsIgnoreCase(pending);
    }

    private WhatsappCommandResult handleTripTypeSelection(
            WhatsappSessionResponse session,
            String normalizedMessage) {

        if (isRoundTripIntent(normalizedMessage)) {
            String metadata = appendMetadata(
                    session.getMetadata(),
                    "trip_type_selection_pending=false",
                    "trip_type=ROUND_TRIP",
                    "outbound_search_pending=true",
                    "return_search_pending=false");

            updateSessionStep(
                    session,
                    WhatsappConversationStep.PASSENGER_IDENTIFICATION,
                    metadata);

            return allowed("ASK_OUTBOUND_TRIP", buildOutboundTripSearchPrompt(true));
        }

        if (isOneWayIntent(normalizedMessage)) {
            String metadata = appendMetadata(
                    session.getMetadata(),
                    "trip_type_selection_pending=false",
                    "trip_type=ONE_WAY",
                    "outbound_search_pending=true",
                    "return_search_pending=false");

            updateSessionStep(
                    session,
                    WhatsappConversationStep.PASSENGER_IDENTIFICATION,
                    metadata);

            return allowed("ASK_OUTBOUND_TRIP", buildOutboundTripSearchPrompt(false));
        }

        return allowed(
                "ASK_TRIP_TYPE",
                String.join("\n",
                        "Não consegui entender o tipo de viagem.",
                        "",
                        "Responda com número ou escrevendo:",
                        "",
                        "1. Só ida",
                        "2. Ida e volta"));
    }

    private WhatsappCommandResult handleOutboundTripSearchAnswer(
            WhatsappSessionResponse session,
            String messageText) {

        TripSearchInput input = parseTripSearch(messageText);

        if (input == null) {
            boolean roundTrip = "ROUND_TRIP".equalsIgnoreCase(
                    extractMetadataValue(session.getMetadata(), "trip_type"));

            return allowed("ASK_OUTBOUND_TRIP", buildOutboundTripSearchPrompt(roundTrip));
        }

        String tripType = extractMetadataValue(session.getMetadata(), "trip_type");

        if ("ROUND_TRIP".equalsIgnoreCase(tripType) && input.returnDate() == null) {
            String metadata = appendMetadata(
                    session.getMetadata(),
                    "outbound_search_pending=false",
                    "return_search_pending=true",
                    "origin=" + input.origin(),
                    "destination=" + input.destination(),
                    "date=" + input.date().format(DATE_FORMATTER));

            updateSessionStep(
                    session,
                    WhatsappConversationStep.PASSENGER_IDENTIFICATION,
                    metadata);

            return allowed(
                    "ASK_RETURN_TRIP",
                    buildReturnTripSearchPrompt(input.origin(), input.destination(), input.date()));
        }

        TripSearchInput finalInput = new TripSearchInput(
                input.origin(),
                input.destination(),
                input.date(),
                "ROUND_TRIP".equalsIgnoreCase(tripType) ? input.returnDate() : null);

        return searchTripsForPassenger(session, finalInput);
    }

    private WhatsappCommandResult handleReturnTripSearchAnswer(
            WhatsappSessionResponse session,
            String messageText) {

        String origin = extractMetadataValue(session.getMetadata(), "origin");
        String destination = extractMetadataValue(session.getMetadata(), "destination");
        String outboundDateText = extractMetadataValue(session.getMetadata(), "date");

        if (origin == null || destination == null || outboundDateText == null) {
            String metadata = appendMetadata(
                    session.getMetadata(),
                    "trip_type_selection_pending=true",
                    "outbound_search_pending=false",
                    "return_search_pending=false");

            updateSessionStep(
                    session,
                    WhatsappConversationStep.PASSENGER_IDENTIFICATION,
                    metadata);

            return allowed(
                    "ASK_TRIP_TYPE",
                    "Perdi os dados da ida. Vamos reiniciar.\n\nResponda:\n1. Só ida\n2. Ida e volta");
        }

        LocalDate outboundDate;

        try {
            outboundDate = LocalDate.parse(outboundDateText, DATE_FORMATTER);
        } catch (DateTimeParseException exception) {
            return allowed("ASK_OUTBOUND_TRIP", buildOutboundTripSearchPrompt(true));
        }

        TripSearchInput returnInput = parseTripSearch(messageText);
        LocalDate returnDate = returnInput != null ? returnInput.date() : extractSearchDate(messageText);

        if (returnDate == null) {
            return allowed(
                    "ASK_RETURN_TRIP",
                    buildReturnTripSearchPrompt(origin, destination, outboundDate));
        }

        if (returnDate.isBefore(outboundDate)) {
            return allowed(
                    "ASK_RETURN_TRIP",
                    String.join("\n",
                            "A data da volta não pode ser anterior à data da ida.",
                            "",
                            "Informe a volta novamente.",
                            "",
                            "Exemplo:",
                            destination + " > " + origin + " " + outboundDate.plusDays(1).format(DATE_FORMATTER)));
        }

        if (returnInput != null) {
            boolean reverseRoute = cityMatches(returnInput.origin(), destination)
                    && cityMatches(returnInput.destination(), origin);

            if (!reverseRoute) {
                return allowed(
                        "ASK_RETURN_TRIP",
                        String.join("\n",
                                "Para ida e volta, a volta precisa ser o trecho inverso da ida.",
                                "",
                                "Ida: " + origin + " -> " + destination,
                                "Volta esperada: " + destination + " -> " + origin,
                                "",
                                "Exemplo:",
                                destination + " > " + origin + " " + returnDate.format(DATE_FORMATTER)));
            }
        }

        String metadata = appendMetadata(
                session.getMetadata(),
                "return_search_pending=false",
                "return_date=" + returnDate.format(DATE_FORMATTER));

        updateSessionStep(
                session,
                WhatsappConversationStep.PASSENGER_IDENTIFICATION,
                metadata);

        TripSearchInput finalInput = new TripSearchInput(
                origin,
                destination,
                outboundDate,
                returnDate);

        return searchTripsForPassenger(session, finalInput);
    }

    private boolean isRoundTripIntent(String normalizedMessage) {
        if (normalizedMessage == null || normalizedMessage.isBlank()) {
            return false;
        }

        return isOptionTwo(normalizedMessage)
                || containsAny(
                normalizedMessage,
                "ida e volta",
                "ida volta",
                "volta",
                "retorno",
                "regresso");
    }

    private boolean isOneWayIntent(String normalizedMessage) {
        if (normalizedMessage == null || normalizedMessage.isBlank()) {
            return false;
        }

        return isOptionOne(normalizedMessage)
                || containsAny(
                normalizedMessage,
                "so ida",
                "somente ida",
                "apenas ida",
                "ida simples",
                "ida unica")
                || "ida".equals(normalizedMessage.trim());
    }

    private String buildOutboundTripSearchPrompt(boolean roundTrip) {
        if (roundTrip) {
            return String.join("\n",
                    "Ida e volta selecionada.",
                    "",
                    "Primeiro informe os dados da ida.",
                    "",
                    "Envie assim:",
                    "Luanda > Benguela 26/06/2026",
                    "",
                    "Também aceito:",
                    "Origem: Luanda",
                    "Destino: Benguela",
                    "Data: 26/06/2026");
        }

        return String.join("\n",
                "Só ida selecionada.",
                "",
                "Informe os dados da viagem.",
                "",
                "Envie assim:",
                "Luanda > Benguela 26/06/2026",
                "",
                "Também aceito:",
                "Origem: Luanda",
                "Destino: Benguela",
                "Data: 26/06/2026");
    }

    private String buildReturnTripSearchPrompt(
            String origin,
            String destination,
            LocalDate outboundDate) {

        LocalDate suggestedReturnDate = outboundDate != null
                ? outboundDate.plusDays(1)
                : LocalDate.now().plusDays(1);

        return String.join("\n",
                "Agora informe os dados da volta.",
                "",
                "Ida informada: " + origin + " -> " + destination,
                "",
                "Envie assim:",
                destination + " > " + origin + " " + suggestedReturnDate.format(DATE_FORMATTER),
                "",
                "Ou informe apenas a data da volta:",
                suggestedReturnDate.format(DATE_FORMATTER));
    }

    private boolean cityMatches(String first, String second) {
        return normalizeText(first).equals(normalizeText(second));
    }




            private WhatsappCommandResult searchTripsForPassenger(
            WhatsappSessionResponse session,
            TripSearchInput input) {

        CountryResolutionResponse countryResolution = countryResolverService.resolveByCities(
                input.origin(),
                input.destination());

        if (countryResolution.needsCountryConfirmation()) {
            String metadata = appendMetadata(
                    buildTripSearchMetadata(input, List.of()),
                    "country_resolution_pending=true");

            updateSessionStep(
                    session,
                    WhatsappConversationStep.PASSENGER_IDENTIFICATION,
                    metadata);

            return allowed(
                    "COUNTRY_CONFIRMATION_REQUIRED",
                    """
                            Não consegui confirmar o país da sua viagem apenas pelas cidades informadas.

                            Origem: %s
                            Destino: %s

                            A viagem é no Brasil ou em Angola?
                            """.formatted(
                            input.origin(),
                            input.destination()).trim());
        }

        String countryMetadata = buildCountryMetadata(countryResolution);

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
            String metadata = appendMetadata(
                    buildTripSearchMetadata(input, options),
                    countryMetadata);

            updateSessionStep(
                    session,
                    WhatsappConversationStep.PASSENGER_IDENTIFICATION,
                    metadata);

            String reply = """
                    %s

                    Não encontrei viagens disponíveis para:

                    Origem: %s
                    Destino: %s
                    Data: %s

                    %s
                    """.formatted(
                    buildCountryDetectedMessage(countryResolution),
                    input.origin(),
                    input.destination(),
                    input.date().format(DATE_FORMATTER),
                    buildNewSearchInstructionCard());

            return allowed("SEARCH_TRIPS", reply.trim());
        }

        String metadata = appendMetadata(
                buildTripSearchMetadata(input, options),
                countryMetadata);

        updateSessionStep(
                session,
                WhatsappConversationStep.CHOOSING_TRIP,
                metadata);

        StringBuilder reply = new StringBuilder();

        reply.append(buildCountryDetectedMessage(countryResolution)).append("\n\n");

        String roundTripIntro = buildRoundTripSearchIntro(input);

        if (!roundTripIntro.isBlank()) {
            reply.append(roundTripIntro).append("\n\n");
        }

        reply.append("🚌 Encontrei viagens disponíveis\n\n");

        for (int i = 0; i < options.size(); i++) {
            Trip trip = options.get(i);
            reply.append(formatTripOption(i + 1, trip)).append("\n\n");
        }

        reply.append("Escolha uma opção respondendo apenas com o número.\n\n");
        reply.append("Exemplo: 1\n\n");
        reply.append(buildNewSearchInstructionCard());

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

        if (isReturnTripSelectionPending(session.getMetadata())) {
            return confirmReturnTripSelectionAndAskPassenger(session, messageText);
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

        if (isRoundTripMetadata(metadata) && !isReturnTripSelected(metadata)) {
            return askReturnTripSelection(session, metadata);
        }

        Optional<Passenger> optionalPassenger = findRealPassengerForWhatsapp(session);

        if (optionalPassenger.isPresent()) {
            Passenger passenger = optionalPassenger.get();

            PassengerDocumentType requiredDocumentType = resolveDocumentTypeFromMetadata(metadata);

            if (passenger == null || passenger.getDocumentType() == null || !passenger.getDocumentType().equals(requiredDocumentType)) {
                String adjustedMetadata = appendMetadata(
                        metadata,
                        "pending_passenger_name=" + passenger.getFullName(),
                        "saved_passenger_id=" + passenger.getId(),
                        "saved_passenger_document_mismatch=true",
                        "required_document_type=" + requiredDocumentType);

                updateSessionStep(
                        session,
                        WhatsappConversationStep.ASKING_DOCUMENT,
                        adjustedMetadata);

                String firstName = extractFirstName(passenger.getFullName());
                String requiredDocumentLabel = documentValidatorService.label(requiredDocumentType);
                String currentDocumentLabel = passenger.getDocumentType() != null
                        ? documentValidatorService.label(passenger.getDocumentType())
                        : "documento";
                String currentMaskedDocument = passenger.getDocumentType() != null
                        ? documentValidatorService.mask(passenger.getDocumentType(), passenger.getDocumentNumber())
                        : (passenger.getDocumentNumber() != null ? passenger.getDocumentNumber() : "***");

                String reply = """
                        Olá %s, encontrei seus dados salvos:

                        Passageiro: %s
                        Documento salvo: %s %s

                        Mas esta viagem exige outro documento:

                        %s

                        Para continuar, informe o %s do passageiro.
                        """.formatted(
                        firstName,
                        passenger.getFullName(),
                        currentDocumentLabel,
                        currentMaskedDocument,
                        buildCountryContextFromMetadata(metadata),
                        requiredDocumentLabel);

                return allowed("ASK_PASSENGER_DOCUMENT", reply.trim());
            }

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

                    %s

                    %s

                    Passageiro: %s
                    %s: %s

                    Essa compra é para esse passageiro?

                    1. Sim, continuar
                    2. Não, comprar para outra pessoa
                    3. Alterar meus dados
                    """.formatted(
                    firstName,
                    buildCountryContextFromMetadata(metadata),
                    buildSelectedReturnTripSummary(metadata),
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

                        %s

                        %s

                        Qual é o nome completo do passageiro?
                        """.formatted(
                        buildCountryContextFromMetadata(metadata),
                        buildSelectedReturnTripSummary(metadata)).trim());
    }





        private WhatsappCommandResult askReturnTripSelection(
            WhatsappSessionResponse session,
            String metadata) {
        String origin = extractMetadataValue(metadata, "origin");
        String destination = extractMetadataValue(metadata, "destination");
        String returnDateText = extractMetadataValue(metadata, "return_date");

        if (origin == null || destination == null || returnDateText == null) {
            updateSessionStep(
                    session,
                    WhatsappConversationStep.ASKING_FULL_NAME,
                    metadata);

            return allowed(
                    "ASK_PASSENGER_NAME",
                    "Não consegui carregar os dados da volta. Vamos continuar com a ida.\n\nQual é o nome completo do passageiro?");
        }

        LocalDate returnDate;

        try {
            returnDate = LocalDate.parse(returnDateText, DATE_FORMATTER);
        } catch (DateTimeParseException exception) {
            updateSessionStep(
                    session,
                    WhatsappConversationStep.ASKING_FULL_NAME,
                    metadata);

            return allowed(
                    "ASK_PASSENGER_NAME",
                    "A data de volta ficou inválida na sessão. Vamos continuar com a ida.\n\nQual é o nome completo do passageiro?");
        }

        LocalDateTime startDateTime = returnDate.atStartOfDay();
        LocalDateTime endDateTime = returnDate.plusDays(1).atStartOfDay();

        List<Trip> returnTrips = tripRepository.searchAvailableTrips(
                destination,
                origin,
                startDateTime,
                endDateTime,
                TripStatus.SCHEDULED);

        List<Trip> options = returnTrips.stream()
                .sorted(Comparator.comparing(Trip::getDepartureAt))
                .limit(5)
                .toList();

        if (options.isEmpty()) {
            String adjustedMetadata = appendMetadata(
                    metadata,
                    "return_trip_selection_pending=false",
                    "return_trip_available=false");

            updateSessionStep(
                    session,
                    WhatsappConversationStep.ASKING_FULL_NAME,
                    adjustedMetadata);

            return allowed(
                    "ASK_PASSENGER_NAME",
                    """
                            🔁 Ida e volta identificada.

                            Não encontrei ônibus disponíveis para a volta:

                            Origem: %s
                            Destino: %s
                            Data: %s

                            Vamos continuar com a reserva da ida.

                            Qual é o nome completo do passageiro?
                            """.formatted(
                            destination,
                            origin,
                            returnDate.format(DATE_FORMATTER)).trim());
        }

        String returnMetadata = buildReturnTripOptionsMetadata(metadata, options);

        updateSessionStep(
                session,
                WhatsappConversationStep.CHOOSING_TRIP,
                returnMetadata);

        StringBuilder reply = new StringBuilder();

        reply.append("🔁 Agora escolha a viagem de volta\n\n");
        reply.append("Origem: ").append(destination).append("\n");
        reply.append("Destino: ").append(origin).append("\n");
        reply.append("Data: ").append(returnDate.format(DATE_FORMATTER)).append("\n\n");

        for (int i = 0; i < options.size(); i++) {
            Trip trip = options.get(i);
            reply.append(formatTripOption(i + 1, trip)).append("\n\n");
        }

        reply.append("Escolha a volta respondendo apenas com o número.\n\n");
        reply.append("Exemplo: 1");

        return allowed("CHOOSE_RETURN_TRIP", reply.toString().trim());
    }

        private WhatsappCommandResult confirmReturnTripSelectionAndAskPassenger(
            WhatsappSessionResponse session,
            String messageText) {
        Integer optionNumber = extractSelectedOptionNumber(messageText);

        if (optionNumber == null || optionNumber < 1) {
            return allowed(
                    "CHOOSE_RETURN_TRIP",
                    "Não consegui identificar a viagem de volta escolhida.\n\nResponda apenas com o número. Exemplo: 1");
        }

        UUID returnTripId = extractReturnTripIdFromMetadata(session.getMetadata(), optionNumber);

        if (returnTripId == null) {
            return allowed(
                    "CHOOSE_RETURN_TRIP",
                    "Não encontrei esta opção de volta na sessão atual. Faça uma nova busca de ida e volta.");
        }

        Trip returnTrip = tripRepository.findById(returnTripId)
                .orElseThrow(() -> new IllegalArgumentException("Viagem de volta não encontrada."));

        String metadata = appendMetadata(
                session.getMetadata(),
                "return_selected_option=" + optionNumber,
                "return_selected_trip_id=" + returnTrip.getId(),
                "return_trip_selection_pending=false",
                "return_trip_selected=true");

        Optional<Passenger> optionalPassenger = findRealPassengerForWhatsapp(session);

        if (optionalPassenger.isPresent()) {
            Passenger passenger = optionalPassenger.get();
            PassengerDocumentType requiredDocumentType = resolveDocumentTypeFromMetadata(metadata);

            if (passenger.getDocumentType() != null
                    && passenger.getDocumentType().equals(requiredDocumentType)) {
                updateSessionStep(
                        session,
                        WhatsappConversationStep.CONFIRMING_SAVED_PASSENGER,
                        metadata);

                String reply = """
                        ✅ Viagem de volta escolhida.

                        %s

                        Encontrei seus dados salvos:

                        Passageiro: %s
                        Documento: %s

                        Essa compra ida e volta é para esse passageiro?

                        1️⃣ Sim, continuar
                        2️⃣ Não, comprar para outra pessoa
                        3️⃣ Alterar meus dados
                        """.formatted(
                        buildSelectedReturnTripSummary(metadata),
                        passenger.getFullName(),
                        passenger.getDocumentNumber());

                return allowed("CONFIRM_PASSENGER", reply.trim());
            }
        }

        updateSessionStep(
                session,
                WhatsappConversationStep.ASKING_FULL_NAME,
                metadata);

        String reply = """
                ✅ Viagem de volta escolhida.

                %s

                Agora vamos finalizar a compra ida e volta.

                Qual é o nome completo do passageiro?
                """.formatted(
                buildSelectedReturnTripSummary(metadata)).trim();

        return allowed("ASK_PASSENGER_NAME", reply);
    }


    private boolean isRoundTripMetadata(String metadata) {
        String tripType = extractMetadataValue(metadata, "trip_type");
        return "ROUND_TRIP".equalsIgnoreCase(tripType);
    }

            private boolean isReturnTripSelectionPending(String metadata) {
        String pending = extractMetadataValue(metadata, "return_trip_selection_pending");

        if ("true".equalsIgnoreCase(pending)) {
            return true;
        }

        String returnTripSelected = extractMetadataValue(metadata, "return_trip_selected");
        String returnTripId = extractMetadataValue(metadata, "return_selected_trip_id");
        String firstReturnOption = extractMetadataValue(metadata, "return_option_1");

        boolean hasReturnOptions = firstReturnOption != null && !firstReturnOption.isBlank();
        boolean alreadySelected = "true".equalsIgnoreCase(returnTripSelected)
                || (returnTripId != null && !returnTripId.isBlank());

        return hasReturnOptions && !alreadySelected;
    }



    private boolean isReturnTripSelected(String metadata) {
        String selected = extractMetadataValue(metadata, "return_trip_selected");
        String returnTripId = extractMetadataValue(metadata, "return_selected_trip_id");

        return "true".equalsIgnoreCase(selected)
                || (returnTripId != null && !returnTripId.isBlank());
    }

    private String buildReturnTripOptionsMetadata(
            String metadata,
            List<Trip> options) {
        StringBuilder builder = new StringBuilder();

        if (metadata != null && !metadata.isBlank()) {
            builder.append(metadata.trim()).append("\n");
        }

        builder.append("return_trip_selection_pending=true\n");

        for (int i = 0; i < options.size(); i++) {
            builder
                    .append("return_option_")
                    .append(i + 1)
                    .append("=")
                    .append(options.get(i).getId())
                    .append("\n");
        }

        return builder.toString().trim();
    }

    private UUID extractReturnTripIdFromMetadata(String metadata, int optionNumber) {
        if (metadata == null || metadata.isBlank()) {
            return null;
        }

        String key = "return_option_" + optionNumber + "=";

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

        private String buildSelectedReturnTripSummary(String metadata) {
        String returnTripId = extractMetadataValue(metadata, "return_selected_trip_id");

        if (returnTripId == null || returnTripId.isBlank()) {
            return "";
        }

        try {
            Trip trip = tripRepository.findById(UUID.fromString(returnTripId)).orElse(null);

            if (trip == null) {
                return "";
            }

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

            String price = trip.getPrice() != null ? trip.getPrice().toPlainString() : "0.00";
            String currency = trip.getCurrency() != null ? trip.getCurrency() : "-";

            return """
                    🔁 Volta escolhida
                    🚌 Empresa: %s
                    📍 Trecho: %s → %s
                    🕒 Saída: %s
                    💰 Valor: %s %s
                    """.formatted(
                    companyName,
                    origin,
                    destination,
                    departure,
                    currency,
                    price).trim();

        } catch (Exception exception) {
            return "";
        }
    }

    private WhatsappCommandResult askPaymentMethodFromWhatsapp(
            WhatsappSessionResponse session,
            String messageText) {
        String bookingCode = extractBookingCode(messageText);

        if (bookingCode == null || bookingCode.isBlank()) {
            bookingCode = extractMetadataValue(session.getMetadata(), "booking_code");
        }

        if (bookingCode == null || bookingCode.isBlank()) {
            return allowed(
                    "PAYMENT_METHOD",
                    """
                            Não encontrei uma reserva pendente na sua sessão.

                            Envie o código da reserva no formato:
                            Pagar reserva VR123456
                            """.trim());
        }

        try {
            Booking booking = bookingRepository.findByBookingCode(bookingCode)
                    .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada."));

            if (!BookingStatus.PENDING_PAYMENT.equals(booking.getStatus())) {
                return payBookingFromWhatsapp(session, messageText);
            }

            String metadata = appendMetadata(
                    session.getMetadata(),
                    "booking_code=" + booking.getBookingCode(),
                    "payment_method_selection_pending=true");

            updateSessionStep(
                    session,
                    WhatsappConversationStep.WAITING_PAYMENT,
                    metadata);

            String reply = """
                    💳 Escolha a forma de pagamento

                    %s

                    🎫 Reserva: %s
                    📍 Trecho: %s → %s
                    💰 Valor: %s %s

                    %s
                    """.formatted(
                    buildCountryContextFromMetadata(metadata),
                    booking.getBookingCode(),
                    resolveBookingOriginCity(booking),
                    resolveBookingDestinationCity(booking),
                    booking.getCurrency(),
                    booking.getAmount() != null ? booking.getAmount().toPlainString() : "0.00",
                    buildPaymentMethodOptionsCard(metadata, booking));

            return allowed("PAYMENT_METHOD", reply.trim());

        } catch (Exception exception) {
            return allowed(
                    "PAYMENT_METHOD",
                    "Não foi possível carregar as formas de pagamento agora.\n\nMotivo: "
                            + exception.getMessage());
        }
    }

    private WhatsappCommandResult payBookingWithSelectedPaymentMethod(
            WhatsappSessionResponse session,
            String normalizedMessage) {
        String bookingCode = extractMetadataValue(session.getMetadata(), "booking_code");

        if (bookingCode == null || bookingCode.isBlank()) {
            return allowed(
                    "PAY_BOOKING",
                    "Não encontrei a reserva na sessão atual. Faça uma nova busca ou envie o código da reserva.");
        }

        try {
            Booking booking = bookingRepository.findByBookingCode(bookingCode)
                    .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada."));

            PaymentMethod selectedMethod = resolveSelectedPaymentMethod(
                    session.getMetadata(),
                    booking,
                    normalizedMessage);

            if (selectedMethod == null) {
                return allowed(
                        "PAYMENT_METHOD",
                        """
                                Opção de pagamento inválida.

                                %s
                                """.formatted(
                                buildPaymentMethodOptionsCard(session.getMetadata(), booking)).trim());
            }

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

            if (isAngolaPaymentContext(session.getMetadata(), booking)
                    && PaymentMethod.CASH.equals(selectedMethod)) {
                return confirmAngolaCashReservationAtCounter(session, booking);
            }

            PaymentRequest paymentRequest = new PaymentRequest()
                    .setBookingId(booking.getId())
                    .setMethod(selectedMethod);

            PaymentResponse createdPayment = paymentService.create(paymentRequest);
            PaymentResponse confirmedPayment = paymentService.confirm(createdPayment.getId());
            PaymentResponse confirmedReturnPayment = payReturnBookingIfNeeded(session.getMetadata(), selectedMethod);

            String metadata = appendMetadata(
                    session.getMetadata(),
                    "payment_method_selection_pending=false",
                    "payment_id=" + confirmedPayment.getId(),
                    "payment_code=" + confirmedPayment.getPaymentCode(),
                    "payment_method=" + confirmedPayment.getMethod(),
                    "payment_method_label=" + paymentMethodLabel(confirmedPayment.getMethod()),
                    "payment_status=" + confirmedPayment.getStatus(),
                    "booking_status=" + confirmedPayment.getBookingStatus(),
                    buildReturnPaymentMetadata(confirmedReturnPayment));

            updateSessionStep(
                    session,
                    WhatsappConversationStep.CONFIRMING_BOOKING,
                    metadata);

            String reply = """
                    💳 Pagamento confirmado

                    %s

                    🎫 Reserva: %s
                    💳 Método: %s
                    💰 Valor: %s %s
                    ✅ Status: %s
                    📍 Trecho: %s → %s
                    🕒 Saída: %s
                    💺 Poltrona: %d

                    Escolha uma opção:
                    1️⃣ Emitir bilhete agora
                    2️⃣ Fazer nova busca
                    """.formatted(
                    buildCountryContextFromMetadata(metadata),
                    confirmedPayment.getBookingCode(),
                    paymentMethodLabel(confirmedPayment.getMethod()),
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

    private WhatsappCommandResult confirmAngolaCashReservationAtCounter(
            WhatsappSessionResponse session,
            Booking booking) {
        String metadata = appendMetadata(
                session.getMetadata(),
                "payment_method_selection_pending=false",
                "payment_method=CASH",
                "payment_method_label=Dinheiro",
                "counter_payment_pending=true",
                "booking_code=" + booking.getBookingCode(),
                "booking_status=" + booking.getStatus());

        updateSessionStep(
                session,
                WhatsappConversationStep.START,
                metadata);

        String reply = """
                ✅ Reserva confirmada.

                %s

                🎫 Reserva: %s
                📍 Trecho: %s → %s
                🕒 Saída: %s
                💺 Poltrona: %s
                💰 Valor: %s %s
                💳 Pagamento: Dinheiro no guichê

                Para esta opção de pagamento, apresente-se 30 minutos antes no guichê da empresa para efetuar o pagamento e validar seu bilhete.

                Após o pagamento no guichê, a empresa poderá confirmar a reserva e emitir/validar o bilhete.

                Boa viagem. 🚌
                """.formatted(
                buildCountryContextFromMetadata(metadata),
                booking.getBookingCode(),
                resolveBookingOriginCity(booking),
                resolveBookingDestinationCity(booking),
                booking.getTrip() != null && booking.getTrip().getDepartureAt() != null
                        ? booking.getTrip().getDepartureAt().format(DATE_TIME_FORMATTER)
                        : "-",
                booking.getSeatNumber() != null ? booking.getSeatNumber().toString() : "-",
                booking.getCurrency() != null ? booking.getCurrency() : "AOA",
                booking.getAmount() != null ? booking.getAmount().toPlainString() : "0.00");

        return allowed("COUNTER_PAYMENT_RESERVATION", reply.trim());
    }
    private String resolveBookingOriginCity(Booking booking) {
        if (booking == null
                || booking.getTrip() == null
                || booking.getTrip().getRoute() == null) {
            return "-";
        }

        return booking.getTrip().getRoute().getOriginCity();
    }

    private String resolveBookingDestinationCity(Booking booking) {
        if (booking == null
                || booking.getTrip() == null
                || booking.getTrip().getRoute() == null) {
            return "-";
        }

        return booking.getTrip().getRoute().getDestinationCity();
    }
    private boolean isPaymentMethodSelectionPending(String metadata) {
        String pending = extractMetadataValue(metadata, "payment_method_selection_pending");
        return "true".equalsIgnoreCase(pending);
    }
    private boolean isPaymentMethodOption(String normalizedMessage) {
        Integer optionNumber = extractSimpleOptionNumber(normalizedMessage);
        return optionNumber != null && optionNumber >= 1 && optionNumber <= 4;
    }


    private PaymentMethod resolveSelectedPaymentMethod(
            String metadata,
            Booking booking,
            String normalizedMessage) {
        boolean angola = isAngolaPaymentContext(metadata, booking);
        Integer optionNumber = extractSimpleOptionNumber(normalizedMessage);
        String option = optionNumber == null ? "" : optionNumber.toString();

        if (angola) {
            return switch (option) {
                case "1" -> PaymentMethod.MULTICAIXA_EXPRESS;
                case "2" -> PaymentMethod.UNITEL_MONEY;
                case "3" -> PaymentMethod.AFRIMONEY;
                case "4" -> PaymentMethod.CASH;
                default -> null;
            };
        }

        return switch (option) {
            case "1" -> PaymentMethod.PIX;
            case "2" -> PaymentMethod.CASH;
            default -> null;
        };
    }

    private String buildPaymentMethodOptionsCard(
            String metadata,
            Booking booking) {
        if (isAngolaPaymentContext(metadata, booking)) {
            return """
                    Escolha uma opção:

                    1️⃣ Multicaixa Express
                    2️⃣ Unitel Money
                    3️⃣ Afrimoney
                    4️⃣ Dinheiro
                    """.trim();
        }

        return """
                Escolha uma opção:

                1️⃣ Pix
                2️⃣ Dinheiro
                """.trim();
    }

    private boolean isAngolaPaymentContext(
            String metadata,
            Booking booking) {
        String country = extractMetadataValue(metadata, "country");

        if ("AO".equalsIgnoreCase(country)) {
            return true;
        }

        return booking != null && "AOA".equalsIgnoreCase(booking.getCurrency());
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

            PaymentMethod paymentMethod = resolvePaymentMethodForBooking(
                    session.getMetadata(),
                    booking);

            PaymentRequest paymentRequest = new PaymentRequest()
                    .setBookingId(booking.getId())
                    .setMethod(paymentMethod);

            PaymentResponse createdPayment = paymentService.create(paymentRequest);
            PaymentResponse confirmedPayment = paymentService.confirm(createdPayment.getId());
            PaymentResponse confirmedReturnPayment = payReturnBookingIfNeeded(session.getMetadata(), paymentMethod);

            String metadata = appendMetadata(
                    session.getMetadata(),
                    "payment_id=" + confirmedPayment.getId(),
                    "payment_code=" + confirmedPayment.getPaymentCode(),
                    "payment_method=" + confirmedPayment.getMethod(),
                    "payment_method_label=" + paymentMethodLabel(confirmedPayment.getMethod()),
                    "payment_status=" + confirmedPayment.getStatus(),
                    "booking_status=" + confirmedPayment.getBookingStatus(),
                    buildReturnPaymentMetadata(confirmedReturnPayment));

            updateSessionStep(
                    session,
                    WhatsappConversationStep.CONFIRMING_BOOKING,
                    metadata);

            String reply = """
                    💳 Pagamento confirmado

                    %s

                    🎫 Reserva: %s
                    💳 Método: %s
                    💰 Valor: %s %s
                    ✅ Status: %s
                    📍 Trecho: %s → %s
                    🕒 Saída: %s
                    💺 Poltrona: %d

                    Escolha uma opção:
                    1️⃣ Emitir bilhete agora
                    2️⃣ Fazer nova busca
                    """.formatted(
                    buildCountryContextFromMetadata(metadata),
                    confirmedPayment.getBookingCode(),
                    paymentMethodLabel(confirmedPayment.getMethod()),
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


    private WhatsappCommandResult recoverPaidTicketFromWhatsapp(WhatsappSessionResponse session) {
        Optional<Passenger> optionalPassenger = findRealPassengerForWhatsapp(session);

        if (optionalPassenger.isEmpty()) {
            return allowed(
                    "RECOVER_TICKET",
                    """
                            Não encontrei seus dados de passageiro neste WhatsApp.

                            Para recuperar ou emitir um bilhete pago, envie o código da reserva:

                            Emitir bilhete VR123456
                            """.trim());
        }

        Passenger passenger = optionalPassenger.get();

        List<Booking> paidBookings = bookingRepository.findByPassenger_IdAndStatusOrderByCreatedAtDesc(
                passenger.getId(),
                BookingStatus.PAID);

        if (paidBookings.isEmpty()) {
            return allowed(
                    "RECOVER_TICKET",
                    """
                            Não encontrei reserva paga aguardando emissão para este WhatsApp.

                            Se você tiver o código da reserva, envie:

                            Emitir bilhete VR123456
                            """.trim());
        }

        if (paidBookings.size() == 1) {
            Booking booking = paidBookings.get(0);

            String metadata = appendMetadata(
                    session.getMetadata(),
                    "booking_id=" + booking.getId(),
                    "booking_code=" + booking.getBookingCode());

            updateSessionStep(
                    session,
                    WhatsappConversationStep.CONFIRMING_BOOKING,
                    metadata);

            return issueTicketFromWhatsapp(session, "Emitir bilhete " + booking.getBookingCode());
        }

        List<Booking> options = paidBookings.stream()
                .limit(5)
                .toList();

        String metadata = buildPaidBookingSelectionMetadata(session.getMetadata(), options);

        updateSessionStep(
                session,
                WhatsappConversationStep.CONFIRMING_BOOKING,
                metadata);

        StringBuilder reply = new StringBuilder();

        reply.append("🎫 Encontrei reservas pagas aguardando emissão.\n\n");
        reply.append("Escolha qual bilhete deseja emitir:\n\n");

        for (int i = 0; i < options.size(); i++) {
            Booking booking = options.get(i);
            reply.append(formatPaidBookingOption(i + 1, booking)).append("\n\n");
        }

        reply.append("Responda apenas com o número.\n");
        reply.append("Exemplo: 1");

        return allowed("RECOVER_TICKET", reply.toString().trim());
    }

    private WhatsappCommandResult issueTicketFromPaidBookingSelection(
            WhatsappSessionResponse session,
            String normalizedMessage) {
        Integer optionNumber = extractSimpleOptionNumber(normalizedMessage);

        if (optionNumber == null || optionNumber < 1) {
            return allowed(
                    "RECOVER_TICKET",
                    "Não consegui identificar a opção. Responda apenas com o número. Exemplo: 1");
        }

        String bookingCode = extractMetadataValue(session.getMetadata(), "paid_booking_" + optionNumber + "_code");

        if (bookingCode == null || bookingCode.isBlank()) {
            return allowed(
                    "RECOVER_TICKET",
                    "Não encontrei esta opção na sua sessão atual. Envie novamente: Reemitir bilhete");
        }

        return issueTicketFromWhatsapp(session, "Emitir bilhete " + bookingCode);
    }

    private boolean isRecoverTicketCommand(String normalizedMessage) {
        return containsAny(
                normalizedMessage,
                "reemitir bilhete",
                "reimprimir bilhete",
                "recuperar bilhete",
                "reenviar bilhete",
                "segunda via",
                "2 via",
                "2ª via",
                "meu bilhete",
                "meu ticket",
                "emitir bilhete",
                "emitir ticket");
    }

    private boolean isPaidBookingSelectionPending(String metadata) {
        String pending = extractMetadataValue(metadata, "paid_booking_selection_pending");
        return "true".equalsIgnoreCase(pending);
    }

    private boolean isIssueTicketSelection(String normalizedMessage) {
        Integer optionNumber = extractSimpleOptionNumber(normalizedMessage);
        return optionNumber != null && optionNumber >= 1 && optionNumber <= 5;
    }
    private Integer extractSimpleOptionNumber(String normalizedMessage) {
        if (normalizedMessage == null || normalizedMessage.isBlank()) {
            return null;
        }

        String cleaned = normalizeText(normalizedMessage)
                .replaceAll("[^0-9a-z ]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (cleaned.isBlank()) {
            return null;
        }

        Matcher matcher = Pattern.compile("(\\d+)").matcher(cleaned);

        if (!matcher.find()) {
            return null;
        }

        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException exception) {
            return null;
        }
    }


    private String buildPaidBookingSelectionMetadata(
            String currentMetadata,
            List<Booking> bookings) {
        List<String> entries = new ArrayList<>();

        if (currentMetadata != null && !currentMetadata.isBlank()) {
            entries.add(currentMetadata);
        }

        entries.add("paid_booking_selection_pending=true");

        for (int i = 0; i < bookings.size(); i++) {
            Booking booking = bookings.get(i);
            int option = i + 1;

            entries.add("paid_booking_" + option + "_id=" + booking.getId());
            entries.add("paid_booking_" + option + "_code=" + booking.getBookingCode());
        }

        return String.join(";", entries);
    }

    private String formatPaidBookingOption(int option, Booking booking) {
        String origin = "-";
        String destination = "-";
        String departure = "-";
        String currency = booking.getCurrency() != null ? booking.getCurrency() : "-";
        String amount = booking.getAmount() != null ? booking.getAmount().toPlainString() : "0.00";

        if (booking.getTrip() != null && booking.getTrip().getRoute() != null) {
            origin = booking.getTrip().getRoute().getOriginCity();
            destination = booking.getTrip().getRoute().getDestinationCity();

            if (booking.getTrip().getDepartureAt() != null) {
                departure = booking.getTrip().getDepartureAt().format(DATE_TIME_FORMATTER);
            }
        }

        return """
                %d️⃣ Reserva: %s
                📍 Trecho: %s → %s
                🕒 Saída: %s
                💺 Poltrona: %s
                💰 Valor: %s %s
                """.formatted(
                option,
                booking.getBookingCode(),
                origin,
                destination,
                departure,
                booking.getSeatNumber() != null ? booking.getSeatNumber().toString() : "-",
                currency,
                amount).trim();
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

                    TicketResponse returnTicket = issueReturnTicketIfNeeded(session.getMetadata());

                    return allowed(
                            "ISSUE_TICKET",
                            returnTicket != null
                                    ? formatRoundTripTicketsIssuedReply(existingTicket, returnTicket, true)
                                    : formatTicketIssuedReply(existingTicket, true));
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
            TicketResponse returnTicket = issueReturnTicketIfNeeded(session.getMetadata());

            String metadata = appendMetadata(
                    session.getMetadata(),
                    "ticket_id=" + ticket.getId(),
                    "ticket_code=" + ticket.getTicketCode(),
                    "ticket_status=" + ticket.getStatus(),
                    "ticket_pdf_url=" + buildTicketPdfUrl(ticket.getId()),
                    buildReturnTicketMetadata(returnTicket));

            updateSessionStep(
                    session,
                    WhatsappConversationStep.TICKET_ISSUED,
                    metadata);

            return allowed(
                    "ISSUE_TICKET",
                    returnTicket != null
                            ? formatRoundTripTicketsIssuedReply(ticket, returnTicket, false)
                            : formatTicketIssuedReply(ticket, false));

        } catch (Exception exception) {
            return allowed(
                    "ISSUE_TICKET",
                    "Não foi possível emitir o bilhete agora.\n\nMotivo: "
                            + exception.getMessage()
                            + "\n\nTente novamente ou fale com o suporte.");
        }
    }


    private BookingResponse createReturnBookingIfNeeded(
            WhatsappSessionResponse session,
            Passenger passenger) {
        String metadata = session.getMetadata();

        String returnTripIdText = extractMetadataValue(metadata, "return_selected_trip_id");
        String existingReturnBookingCode = extractMetadataValue(metadata, "return_booking_code");

        if (existingReturnBookingCode != null && !existingReturnBookingCode.isBlank()) {
            return null;
        }

        if (returnTripIdText == null || returnTripIdText.isBlank()) {
            return null;
        }

        try {
            UUID returnTripId = UUID.fromString(returnTripIdText);

            Trip returnTrip = tripRepository.findById(returnTripId)
                    .orElseThrow(() -> new IllegalArgumentException("Viagem de volta não encontrada."));

            Integer returnSeatNumber = findFirstAvailableSeat(returnTrip);

            BookingRequest returnRequest = new BookingRequest()
                    .setTripId(returnTrip.getId())
                    .setPassengerId(passenger.getId())
                    .setSeatNumber(returnSeatNumber)
                    .setPassengerFareType(resolvePassengerFareTypeFromMetadata(metadata));

            return bookingService.create(returnRequest);

        } catch (Exception exception) {
            throw new IllegalArgumentException("Não foi possível criar a reserva da volta: " + exception.getMessage());
        }
    }

    private String buildReturnBookingMetadata(BookingResponse returnBooking) {
        if (returnBooking == null) {
            return "";
        }

        return String.join("\n",
                "return_booking_id=" + returnBooking.getId(),
                "return_booking_code=" + returnBooking.getBookingCode(),
                "return_seat_number=" + returnBooking.getSeatNumber(),
                "return_booking_status=" + returnBooking.getStatus(),
                "return_currency=" + returnBooking.getCurrency());
    }

    private PaymentResponse payReturnBookingIfNeeded(
            String metadata,
            PaymentMethod paymentMethod) {
        String returnBookingCode = extractMetadataValue(metadata, "return_booking_code");

        if (returnBookingCode == null || returnBookingCode.isBlank()) {
            return null;
        }

        Booking returnBooking = bookingRepository.findByBookingCode(returnBookingCode).orElse(null);

        if (returnBooking == null) {
            return null;
        }

        if (BookingStatus.PAID.equals(returnBooking.getStatus())
                || BookingStatus.TICKET_ISSUED.equals(returnBooking.getStatus())) {
            return null;
        }

        if (!BookingStatus.PENDING_PAYMENT.equals(returnBooking.getStatus())) {
            return null;
        }

        PaymentRequest returnPaymentRequest = new PaymentRequest()
                .setBookingId(returnBooking.getId())
                .setMethod(paymentMethod);

        PaymentResponse createdReturnPayment = paymentService.create(returnPaymentRequest);
        return paymentService.confirm(createdReturnPayment.getId());
    }

    private String buildReturnPaymentMetadata(PaymentResponse returnPayment) {
        if (returnPayment == null) {
            return "";
        }

        return String.join("\n",
                "return_payment_id=" + returnPayment.getId(),
                "return_payment_code=" + returnPayment.getPaymentCode(),
                "return_payment_method=" + returnPayment.getMethod(),
                "return_payment_status=" + returnPayment.getStatus(),
                "return_booking_status=" + returnPayment.getBookingStatus());
    }

    private TicketResponse issueReturnTicketIfNeeded(String metadata) {
        String returnBookingCode = extractMetadataValue(metadata, "return_booking_code");

        if (returnBookingCode == null || returnBookingCode.isBlank()) {
            return null;
        }

        Booking returnBooking = bookingRepository.findByBookingCode(returnBookingCode).orElse(null);

        if (returnBooking == null) {
            return null;
        }

        if (BookingStatus.TICKET_ISSUED.equals(returnBooking.getStatus())) {
            return ticketRepository.findByBooking_Id(returnBooking.getId())
                    .map(ticket -> ticketService.findById(ticket.getId()))
                    .orElse(null);
        }

        if (!BookingStatus.PAID.equals(returnBooking.getStatus())) {
            return null;
        }

        TicketRequest returnTicketRequest = new TicketRequest()
                .setBookingId(returnBooking.getId());

        return ticketService.issue(returnTicketRequest);
    }

    private String buildReturnTicketMetadata(TicketResponse returnTicket) {
        if (returnTicket == null) {
            return "";
        }

        return String.join("\n",
                "return_ticket_id=" + returnTicket.getId(),
                "return_ticket_code=" + returnTicket.getTicketCode(),
                "return_ticket_status=" + returnTicket.getStatus(),
                "return_ticket_pdf_url=" + buildTicketPdfUrl(returnTicket.getId()));
    }

    private String formatRoundTripTicketsIssuedReply(
            TicketResponse outboundTicket,
            TicketResponse returnTicket,
            boolean alreadyIssued) {
        String title = alreadyIssued
                ? "🎫 Bilhetes já emitidos para esta compra ida e volta."
                : "🎫 Bilhetes emitidos com sucesso para ida e volta.";

        return """
                %s

                🚌 BILHETE DE IDA
                🎫 Código: %s
                📄 Reserva: %s
                🧍 Passageiro: %s
                📍 Trecho: %s → %s
                🕒 Saída: %s
                💺 Poltrona: %d
                💰 Valor: %s %s
                📄 PDF:
                %s

                🔁 BILHETE DE VOLTA
                🎫 Código: %s
                📄 Reserva: %s
                🧍 Passageiro: %s
                📍 Trecho: %s → %s
                🕒 Saída: %s
                💺 Poltrona: %d
                💰 Valor: %s %s
                📄 PDF:
                %s

                Apresente o bilhete correspondente em cada embarque.
                """.formatted(
                title,
                outboundTicket.getTicketCode(),
                outboundTicket.getBookingCode(),
                outboundTicket.getPassengerName(),
                outboundTicket.getOriginCity(),
                outboundTicket.getDestinationCity(),
                outboundTicket.getDepartureAt() != null
                        ? outboundTicket.getDepartureAt().format(DATE_TIME_FORMATTER)
                        : "-",
                outboundTicket.getSeatNumber(),
                outboundTicket.getCurrency(),
                outboundTicket.getAmount() != null ? outboundTicket.getAmount().toPlainString() : "0.00",
                buildTicketPdfUrl(outboundTicket.getId()),
                returnTicket.getTicketCode(),
                returnTicket.getBookingCode(),
                returnTicket.getPassengerName(),
                returnTicket.getOriginCity(),
                returnTicket.getDestinationCity(),
                returnTicket.getDepartureAt() != null
                        ? returnTicket.getDepartureAt().format(DATE_TIME_FORMATTER)
                        : "-",
                returnTicket.getSeatNumber(),
                returnTicket.getCurrency(),
                returnTicket.getAmount() != null ? returnTicket.getAmount().toPlainString() : "0.00",
                buildTicketPdfUrl(returnTicket.getId())).trim();
    }

    private PaymentMethod resolvePaymentMethodForBooking(
            String metadata,
            Booking booking) {
        String rawMethod = extractMetadataValue(metadata, "payment_method");

        if (rawMethod != null && !rawMethod.isBlank()) {
            try {
                return PaymentMethod.valueOf(rawMethod.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // Continua para resolver pelo país/moeda.
            }
        }

        String country = extractMetadataValue(metadata, "country");

        if ("AO".equalsIgnoreCase(country)) {
            return PaymentMethod.MULTICAIXA_EXPRESS;
        }

        if ("BR".equalsIgnoreCase(country)) {
            return PaymentMethod.PIX;
        }

        String currency = booking != null ? booking.getCurrency() : null;

        if ("AOA".equalsIgnoreCase(currency)) {
            return PaymentMethod.MULTICAIXA_EXPRESS;
        }

        if ("BRL".equalsIgnoreCase(currency)) {
            return PaymentMethod.PIX;
        }

        return PaymentMethod.PIX;
    }

    private String paymentMethodLabel(PaymentMethod method) {
        if (method == null) {
            return "-";
        }

        return switch (method) {
            case PIX -> "Pix";
            case CREDIT_CARD -> "Cartão de crédito";
            case DEBIT_CARD -> "Cartão de débito";
            case CASH -> "Dinheiro";
            case BANK_TRANSFER -> "Transferência bancária";
            case MULTICAIXA_EXPRESS -> "Multicaixa Express";
            case UNITEL_MONEY -> "Unitel Money";
            case AFRIMONEY -> "Afrimoney";
        };
    }
        private String formatTicketIssuedReply(TicketResponse ticket, boolean alreadyIssued) {
        String title = alreadyIssued
                ? "🎫 Bilhete já emitido para esta reserva."
                : "🎫 Bilhete emitido com sucesso.";

        String companyName = resolveTicketCompanyName(ticket);
        String countryCard = buildCountryContextFromTicket(ticket);
        String paymentMethod = extractMetadataValueFromCurrentSessionFallback("payment_method_label");

        if (paymentMethod == null || paymentMethod.isBlank()) {
            paymentMethod = extractMetadataValueFromCurrentSessionFallback("payment_method");
        }

        if (paymentMethod == null || paymentMethod.isBlank()) {
            paymentMethod = "-";
        }

        return """
                %s

                %s

                🚌 Empresa: %s
                🎫 Código do bilhete: %s
                📄 Reserva: %s
                ✅ Status do bilhete: %s

                🧍 Passageiro: %s
                📍 Trecho: %s → %s
                🕒 Saída: %s
                🕘 Chegada: %s
                💺 Poltrona: %d
                💳 Pagamento: %s
                💰 Valor: %s %s

                🔎 Validação:
                %s

                📄 PDF:
                %s

                Apresente este bilhete no momento do embarque.
                """.formatted(
                title,
                countryCard,
                companyName,
                ticket.getTicketCode(),
                ticket.getBookingCode(),
                ticket.getStatus(),
                ticket.getPassengerName(),
                ticket.getOriginCity(),
                ticket.getDestinationCity(),
                ticket.getDepartureAt() != null
                        ? ticket.getDepartureAt().format(DATE_TIME_FORMATTER)
                        : "-",
                ticket.getArrivalAt() != null
                        ? ticket.getArrivalAt().format(DATE_TIME_FORMATTER)
                        : "-",
                ticket.getSeatNumber(),
                paymentMethod,
                ticket.getCurrency(),
                ticket.getAmount() != null ? ticket.getAmount().toPlainString() : "0.00",
                ticket.getValidationUrl(),
                buildTicketPdfUrl(ticket.getId())).trim();
    }


    private String resolveTicketCompanyName(TicketResponse ticket) {
        if (ticket == null) {
            return "-";
        }

        if (ticket.getCompanyTradeName() != null && !ticket.getCompanyTradeName().isBlank()) {
            return ticket.getCompanyTradeName();
        }

        if (ticket.getCompanyName() != null && !ticket.getCompanyName().isBlank()) {
            return ticket.getCompanyName();
        }

        return "-";
    }

    private String buildCountryContextFromTicket(TicketResponse ticket) {
        if (ticket == null) {
            return buildSupportedCountriesCard();
        }

        if ("AOA".equalsIgnoreCase(ticket.getCurrency())) {
            return """
                    🇦🇴 Viagem identificada: Angola
                    Documento: BI ou Passaporte
                    Moeda: AOA
                    Pagamento: Multicaixa, Unitel Money, Afrimoney ou dinheiro
                    """.trim();
        }

        if ("BRL".equalsIgnoreCase(ticket.getCurrency())) {
            return """
                    🇧🇷 Viagem identificada: Brasil
                    Documento: CPF ou Passaporte
                    Moeda: BRL
                    Pagamento: Pix ou dinheiro
                    """.trim();
        }

        return buildSupportedCountriesCard();
    }

    private String extractMetadataValueFromCurrentSessionFallback(String key) {
        return null;
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

                        %s

                        %s
                        """.formatted(
                        buildSupportedCountriesCard(),
                        buildTripSearchExamplesCard()).trim());
    }



        
private TripSearchInput parseTripSearch(String messageText) {
        if (messageText == null || messageText.isBlank()) {
            return null;
        }

        LocalDate date = extractSearchDate(messageText);
        LocalDate returnDate = extractReturnDate(messageText);

        if (date == null) {
            return null;
        }

        String origin = extractLabeledValue(messageText, "origem");
        String destination = extractLabeledValue(messageText, "destino");

        if (isFilled(origin) && isFilled(destination)) {
            return new TripSearchInput(
                    cleanSearchTerm(origin),
                    cleanSearchTerm(destination),
                    date,
                    returnDate);
        }

        String[] routeExpression = extractRouteExpression(messageText);

        if (routeExpression != null
                && isFilled(routeExpression[0])
                && isFilled(routeExpression[1])) {
            return new TripSearchInput(
                    routeExpression[0],
                    routeExpression[1],
                    date,
                    returnDate);
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

        return new TripSearchInput(origin, destination, date, returnDate);
    }

    private String[] extractRouteExpression(String messageText) {
        if (messageText == null || messageText.isBlank()) {
            return null;
        }

        String withoutDates = TRIP_SEARCH_DATE_PATTERN
                .matcher(messageText)
                .replaceAll(" ");

        String cleaned = withoutDates
                .replace("→", ">")
                .replace("->", ">")
                .replace("=>", ">")
                .trim();

        String[] parts = cleaned.split("\\s*>\\s*");

        if (parts.length >= 2) {
            String origin = cleanRouteToken(parts[0]);
            String destination = cleanRouteToken(parts[1]);

            if (isFilled(origin) && isFilled(destination)) {
                return new String[] { origin, destination };
            }
        }

        Matcher matcher = Pattern
                .compile("(?iu)^\\s*(.+?)\\s+(?:para|ate|até)\\s+(.+?)\\s*$")
                .matcher(cleaned);

        if (matcher.find()) {
            String origin = cleanRouteToken(matcher.group(1));
            String destination = cleanRouteToken(matcher.group(2));

            if (isFilled(origin) && isFilled(destination)) {
                return new String[] { origin, destination };
            }
        }

        return null;
    }

    private String cleanRouteToken(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replaceFirst("(?iu)^\\s*origem\\s*:?\\s*", "")
                .replaceFirst("(?iu)^\\s*destino\\s*:?\\s*", "")
                .replaceFirst("(?iu)^\\s*ida\\s*:?\\s*", "")
                .replaceFirst("(?iu)^\\s*volta\\s*:?\\s*", "")
                .replaceFirst("(?iu)^\\s*retorno\\s*:?\\s*", "")
                .replaceFirst("(?iu)^\\s*regresso\\s*:?\\s*", "")
                .replace(":", "")
                .replace(";", "")
                .trim();
    }



        private boolean isRoundTripRequest(String normalizedMessage) {
        if (normalizedMessage == null || normalizedMessage.isBlank()) {
            return false;
        }

        String text = normalizedMessage.trim().toLowerCase(Locale.ROOT);

        return text.contains("ida e volta")
                || text.contains("ida volta")
                || text.contains("volta")
                || text.contains("retorno")
                || text.contains("regresso")
                || text.contains("data de volta")
                || text.contains("data volta")
                || text.contains("data retorno")
                || text.contains("data de retorno");
    }


        private LocalDate extractReturnDate(String messageText) {
        if (messageText == null || messageText.isBlank()) {
            return null;
        }

        for (String line : splitLines(messageText)) {
            String normalizedLine = normalizeText(line);

            boolean isReturnLine = normalizedLine.startsWith("volta")
                    || normalizedLine.startsWith("retorno")
                    || normalizedLine.startsWith("regresso")
                    || normalizedLine.startsWith("data de volta")
                    || normalizedLine.startsWith("data volta")
                    || normalizedLine.startsWith("data de retorno")
                    || normalizedLine.startsWith("data retorno")
                    || normalizedLine.contains("volta:")
                    || normalizedLine.contains("retorno:")
                    || normalizedLine.contains("regresso:");

            if (!isReturnLine) {
                continue;
            }

            Matcher matcher = TRIP_SEARCH_DATE_PATTERN.matcher(line);

            if (matcher.find()) {
                String rawDate = matcher.group(1).replace("-", "/");

                try {
                    return LocalDate.parse(rawDate, DATE_FORMATTER);
                } catch (DateTimeParseException exception) {
                    return null;
                }
            }
        }

        String normalizedMessage = normalizeText(messageText);

        if (!isRoundTripRequest(normalizedMessage)) {
            return null;
        }

        Matcher matcher = TRIP_SEARCH_DATE_PATTERN.matcher(messageText);
        List<LocalDate> dates = new ArrayList<>();

        while (matcher.find()) {
            String rawDate = matcher.group(1).replace("-", "/");

            try {
                dates.add(LocalDate.parse(rawDate, DATE_FORMATTER));
            } catch (DateTimeParseException ignored) {
                // Ignora data inválida.
            }
        }

        if (dates.size() >= 2) {
            return dates.get(1);
        }

        return null;
    }


    private boolean isRoundTripSearch(TripSearchInput input) {
        return input != null && input.returnDate() != null;
    }

    private String buildRoundTripSearchIntro(TripSearchInput input) {
        if (!isRoundTripSearch(input)) {
            return "";
        }

        return """
                🔁 Ida e volta identificada.

                Primeiro escolha a viagem de ida.
                Depois da reserva da ida, eu vou te orientar para reservar a volta.

                Retorno previsto: %s
                """.formatted(input.returnDate().format(DATE_FORMATTER)).trim();
    }

        private String buildRoundTripReturnInstructionFromMetadata(String metadata) {
        String tripType = extractMetadataValue(metadata, "trip_type");

        if (!"ROUND_TRIP".equalsIgnoreCase(tripType)) {
            return "";
        }

        String returnTripSelected = extractMetadataValue(metadata, "return_trip_selected");
        String returnTripId = extractMetadataValue(metadata, "return_selected_trip_id");

        if ("true".equalsIgnoreCase(returnTripSelected)
                && returnTripId != null
                && !returnTripId.isBlank()) {
            String returnSummary = buildSelectedReturnTripSummary(metadata);

            if (returnSummary == null || returnSummary.isBlank()) {
                return """
                        
                        🔁 Volta já escolhida.
                        """;
            }

            return """
                    
                    ✅ Volta já escolhida antes da finalização.

                    %s
                    """.formatted(returnSummary);
        }

        String origin = extractMetadataValue(metadata, "origin");
        String destination = extractMetadataValue(metadata, "destination");
        String returnDate = extractMetadataValue(metadata, "return_date");

        if (origin == null || destination == null || returnDate == null) {
            return "";
        }

        return """
                
                🔁 Próximo passo: reservar a volta

                Para reservar o trecho de volta, envie:

                Origem: %s
                Destino: %s
                Data: %s
                """.formatted(
                destination,
                origin,
                returnDate);
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
        return containsAny(
                normalizedMessage,
                "emitir bilhete",
                "emitir ticket",
                "reemitir bilhete",
                "reimprimir bilhete",
                "recuperar bilhete",
                "reenviar bilhete",
                "segunda via",
                "2 via",
                "2ª via",
                "meu bilhete",
                "meu ticket");
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

        String normalizedMetadata = metadata
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replace(";", "\n");

        for (String line : splitLines(normalizedMetadata)) {
            String cleanedLine = line == null ? "" : line.trim();

            if (!cleanedLine.startsWith(prefix)) {
                continue;
            }

            String value = cleanedLine.substring(prefix.length()).trim();
            foundValue = value.isBlank() ? null : value;
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

        Matcher optionMatcher = TRIP_OPTION_PATTERN.matcher(messageText);

        if (optionMatcher.find()) {
            try {
                return Integer.parseInt(optionMatcher.group(1));
            } catch (NumberFormatException exception) {
                return null;
            }
        }

        String cleaned = normalizeText(messageText)
                .replaceAll("[^0-9a-z ]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (cleaned.isBlank()) {
            return null;
        }

        Matcher numberMatcher = Pattern.compile("(\\d+)").matcher(cleaned);

        if (!numberMatcher.find()) {
            return null;
        }

        try {
            return Integer.parseInt(numberMatcher.group(1));
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

        if (isOptionOne(normalizedMessage) || containsAny(normalizedMessage, "sim", "confirmar", "confirmo", "continuar")) {
            if (optionalPassenger.isEmpty()) {
                updateSessionStep(
                        session,
                        WhatsappConversationStep.ASKING_FULL_NAME,
                        session.getMetadata());

                return allowed(
                        "ASK_PASSENGER_NAME",
                        "Não encontrei mais seus dados salvos.\n\nQual é o nome completo do passageiro?");
            }

            Passenger passenger = optionalPassenger.get();
            PassengerDocumentType documentType = getPassengerDocumentType(passenger);

            String metadata = appendMetadata(
                    session.getMetadata(),
                    "confirmed_passenger_id=" + passenger.getId(),
                    "confirmed_passenger_name=" + passenger.getFullName(),
                    "confirmed_document_type=" + documentType,
                    "confirmed_document_number=" + passenger.getDocumentNumber(),
                    "passenger_id=" + passenger.getId(),
                    "passenger_name=" + passenger.getFullName(),
                    "passenger_document_type=" + documentType,
                    "passenger_document=" + passenger.getDocumentNumber());

            updateSessionPassenger(session, passenger);

            if (!hasPassengerFareType(metadata)) {
                return askPassengerFareType(session, metadata, passenger);
            }

            if (isAngolaTermsRequired(metadata) && !isAngolaTermsAccepted(metadata)) {
                String termsMetadata = appendMetadata(
                        metadata,
                        "angola_terms_acceptance_pending=true",
                        "angola_terms_version=AO-2026-001");

                updateSessionStep(
                        session,
                        WhatsappConversationStep.CONFIRMING_PASSENGER_DATA,
                        termsMetadata);

                return allowed(
                        "ANGOLA_TERMS",
                        buildAngolaTermsCard());
            }

            session.setMetadata(metadata);

            return createBookingWithPassenger(session, passenger);
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

                %s
                """.formatted(
                firstName,
                buildDocumentInstructionFromMetadata(metadata, documentLabel));

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
                    ? "Exemplo: 001058899UE035"
                    : "Exemplo: 52998224725";

            boolean angolaFlow = isAngolaDocumentFlow(session.getMetadata(), documentType);

            int attempts = readIntMetadataValue(
                    session.getMetadata(),
                    "invalid_document_attempts",
                    0);

            attempts++;

            String updatedMetadata = appendMetadata(
                    session.getMetadata(),
                    "invalid_document_attempts=" + attempts);

            updateSessionStep(
                    session,
                    WhatsappConversationStep.ASKING_DOCUMENT,
                    updatedMetadata);

            if (angolaFlow && attempts >= 3) {
                String blockedMetadata = appendMetadata(
                        updatedMetadata,
                        "document_blocked=true",
                        "document_block_reason=invalid_document_limit",
                        "document_block_country=AO");

                updateSessionStep(
                        session,
                        WhatsappConversationStep.START,
                        blockedMetadata);

                return allowed(
                        "DOCUMENT_BLOCKED",
                        """
                                Lamentamos, não é possível completar a reserva e emitir bilhete sem documento de identificação válido.

                                Para viagens em Angola, o passageiro deve apresentar BI ou Passaporte válido.

                                Verifique o documento e inicie uma nova compra quando tiver os dados corretos.

                                Exemplo de BI válido:
                                001058899UE035
                                """.trim());
            }

            String warning = angolaFlow
                    ? "Tentativa " + attempts + " de 3."
                    : "Verifique os dados e envie novamente.";

            return allowed(
                    "ASK_PASSENGER_DOCUMENT",
                    """
                            %s inválido.

                            %s

                            %s
                            """.formatted(documentLabel, warning, example).trim());
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
                "confirmed_document_number=" + passenger.getDocumentNumber(),
                "invalid_document_attempts=0",
                "document_blocked=false");

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


    private boolean isAngolaDocumentFlow(
            String metadata,
            PassengerDocumentType documentType) {
        String country = extractMetadataValue(metadata, "country");

        if ("AO".equalsIgnoreCase(country)) {
            return true;
        }

        String currency = extractMetadataValue(metadata, "currency");

        if ("AOA".equalsIgnoreCase(currency)) {
            return true;
        }

        return PassengerDocumentType.BI.equals(documentType);
    }

    private int readIntMetadataValue(
            String metadata,
            String key,
            int defaultValue) {
        String raw = extractMetadataValue(metadata, key);

        if (raw == null || raw.isBlank()) {
            raw = extractMetadataValueFlexible(metadata, key);
        }

        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private String extractMetadataValueFlexible(
            String metadata,
            String key) {
        if (metadata == null || metadata.isBlank() || key == null || key.isBlank()) {
            return null;
        }

        String pattern = "(^|[;\\n\\r])\\s*" + java.util.regex.Pattern.quote(key) + "=([^;\\n\\r]*)";
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile(pattern)
                .matcher(metadata);

        String value = null;

        while (matcher.find()) {
            value = matcher.group(2) != null ? matcher.group(2).trim() : null;
        }

        return value == null || value.isBlank() ? null : value;
    }
    private boolean hasAngolaTermsContext(String metadata) {
        return isAngolaTermsAcceptancePending(metadata) && !isAngolaTermsAccepted(metadata);
    }

    private boolean isAngolaTermsAcceptanceIntent(String normalizedMessage) {
        return isOptionOne(normalizedMessage)
                || containsAny(normalizedMessage, "aceito", "aceitar", "concordo", "continuar");
    }

    private WhatsappCommandResult acceptAngolaTermsAndCreateBooking(WhatsappSessionResponse session) {
        String metadata = appendMetadata(
                session.getMetadata(),
                "angola_terms_acceptance_pending=false",
                "angola_terms_accepted=true",
                "angola_terms_version=AO-2026-001",
                "angola_terms_accepted_at=" + LocalDateTime.now());

        String passengerId = extractMetadataValue(metadata, "confirmed_passenger_id");

        if (passengerId == null || passengerId.isBlank()) {
            Optional<Passenger> optionalPassenger = findRealPassengerForWhatsapp(session);

            if (optionalPassenger.isPresent()) {
                Passenger passenger = optionalPassenger.get();
                PassengerDocumentType documentType = getPassengerDocumentType(passenger);

                metadata = appendMetadata(
                        metadata,
                        "confirmed_passenger_id=" + passenger.getId(),
                        "confirmed_passenger_name=" + passenger.getFullName(),
                        "confirmed_document_type=" + documentType,
                        "confirmed_document_number=" + passenger.getDocumentNumber(),
                        "passenger_id=" + passenger.getId(),
                        "passenger_name=" + passenger.getFullName(),
                        "passenger_document_type=" + documentType,
                        "passenger_document=" + passenger.getDocumentNumber());

                passengerId = passenger.getId().toString();
                updateSessionPassenger(session, passenger);
            }
        }

        updateSessionStep(
                session,
                WhatsappConversationStep.CONFIRMING_PASSENGER_DATA,
                metadata);

        if (passengerId == null || passengerId.isBlank()) {
            updateSessionStep(
                    session,
                    WhatsappConversationStep.ASKING_FULL_NAME,
                    metadata);

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
                    metadata);

            return allowed(
                    "ASK_PASSENGER_NAME",
                    "Não consegui confirmar o passageiro informado.\n\nQual é o nome completo do passageiro?");
        }
    }

        private WhatsappCommandResult handlePassengerDataConfirmation(
            WhatsappSessionResponse session,
            String normalizedMessage) {
        if (isAngolaTermsAcceptancePending(session.getMetadata())) {
            if (isAngolaTermsAcceptanceIntent(normalizedMessage)) {
                return acceptAngolaTermsAndCreateBooking(session);
            }

            if (isOptionTwo(normalizedMessage)
                    || containsAny(normalizedMessage, "cancelar", "não aceito", "nao aceito", "recusar")) {
                String metadata = appendMetadata(
                        session.getMetadata(),
                        "angola_terms_acceptance_pending=false",
                        "angola_terms_accepted=false",
                        "booking_cancelled_by_terms=true");

                updateSessionStep(
                        session,
                        WhatsappConversationStep.START,
                        metadata);

                return allowed(
                        "ANGOLA_TERMS_DECLINED",
                        """
                                Reserva não concluída.

                                Para viagens em Angola, é necessário aceitar as condições de reserva, documento e pagamento antes de finalizar a compra.

                                Quando desejar, inicie uma nova compra enviando:
                                Comprar passagem
                                """.trim());
            }

            return allowed(
                    "ANGOLA_TERMS",
                    buildAngolaTermsCard());
        }

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

            if (!hasPassengerFareType(session.getMetadata())) {
                try {
                    Passenger passenger = passengerRepository.findById(UUID.fromString(passengerId))
                            .orElseThrow(() -> new IllegalArgumentException("Passageiro não encontrado."));

                    return askPassengerFareType(session, session.getMetadata(), passenger);

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

            if (isAngolaTermsRequired(session.getMetadata())
                    && !isAngolaTermsAccepted(session.getMetadata())) {
                String metadata = appendMetadata(
                        session.getMetadata(),
                        "angola_terms_acceptance_pending=true",
                        "angola_terms_version=AO-2026-001");

                updateSessionStep(
                        session,
                        WhatsappConversationStep.CONFIRMING_PASSENGER_DATA,
                        metadata);

                return allowed(
                        "ANGOLA_TERMS",
                        buildAngolaTermsCard());
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


            private boolean isAngolaTermsRequired(String metadata) {
        String country = extractMetadataValue(metadata, "country");

        if ("AO".equalsIgnoreCase(country)) {
            return true;
        }

        String currency = extractMetadataValue(metadata, "currency");

        if ("AOA".equalsIgnoreCase(currency)) {
            return true;
        }

        String documentType = extractMetadataValue(metadata, "confirmed_document_type");

        return "BI".equalsIgnoreCase(documentType);
    }

    private boolean isAngolaTermsAccepted(String metadata) {
        String accepted = extractMetadataValue(metadata, "angola_terms_accepted");
        return "true".equalsIgnoreCase(accepted);
    }

    private boolean isAngolaTermsAcceptancePending(String metadata) {
        String pending = extractMetadataValue(metadata, "angola_terms_acceptance_pending");
        return "true".equalsIgnoreCase(pending);
    }

    private String buildAngolaTermsCard() {
        return """
                📄 Termos de reserva — Angola

                Antes de concluir a reserva, confirme que leu e aceita as condições:

                1️⃣ O passageiro deve apresentar BI ou Passaporte válido.
                2️⃣ Para pagamento em dinheiro, o passageiro deve comparecer ao guichê da empresa com antecedência mínima de 30 minutos.
                3️⃣ A reserva pode expirar caso o pagamento não seja confirmado dentro do prazo.
                4️⃣ A empresa transportadora é responsável pela execução da viagem.
                5️⃣ Os dados informados serão usados para emissão da reserva, bilhete e fatura, quando solicitado.

                Responda:
                1️⃣ Aceito e continuar
                2️⃣ Cancelar
                """.trim();
    }

    private boolean isPassengerFareTypeSelectionPending(String metadata) {
        String pending = extractMetadataValue(metadata, "passenger_fare_type_selection_pending");
        return "true".equalsIgnoreCase(pending);
    }

    private boolean hasPassengerFareType(String metadata) {
        String raw = extractMetadataValue(metadata, "passenger_fare_type");

        if (raw == null || raw.isBlank()) {
            return false;
        }

        try {
            PassengerFareType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private WhatsappCommandResult askPassengerFareType(
            WhatsappSessionResponse session,
            String metadata,
            Passenger passenger) {
        String adjustedMetadata = metadata;

        if (passenger != null) {
            PassengerDocumentType documentType = getPassengerDocumentType(passenger);

            adjustedMetadata = appendMetadata(
                    adjustedMetadata,
                    "confirmed_passenger_id=" + passenger.getId(),
                    "confirmed_passenger_name=" + passenger.getFullName(),
                    "confirmed_document_type=" + documentType,
                    "confirmed_document_number=" + passenger.getDocumentNumber(),
                    "passenger_id=" + passenger.getId(),
                    "passenger_name=" + passenger.getFullName(),
                    "passenger_document_type=" + documentType,
                    "passenger_document=" + passenger.getDocumentNumber());
        }

        adjustedMetadata = appendMetadata(
                adjustedMetadata,
                "passenger_fare_type_selection_pending=true");

        updateSessionStep(
                session,
                WhatsappConversationStep.CONFIRMING_PASSENGER_DATA,
                adjustedMetadata);

        String passengerName = passenger != null && passenger.getFullName() != null
                ? passenger.getFullName()
                : "passageiro";

        return allowed(
                "ASK_PASSENGER_FARE_TYPE",
                String.join("\n",
                        "👤 Tipo de passageiro",
                        "",
                        "Passageiro: " + passengerName,
                        "",
                        "Informe quem vai viajar:",
                        "",
                        "1️⃣ Adulto",
                        "2️⃣ Criança acompanhada com poltrona",
                        "3️⃣ Menor desacompanhado",
                        "",
                        "Você pode responder com o número ou escrevendo:",
                        "- adulto",
                        "- criança",
                        "- menor desacompanhado",
                        "",
                        "Obs.: criança de colo sem poltrona será liberada no próximo módulo."));
    }

    private WhatsappCommandResult handlePassengerFareTypeSelection(
            WhatsappSessionResponse session,
            String normalizedMessage) {
        PassengerFareType passengerFareType = resolveSelectedPassengerFareType(normalizedMessage);

        if (PassengerFareType.INFANT_ON_LAP.equals(passengerFareType)) {
            return allowed(
                    "ASK_PASSENGER_FARE_TYPE",
                    String.join("\n",
                            "Criança de colo sem poltrona será liberada no próximo módulo.",
                            "",
                            "Por enquanto escolha uma das opções:",
                            "",
                            "1️⃣ Adulto",
                            "2️⃣ Criança acompanhada com poltrona",
                            "3️⃣ Menor desacompanhado"));
        }

        if (passengerFareType == null) {
            return allowed(
                    "ASK_PASSENGER_FARE_TYPE",
                    String.join("\n",
                            "Não consegui identificar o tipo de passageiro.",
                            "",
                            "Responda:",
                            "",
                            "1️⃣ Adulto",
                            "2️⃣ Criança acompanhada com poltrona",
                            "3️⃣ Menor desacompanhado"));
        }

        String metadata = appendMetadata(
                session.getMetadata(),
                "passenger_fare_type_selection_pending=false",
                "passenger_fare_type=" + passengerFareType,
                "passenger_fare_label=" + passengerFareTypeLabel(passengerFareType));

        updateSessionStep(
                session,
                WhatsappConversationStep.CONFIRMING_PASSENGER_DATA,
                metadata);

        Passenger passenger = resolveConfirmedPassengerFromMetadata(session, metadata);

        if (passenger == null) {
            updateSessionStep(
                    session,
                    WhatsappConversationStep.ASKING_FULL_NAME,
                    metadata);

            return allowed(
                    "ASK_PASSENGER_NAME",
                    "Não encontrei os dados do passageiro.\\n\\nQual é o nome completo do passageiro?");
        }

        session.setMetadata(metadata);

        return createBookingWithPassenger(session, passenger);
    }


    private boolean isOptionFour(String normalizedMessage) {
        Integer option = extractSelectedOptionNumber(normalizedMessage);
        return option != null && option == 4;
    }
    private PassengerFareType resolveSelectedPassengerFareType(String normalizedMessage) {
        if (normalizedMessage == null || normalizedMessage.isBlank()) {
            return null;
        }

        if (isOptionOne(normalizedMessage)
                || containsAny(normalizedMessage, "adulto", "maior")) {
            return PassengerFareType.ADULT;
        }

        if (isOptionTwo(normalizedMessage)
                || containsAny(normalizedMessage, "crianca", "criança", "infantil", "acompanhada")) {
            return PassengerFareType.CHILD_WITH_SEAT;
        }

        if (isOptionThree(normalizedMessage)
                || containsAny(normalizedMessage, "menor", "desacompanhado", "adolescente")) {
            return PassengerFareType.MINOR_UNACCOMPANIED;
        }

        if (isOptionFour(normalizedMessage)
                || containsAny(normalizedMessage, "colo", "bebe", "bebê")) {
            return PassengerFareType.INFANT_ON_LAP;
        }

        return null;
    }

    private PassengerFareType resolvePassengerFareTypeFromMetadata(String metadata) {
        String raw = extractMetadataValue(metadata, "passenger_fare_type");

        if (raw == null || raw.isBlank()) {
            return PassengerFareType.ADULT;
        }

        try {
            return PassengerFareType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return PassengerFareType.ADULT;
        }
    }

    private String passengerFareTypeLabel(PassengerFareType passengerFareType) {
        if (passengerFareType == null) {
            return "Adulto";
        }

        return switch (passengerFareType) {
            case ADULT -> "Adulto";
            case CHILD_WITH_SEAT -> "Criança acompanhada com poltrona";
            case MINOR_UNACCOMPANIED -> "Menor desacompanhado";
            case INFANT_ON_LAP -> "Criança de colo";
        };
    }

    private Passenger resolveConfirmedPassengerFromMetadata(
            WhatsappSessionResponse session,
            String metadata) {
        String passengerId = extractMetadataValue(metadata, "confirmed_passenger_id");

        if (passengerId != null && !passengerId.isBlank()) {
            try {
                return passengerRepository.findById(UUID.fromString(passengerId)).orElse(null);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        return findRealPassengerForWhatsapp(session).orElse(null);
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

        String existingBookingCode = extractMetadataValue(session.getMetadata(), "booking_code");

        if (existingBookingCode != null && !existingBookingCode.isBlank()) {
            updateSessionStep(
                    session,
                    WhatsappConversationStep.WAITING_PAYMENT,
                    session.getMetadata());

            return allowed(
                    "CREATE_BOOKING",
                    """
                            ✅ Reserva já criada.

                            🎫 Código: %s

                            Escolha uma opção:
                            1️⃣ Pagar agora
                            2️⃣ Fazer nova busca
                            """.formatted(existingBookingCode).trim());
        }

        PassengerDocumentType documentType = getPassengerDocumentType(passenger);

        String preBookingMetadata = appendMetadata(
                session.getMetadata(),
                "confirmed_passenger_id=" + passenger.getId(),
                "confirmed_passenger_name=" + passenger.getFullName(),
                "confirmed_document_type=" + documentType,
                "confirmed_document_number=" + passenger.getDocumentNumber(),
                "passenger_id=" + passenger.getId(),
                "passenger_name=" + passenger.getFullName(),
                "passenger_document_type=" + documentType,
                "passenger_document=" + passenger.getDocumentNumber());

        if (!hasPassengerFareType(preBookingMetadata)) {
            return askPassengerFareType(session, preBookingMetadata, passenger);
        }

        if (isAngolaTermsRequired(preBookingMetadata) && !isAngolaTermsAccepted(preBookingMetadata)) {
            String termsMetadata = appendMetadata(
                    preBookingMetadata,
                    "angola_terms_acceptance_pending=true",
                    "angola_terms_version=AO-2026-001");

            updateSessionStep(
                    session,
                    WhatsappConversationStep.CONFIRMING_PASSENGER_DATA,
                    termsMetadata);

            return allowed(
                    "ANGOLA_TERMS",
                    buildAngolaTermsCard());
        }

        session.setMetadata(preBookingMetadata);

        try {
            Trip trip = tripRepository.findById(tripId)
                    .orElseThrow(() -> new IllegalArgumentException("Viagem não encontrada."));

            Integer seatNumber = findFirstAvailableSeat(trip);

            BookingRequest request = new BookingRequest()
                    .setTripId(trip.getId())
                    .setPassengerId(passenger.getId())
                    .setSeatNumber(seatNumber)
                    .setPassengerFareType(resolvePassengerFareTypeFromMetadata(preBookingMetadata));

            BookingResponse booking = bookingService.create(request);
            BookingResponse returnBooking = createReturnBookingIfNeeded(session, passenger);

            String documentLabel = documentValidatorService.label(documentType);
            String maskedDocument = documentValidatorService.mask(documentType, passenger.getDocumentNumber());

            String metadata = appendMetadata(
                    preBookingMetadata,
                    "angola_terms_acceptance_pending=false",
                    "booking_id=" + booking.getId(),
                    "booking_code=" + booking.getBookingCode(),
                    "seat_number=" + booking.getSeatNumber(),
                    "booking_status=" + booking.getStatus(),
                    "currency=" + booking.getCurrency(),
                    buildReturnBookingMetadata(returnBooking));

            updateSessionStep(
                    session,
                    WhatsappConversationStep.WAITING_PAYMENT,
                    metadata);

            String reply = """
                    ✅ Reserva criada

                    %s

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
                    %s
                    """.formatted(
                    buildCountryContextFromMetadata(metadata),
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
                            : "-",
                    buildRoundTripReturnInstructionFromMetadata(metadata));

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

        session
                .setPassengerId(passenger.getId())
                .setPassengerFullName(passenger.getFullName())
                .setPassengerDocumentNumber(passenger.getDocumentNumber());
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

        session
                .setCurrentStep(step)
                .setMetadata(metadata);
    }


    private boolean isPassengerDocumentCompatibleWithTrip(
            Passenger passenger,
            PassengerDocumentType requiredDocumentType) {
        if (passenger == null || requiredDocumentType == null) {
            return false;
        }

        if (passenger.getDocumentType() == null) {
            return false;
        }

        return passenger.getDocumentType().equals(requiredDocumentType);
    }
    private String buildSupportedCountriesCard() {
        return """
                🌍 O VaiRápido atende viagens no Brasil e em Angola.

                🇦🇴 Angola
                Documento: BI ou Passaporte
                Moeda: AOA
                Pagamento: Multicaixa, Unitel Money, Afrimoney ou dinheiro

                🇧🇷 Brasil
                Documento: CPF ou Passaporte
                Moeda: BRL
                Pagamento: Pix ou dinheiro

                Eu identifico o país automaticamente pela cidade de origem e destino.
                """.trim();
    }

    private String buildTripSearchExamplesCard() {
        return """
                Exemplos de busca:

                🇦🇴 Angola
                Origem: Luanda
                Destino: Benguela
                Data: 25/06/2026

                🇧🇷 Brasil
                Origem: São Paulo
                Destino: Rio de Janeiro
                Data: 25/06/2026
                """.trim();
    }

    private String buildCountryContextFromMetadata(String metadata) {
        String country = extractMetadataValue(metadata, "country");

        if ("AO".equalsIgnoreCase(country)) {
            return """
                    🇦🇴 Viagem identificada: Angola
                    Documento: BI ou Passaporte
                    Moeda: AOA
                    Pagamento: Multicaixa, Unitel Money, Afrimoney ou dinheiro
                    """.trim();
        }

        if ("BR".equalsIgnoreCase(country)) {
            return """
                    🇧🇷 Viagem identificada: Brasil
                    Documento: CPF ou Passaporte
                    Moeda: BRL
                    Pagamento: Pix ou dinheiro
                    """.trim();
        }

        return buildSupportedCountriesCard();
    }

    private String buildDocumentInstructionFromMetadata(String metadata, String documentLabel) {
        String country = extractMetadataValue(metadata, "country");

        if ("AO".equalsIgnoreCase(country)) {
            return """
                    🇦🇴 Para viagens em Angola, informe o BI do passageiro.
                    Também aceitaremos Passaporte em versão futura do fluxo.
                    """.trim();
        }

        if ("BR".equalsIgnoreCase(country)) {
            return """
                    🇧🇷 Para viagens no Brasil, informe o CPF do passageiro.
                    Também aceitaremos Passaporte em versão futura do fluxo.
                    """.trim();
        }

        return "Agora informe o " + documentLabel + " do passageiro.";
    }

    private String buildNewSearchInstructionCard() {
        return """
                Para mudar a busca, envie novamente origem, destino e data.

                🇦🇴 Exemplo Angola:
                Origem: Luanda
                Destino: Benguela
                Data: 25/06/2026

                🇧🇷 Exemplo Brasil:
                Origem: São Paulo
                Destino: Rio de Janeiro
                Data: 25/06/2026
                """.trim();
    }

    private String buildCountryMetadata(CountryResolutionResponse resolution) {
        if (resolution == null || resolution.country() == null) {
            return "";
        }

        return """
                country=%s
                currency=%s
                document_types=%s
                payment_methods=%s
                """.formatted(
                resolution.country(),
                resolution.currency(),
                joinEnumNames(resolution.documentTypes()),
                joinEnumNames(resolution.paymentMethods())).trim();
    }

    private String buildCountryDetectedMessage(CountryResolutionResponse resolution) {
        if (resolution == null || resolution.country() == null) {
            return "";
        }

        return switch (resolution.country()) {
            case AO -> """
                    🇦🇴 País identificado: Angola
                    Moeda: AOA
                    Documento: BI ou Passaporte
                    Pagamento: Multicaixa, Unitel Money, Afrimoney ou dinheiro
                    """.trim();

            case BR -> """
                    🇧🇷 País identificado: Brasil
                    Moeda: BRL
                    Documento: CPF ou Passaporte
                    Pagamento: Pix ou dinheiro
                    """.trim();
        };
    }

    private String joinEnumNames(List<? extends Enum<?>> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        return values.stream()
                .map(Enum::name)
                .reduce((first, second) -> first + "," + second)
                .orElse("");
    }

        private String buildTripSearchMetadata(
            TripSearchInput input,
            List<Trip> options) {
        StringBuilder metadata = new StringBuilder();

        metadata.append("flow=BUY_TICKET\n");
        metadata.append("origin=").append(input.origin()).append("\n");
        metadata.append("destination=").append(input.destination()).append("\n");
        metadata.append("date=").append(input.date().format(DATE_FORMATTER)).append("\n");

        if (input.returnDate() != null) {
            metadata.append("trip_type=ROUND_TRIP\n");
            metadata.append("return_date=").append(input.returnDate().format(DATE_FORMATTER)).append("\n");
        } else {
            metadata.append("trip_type=ONE_WAY\n");
        }

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

        if (rawType != null && !rawType.isBlank()) {
            try {
                return PassengerDocumentType.valueOf(rawType.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // Continua para resolver pelo país.
            }
        }

        String country = extractMetadataValue(metadata, "country");

        if ("AO".equalsIgnoreCase(country)) {
            return PassengerDocumentType.BI;
        }

        if ("BR".equalsIgnoreCase(country)) {
            return PassengerDocumentType.CPF;
        }

        String documentTypes = extractMetadataValue(metadata, "document_types");

        if (documentTypes != null && !documentTypes.isBlank()) {
            String upperDocumentTypes = documentTypes.toUpperCase(Locale.ROOT);

            if (upperDocumentTypes.contains("BI")) {
                return PassengerDocumentType.BI;
            }

            if (upperDocumentTypes.contains("CPF")) {
                return PassengerDocumentType.CPF;
            }
        }

        return documentValidatorService.defaultDocumentType();
    }



            private PassengerDocumentType getPassengerDocumentType(Passenger passenger) {
        if (passenger == null || passenger.getDocumentType() == null) {
            return documentValidatorService.defaultDocumentType();
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
        Integer optionNumber = extractSimpleOptionNumber(normalizedMessage);
        return optionNumber != null && optionNumber == 1;
    }
    private boolean isOptionTwo(String normalizedMessage) {
        Integer optionNumber = extractSimpleOptionNumber(normalizedMessage);
        return optionNumber != null && optionNumber == 2;
    }
    private boolean isOptionThree(String normalizedMessage) {
        Integer optionNumber = extractSimpleOptionNumber(normalizedMessage);
        return optionNumber != null && optionNumber == 3;
    }


    private String extractFirstName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "passageiro";
        }

        return fullName.trim().split("\\s+")[0];
    }
    private boolean containsAny(String text, String... words) {
        if (text == null || words == null) {
            return false;
        }

        for (String word : words) {
            if (word != null && !word.isBlank() && text.contains(word)) {
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
            LocalDate date,
            LocalDate returnDate) {
    }
}





























