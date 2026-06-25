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

    @Transactional
    public WhatsappCommandResult handleCommand(
            WhatsappSessionResponse session,
            String messageText) {
        String normalizedMessage = normalizeText(messageText);

        if (normalizedMessage.isBlank()) {
            return defaultHelp(session);
        }

        if (WhatsappSessionType.PASSENGER.equals(session.getSessionType())
                && isGreetingNamePending(session.getMetadata())) {
            return handleGreetingNameAnswer(session, messageText);
        }

        if (WhatsappSessionType.PASSENGER.equals(session.getSessionType())
                && isSmartGreeting(normalizedMessage)) {
            if (hasCustomerName(session.getMetadata())) {
                return handleKnownCustomerGreeting(session, normalizedMessage);
            }

            return askCustomerNameFromGreeting(session, normalizedMessage);
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
                && isPaymentMethodSelectionPending(session.getMetadata())
                && isPaymentMethodOption(normalizedMessage)) {
            return payBookingWithSelectedPaymentMethod(session, normalizedMessage);
        }

        if (WhatsappSessionType.PASSENGER.equals(session.getSessionType())
                && WhatsappConversationStep.WAITING_PAYMENT.equals(session.getCurrentStep())
                && isOptionOne(normalizedMessage)) {
            return askPaymentMethodFromWhatsapp(session, "Pagar reserva");
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

        String reply = """
                %s
                Posso ajudar você a comprar sua passagem pelo WhatsApp.

                %s

                Para comprar sua passagem, envie:
                1. Cidade de origem
                2. Cidade de destino
                3. Data da viagem
                4. Documento do passageiro

                %s

                Você também pode enviar em linhas simples:
                Luanda
                Benguela
                25/06/2026
                """.formatted(
                nameLine,
                buildSupportedCountriesCard(),
                buildTripSearchExamplesCard());

        return allowed("BUY_TICKET", reply.trim());
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

                    Passageiro: %s
                    %s: %s

                    Essa compra é para esse passageiro?

                    1. Sim, continuar
                    2. Não, comprar para outra pessoa
                    3. Alterar meus dados
                    """.formatted(
                    firstName,
                    buildCountryContextFromMetadata(metadata),
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

                        Qual é o nome completo do passageiro?
                        """.formatted(
                        buildCountryContextFromMetadata(metadata)).trim());
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

            String metadata = appendMetadata(
                    session.getMetadata(),
                    "payment_method_selection_pending=false",
                    "payment_id=" + confirmedPayment.getId(),
                    "payment_code=" + confirmedPayment.getPaymentCode(),
                    "payment_method=" + confirmedPayment.getMethod(),
                    "payment_method_label=" + paymentMethodLabel(confirmedPayment.getMethod()),
                    "payment_status=" + confirmedPayment.getStatus(),
                    "booking_status=" + confirmedPayment.getBookingStatus());

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
        return normalizedMessage != null && normalizedMessage.matches("[1-4]");
    }

    private PaymentMethod resolveSelectedPaymentMethod(
            String metadata,
            Booking booking,
            String normalizedMessage) {
        boolean angola = isAngolaPaymentContext(metadata, booking);
        String option = normalizedMessage == null ? "" : normalizedMessage.trim();

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

            String metadata = appendMetadata(
                    session.getMetadata(),
                    "payment_id=" + confirmedPayment.getId(),
                    "payment_code=" + confirmedPayment.getPaymentCode(),
                    "payment_method=" + confirmedPayment.getMethod(),
                    "payment_method_label=" + paymentMethodLabel(confirmedPayment.getMethod()),
                    "payment_status=" + confirmedPayment.getStatus(),
                    "booking_status=" + confirmedPayment.getBookingStatus());

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
        return normalizedMessage != null && normalizedMessage.matches("[1-5]");
    }

    private Integer extractSimpleOptionNumber(String normalizedMessage) {
        if (normalizedMessage == null || !normalizedMessage.matches("\\d+")) {
            return null;
        }

        try {
            return Integer.parseInt(normalizedMessage);
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


    private boolean isRoundTripRequest(String normalizedMessage) {
        if (normalizedMessage == null || normalizedMessage.isBlank()) {
            return false;
        }

        return normalizedMessage.contains("ida e volta")
                || normalizedMessage.contains("ida volta")
                || normalizedMessage.contains("volta")
                || normalizedMessage.contains("retorno")
                || normalizedMessage.contains("regresso");
    }

    private LocalDate extractReturnDate(String messageText) {
        if (messageText == null || messageText.isBlank()) {
            return null;
        }

        String[] labels = {"volta", "retorno", "regresso"};

        for (String label : labels) {
            for (String line : splitLines(messageText)) {
                String normalizedLine = normalizeText(line);

                if (!normalizedLine.startsWith(label)) {
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
        private WhatsappCommandResult handlePassengerDataConfirmation(
            WhatsappSessionResponse session,
            String normalizedMessage) {
        if (isAngolaTermsAcceptancePending(session.getMetadata())) {
            if (isOptionOne(normalizedMessage)
                    || containsAny(normalizedMessage, "aceito", "aceitar", "concordo", "continuar")) {
                String metadata = appendMetadata(
                        session.getMetadata(),
                        "angola_terms_acceptance_pending=false",
                        "angola_terms_accepted=true",
                        "angola_terms_version=AO-2026-001",
                        "angola_terms_accepted_at=" + LocalDateTime.now());

                updateSessionStep(
                        session,
                        WhatsappConversationStep.CONFIRMING_PASSENGER_DATA,
                        metadata);

                String passengerId = extractMetadataValue(metadata, "confirmed_passenger_id");

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
            LocalDate date,
            LocalDate returnDate) {
    }
}























