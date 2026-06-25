from pathlib import Path

path = Path("src/main/java/com/vairapido/api/service/WhatsappCommandService.java")
text = path.read_text(encoding="utf-8")

def find_method_bounds(source, method_name):
    marker = f"private WhatsappCommandResult {method_name}("
    start = source.find(marker)
    if start == -1:
        raise SystemExit(f"Metodo nao encontrado: {method_name}")

    brace_start = source.find("{", start)
    if brace_start == -1:
        raise SystemExit(f"Abertura do metodo nao encontrada: {method_name}")

    depth = 0
    i = brace_start
    while i < len(source):
        ch = source[i]
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                return start, i + 1
        i += 1

    raise SystemExit(f"Fechamento do metodo nao encontrado: {method_name}")

# 1) Substitui buyTicket com versao segura, sem text block
buy_start, buy_end = find_method_bounds(text, "buyTicket")

new_buy_ticket = r'''
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
'''

text = text[:buy_start] + new_buy_ticket + text[buy_end:]

# 2) Insere handlers depois do bloco comprar passagem
old_block = '''            if (isBuyTicketCommand(normalizedMessage)
                    && !WhatsappConversationStep.ASKING_FULL_NAME.equals(session.getCurrentStep())
                    && !WhatsappConversationStep.ASKING_DOCUMENT.equals(session.getCurrentStep())
                    && !WhatsappConversationStep.CONFIRMING_PASSENGER_DATA.equals(session.getCurrentStep())
                    && !WhatsappConversationStep.CONFIRMING_SAVED_PASSENGER.equals(session.getCurrentStep())
                    && !WhatsappConversationStep.WAITING_PAYMENT.equals(session.getCurrentStep())
                    && !WhatsappConversationStep.CONFIRMING_BOOKING.equals(session.getCurrentStep())) {
                return buyTicket(session);
            }
'''

new_block = old_block + '''
            if (isTripTypeSelectionPending(session.getMetadata())) {
                return handleTripTypeSelection(session, normalizedMessage);
            }

            if (isOutboundTripSearchPending(session.getMetadata())) {
                return handleOutboundTripSearchAnswer(session, messageText);
            }

            if (isReturnTripSearchPending(session.getMetadata())) {
                return handleReturnTripSearchAnswer(session, messageText);
            }
'''

if old_block not in text:
    raise SystemExit("Bloco de compra não encontrado.")
text = text.replace(old_block, new_block, 1)

# 3) Melhora parseTripSearch para aceitar "Luanda > Benguela 26/06/2026"
parse_start, parse_end = None, None
marker = "private TripSearchInput parseTripSearch("
parse_start = text.find(marker)
if parse_start == -1:
    raise SystemExit("parseTripSearch não encontrado.")

brace_start = text.find("{", parse_start)
depth = 0
i = brace_start
while i < len(text):
    if text[i] == "{":
        depth += 1
    elif text[i] == "}":
        depth -= 1
        if depth == 0:
            parse_end = i + 1
            break
    i += 1

if parse_end is None:
    raise SystemExit("Fim do parseTripSearch não encontrado.")

new_parse = r'''
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
'''

text = text[:parse_start] + new_parse + text[parse_end:]

path.write_text(text, encoding="utf-8")
print("Patch seguro aplicado com sucesso.")
