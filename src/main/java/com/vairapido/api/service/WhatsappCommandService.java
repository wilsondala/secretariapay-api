package com.vairapido.api.service;

import com.vairapido.api.dto.dashboard.DashboardSummaryResponse;
import com.vairapido.api.dto.publicticket.TicketValidationResponse;
import com.vairapido.api.dto.whatsappcommand.WhatsappCommandResult;
import com.vairapido.api.dto.whatsappsession.WhatsappSessionResponse;
import com.vairapido.api.entity.User;
import com.vairapido.api.entity.enums.UserRole;
import com.vairapido.api.entity.enums.UserStatus;
import com.vairapido.api.entity.enums.WhatsappSessionType;
import com.vairapido.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WhatsappCommandService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Pattern TICKET_CODE_PATTERN =
            Pattern.compile("(VRTK-[A-Z0-9\\-]+|VRTK\\s*[A-Z0-9\\-]+)", Pattern.CASE_INSENSITIVE);

    private final UserRepository userRepository;
    private final DashboardService dashboardService;
    private final PublicTicketValidationService publicTicketValidationService;

    public WhatsappCommandService(
            UserRepository userRepository,
            DashboardService dashboardService,
            PublicTicketValidationService publicTicketValidationService
    ) {
        this.userRepository = userRepository;
        this.dashboardService = dashboardService;
        this.publicTicketValidationService = publicTicketValidationService;
    }

    @Transactional
    public WhatsappCommandResult handleCommand(
            WhatsappSessionResponse session,
            String messageText
    ) {
        String normalizedMessage = normalizeText(messageText);

        if (normalizedMessage.isBlank()) {
            return defaultHelp(session);
        }

        if (containsAny(normalizedMessage, "menu", "ajuda", "inicio", "começar", "comecar")) {
            return menu(session);
        }

        if (isTicketValidationCommand(normalizedMessage)) {
            return validateTicket(session, messageText);
        }

        if (isDashboardCommand(normalizedMessage)) {
            return dashboard(session);
        }

        if (isBuyTicketCommand(normalizedMessage)) {
            return buyTicket(session);
        }

        return fallback(session);
    }

    private WhatsappCommandResult validateTicket(
            WhatsappSessionResponse session,
            String originalMessage
    ) {
        if (!isAllowedToValidateTickets(session)) {
            return denied(
                    "VALIDATE_TICKET",
                    "Acesso negado. Este número não está autorizado para validar bilhetes."
            );
        }

        String ticketCode = extractTicketCode(originalMessage);

        if (ticketCode == null || ticketCode.isBlank()) {
            return allowed(
                    "VALIDATE_TICKET",
                    "Envie o código do bilhete para validação.\n\nExemplo:\nValidar bilhete VRTK-ABC123"
            );
        }

        TicketValidationResponse validation =
                publicTicketValidationService.validateByCode(ticketCode);

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
                    "Acesso negado. Este número não está vinculado a um usuário ativo."
            );
        }

        User user = optionalUser.get();

        DashboardSummaryResponse summary;

        if (UserRole.ADMIN.equals(user.getRole())) {
            summary = dashboardService.getSummary();
        } else if (UserRole.COMPANY_ADMIN.equals(user.getRole())
                && user.getTransportCompany() != null) {
            summary = dashboardService.getCompanySummary(
                    user.getTransportCompany().getId()
            );
        } else {
            return denied(
                    "DASHBOARD",
                    "Acesso negado. Este perfil não possui acesso ao dashboard pelo WhatsApp."
            );
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
                        : "-"
        );

        return allowed("DASHBOARD", reply.trim());
    }

    private WhatsappCommandResult buyTicket(WhatsappSessionResponse session) {
        if (!WhatsappSessionType.PASSENGER.equals(session.getSessionType())) {
            return allowed(
                    "BUY_TICKET",
                    "Este comando é destinado ao passageiro.\n\nPara operação, use:\n- Validar bilhete VRTK-...\n- Resumo de hoje"
            );
        }

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
                """;

        return allowed("BUY_TICKET", reply.trim());
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
                    """
            );
        }

        return allowed(
                "MENU",
                """
                Menu VaiRápido

                Comandos disponíveis:
                1. Comprar passagem
                2. Consultar bilhete
                3. Ajuda
                """
        );
    }

    private WhatsappCommandResult defaultHelp(WhatsappSessionResponse session) {
        return menu(session);
    }

    private WhatsappCommandResult fallback(WhatsappSessionResponse session) {
        if (WhatsappSessionType.USER.equals(session.getSessionType())) {
            return allowed(
                    "FALLBACK",
                    """
                    Não entendi o comando.

                    Tente uma destas opções:
                    - Validar bilhete VRTK-...
                    - Resumo de hoje
                    - Menu
                    """
            );
        }

        return allowed(
                "FALLBACK",
                """
                Não entendi sua mensagem.

                Tente uma destas opções:
                - Comprar passagem
                - Menu
                - Ajuda
                """
        );
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
                UserStatus.ACTIVE
        );
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
            String replyMessage
    ) {
        return new WhatsappCommandResult()
                .setProcessed(true)
                .setAllowed(true)
                .setCommandName(commandName)
                .setReplyMessage(replyMessage);
    }

    private WhatsappCommandResult denied(
            String commandName,
            String replyMessage
    ) {
        return new WhatsappCommandResult()
                .setProcessed(true)
                .setAllowed(false)
                .setCommandName(commandName)
                .setReplyMessage(replyMessage);
    }
}