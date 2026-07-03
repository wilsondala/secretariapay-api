package com.secretariapay.api.service.whatsapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secretariapay.api.dto.financial.MockAutomaticPaymentRequest;
import com.secretariapay.api.dto.financial.MockAutomaticPaymentResponse;
import com.secretariapay.api.dto.whatsapp.WhatsAppCloudSendResult;
import com.secretariapay.api.entity.WhatsappSession;
import com.secretariapay.api.entity.academic.AcademicClass;
import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.WhatsappConversationStep;
import com.secretariapay.api.entity.enums.WhatsappSessionStatus;
import com.secretariapay.api.entity.enums.WhatsappSessionType;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.entity.financial.Receipt;
import com.secretariapay.api.repository.WhatsappSessionRepository;
import com.secretariapay.api.repository.academic.StudentRepository;
import com.secretariapay.api.repository.financial.ChargeRepository;
import com.secretariapay.api.repository.financial.ReceiptRepository;
import com.secretariapay.api.service.financial.SecretariaPayMockAutomaticPaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SecretariaPayWhatsappFinancialConversationService {

    private static final String API_BASE_URL = "https://secretariapay-api.paixaoangola.com";
    private static final BigDecimal DEFAULT_AMOUNT = new BigDecimal("45000.00");
    private static final int SESSION_DURATION_HOURS = 24;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Pattern BI_PATTERN = Pattern.compile("\\b\\d{8,9}[a-zA-Z]{2}\\d{3}\\b");
    private static final Pattern STUDENT_NUMBER_PATTERN = Pattern.compile("\\b[A-Z]{2,10}[-/]?\\d{4}[-/]?[A-Z0-9-]{2,}\\b", Pattern.CASE_INSENSITIVE);

    private final StudentRepository studentRepository;
    private final ChargeRepository chargeRepository;
    private final ReceiptRepository receiptRepository;
    private final WhatsappSessionRepository sessionRepository;
    private final WhatsAppCloudApiClient whatsAppCloudApiClient;
    private final SecretariaPayMockAutomaticPaymentService mockAutomaticPaymentService;
    private final ObjectMapper objectMapper;

    public SecretariaPayWhatsappFinancialConversationService(
            StudentRepository studentRepository,
            ChargeRepository chargeRepository,
            ReceiptRepository receiptRepository,
            WhatsappSessionRepository sessionRepository,
            WhatsAppCloudApiClient whatsAppCloudApiClient,
            SecretariaPayMockAutomaticPaymentService mockAutomaticPaymentService
    ) {
        this.studentRepository = studentRepository;
        this.chargeRepository = chargeRepository;
        this.receiptRepository = receiptRepository;
        this.sessionRepository = sessionRepository;
        this.whatsAppCloudApiClient = whatsAppCloudApiClient;
        this.mockAutomaticPaymentService = mockAutomaticPaymentService;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public Optional<String> handle(String fromPhone, String messageType, String rawMessage) {
        String type = safe(messageType).toLowerCase(Locale.ROOT);
        String message = safe(rawMessage).trim();
        String normalized = normalize(message);

        if (!isTextLike(type) || normalized.isBlank()) {
            return Optional.empty();
        }

        WhatsappSession session = getOrCreateSession(fromPhone);
        Map<String, String> metadata = readMetadata(session);

        if (isRestartIntent(normalized) || isMainMenuIntent(normalized)) {
            resetFinancialMetadata(session, metadata, message);
            return Optional.of(buildMainMenuGreeting());
        }

        if (isThanksIntent(normalized) && isSecretariaPayFinancialContext(metadata)) {
            return Optional.of(closeAttendance(session, metadata, message));
        }

        Optional<String> stepReply = handleExpectedStep(session, metadata, fromPhone, message, normalized);
        if (stepReply.isPresent()) {
            return stepReply;
        }

        Optional<String> directIntentReply = handleDirectIntent(session, metadata, fromPhone, message, normalized);
        if (directIntentReply.isPresent()) {
            return directIntentReply;
        }

        if (isGreetingIntent(normalized)) {
            return Optional.of(buildMainMenuGreeting());
        }

        if (isConfusingMessage(normalized)) {
            return Optional.of(buildFallbackMenu());
        }

        return Optional.empty();
    }

    private Optional<String> handleExpectedStep(
            WhatsappSession session,
            Map<String, String> metadata,
            String fromPhone,
            String message,
            String normalized
    ) {
        String expected = metadata.get("expectedStep");

        if (isBlank(expected)) {
            return Optional.empty();
        }

        if ("STUDENT_IDENTIFIER".equalsIgnoreCase(expected)) {
            Optional<Student> studentOptional = findStudentByIdentifierOrName(message);

            if (studentOptional.isEmpty()) {
                return Optional.of("""
                        Não consegui localizar o estudante com esses dados.

                        Envie por favor um destes dados:
                        • Número de matrícula
                        • BI / Passaporte
                        • Nome completo
                        • Telefone cadastrado

                        Exemplo:
                        IMETRO-2026-TESTE-002
                        """.trim());
            }

            Student student = studentOptional.get();
            rememberStudent(session, metadata, student, message);
            metadata.put("expectedStep", "CONFIRM_STUDENT");
            writeMetadata(session, metadata);
            sessionRepository.save(session);

            return Optional.of(buildStudentConfirmation(student));
        }

        if ("CONFIRM_STUDENT".equalsIgnoreCase(expected)) {
            if (isYes(normalized) || "1".equals(normalized)) {
                metadata.remove("expectedStep");
                writeMetadata(session, metadata);
                sessionRepository.save(session);

                Student student = loadStudentFromMetadata(metadata);
                String pendingAction = metadata.get("pendingAction");

                if (student == null) {
                    return Optional.of(buildMainMenuGreeting());
                }

                return Optional.of(executeActionForStudent(session, metadata, fromPhone, student, pendingAction, normalized));
            }

            if ("2".equals(normalized) || containsAny(normalized, "nao", "não", "errado", "dados errados")) {
                metadata.put("expectedStep", "STUDENT_IDENTIFIER");
                metadata.remove("studentId");
                writeMetadata(session, metadata);
                sessionRepository.save(session);

                return Optional.of("""
                        Sem problema. Envie novamente o número de matrícula, BI ou nome completo para localizar o cadastro correto.
                        """.trim());
            }

            if ("3".equals(normalized) || isHumanRequestIntent(normalized)) {
                metadata.remove("expectedStep");
                metadata.put("lastIntent", "HUMAN_SUPPORT_REQUESTED");
                writeMetadata(session, metadata);
                sessionRepository.save(session);

                return Optional.of(buildHumanSupportReasonMenu());
            }

            return Optional.of("""
                    Confirma que os dados encontrados estão corretos?

                    1. Sim, sou eu
                    2. Não, os dados estão errados
                    3. Quero falar com a secretaria
                    """.trim());
        }

        if ("PAYMENT_TYPE".equalsIgnoreCase(expected)) {
            Student student = loadStudentFromMetadata(metadata);
            if (student == null) {
                metadata.put("expectedStep", "STUDENT_IDENTIFIER");
                writeMetadata(session, metadata);
                sessionRepository.save(session);
                return Optional.of(askForStudentIdentification());
            }

            if ("1".equals(normalized) || containsAny(normalized, "mensalidade do mes", "mês atual", "mes atual")) {
                metadata.remove("expectedStep");
                writeMetadata(session, metadata);
                sessionRepository.save(session);
                return Optional.of(generatePaymentGuideForCurrentMonth(session, metadata, student, normalized));
            }

            if ("2".equals(normalized) || containsAny(normalized, "atraso", "atrasada", "atrasadas")) {
                metadata.remove("expectedStep");
                writeMetadata(session, metadata);
                sessionRepository.save(session);
                return Optional.of(buildOverdueChargesReply(session, metadata, student));
            }

            if ("3".equals(normalized)) {
                return Optional.of("""
                        Propina completa do semestre ainda depende das regras financeiras oficiais da instituição.

                        Posso encaminhar para a secretaria ou gerar a mensalidade do mês atual.

                        1. Gerar mensalidade do mês atual
                        2. Falar com a secretaria
                        """.trim());
            }

            if (containsAny(normalized, "voltar", "menu")) {
                metadata.remove("expectedStep");
                writeMetadata(session, metadata);
                sessionRepository.save(session);
                return Optional.of(buildMainMenuGreeting());
            }

            return Optional.of(buildPaymentTypeMenu());
        }

        if ("CURRENT_MONTH_ACTION".equalsIgnoreCase(expected)) {
            Student student = loadStudentFromMetadata(metadata);
            if (student == null) {
                metadata.put("expectedStep", "STUDENT_IDENTIFIER");
                writeMetadata(session, metadata);
                sessionRepository.save(session);
                return Optional.of(askForStudentIdentification());
            }

            if ("1".equals(normalized) || containsAny(normalized, "gerar", "sim")) {
                metadata.remove("expectedStep");
                writeMetadata(session, metadata);
                sessionRepository.save(session);
                return Optional.of(generatePaymentGuideForCurrentMonth(session, metadata, student, normalized));
            }

            if ("2".equals(normalized) || containsAny(normalized, "formas", "pagamento")) {
                return Optional.of(buildPaymentMethodsReply());
            }

            if ("3".equals(normalized) || containsAny(normalized, "voltar", "menu")) {
                metadata.remove("expectedStep");
                writeMetadata(session, metadata);
                sessionRepository.save(session);
                return Optional.of(buildMainMenuGreeting());
            }

            return Optional.of("""
                    Deseja gerar a guia / referência de pagamento?

                    1. Sim, gerar agora
                    2. Ver formas de pagamento
                    3. Voltar ao menu principal
                    """.trim());
        }

        if ("GUIDE_ACK".equalsIgnoreCase(expected)) {
            if (isReceiptAcknowledgementIntent(normalized)) {
                metadata.put("expectedStep", "PAYMENT_CONFIRMATION");
                metadata.put("lastIntent", "PAYMENT_GUIDE_ACKNOWLEDGED");
                metadata.put("guideAcknowledgedAt", LocalDateTime.now().toString());
                writeMetadata(session, metadata);
                sessionRepository.save(session);

                return Optional.of(("""
                        Perfeito. Confirmamos o recebimento da guia de pagamento.

                        Cobrança: %s

                        Agora aguardamos o pagamento.

                        Para este teste automático, responda:
                        paguei

                        O sistema irá simular a confirmação bancária, emitir o recibo e enviar o PDF automaticamente.
                        """).formatted(firstNonBlank(metadata.get("lastChargeCode"), "-")).trim());
            }

            if (isPaymentIntent(normalized)) {
                return Optional.of(confirmAutomaticPayment(session, metadata, fromPhone, normalized));
            }

            if (isHumanRequestIntent(normalized)) {
                metadata.remove("expectedStep");
                metadata.put("lastIntent", "HUMAN_SUPPORT_REQUESTED");
                writeMetadata(session, metadata);
                sessionRepository.save(session);
                return Optional.of(buildHumanSupportReasonMenu());
            }
        }

        if ("PAYMENT_CONFIRMATION".equalsIgnoreCase(expected)) {
            if (isPaymentIntent(normalized)) {
                return Optional.of(confirmAutomaticPayment(session, metadata, fromPhone, normalized));
            }

            if (isHumanRequestIntent(normalized)) {
                metadata.remove("expectedStep");
                metadata.put("lastIntent", "HUMAN_SUPPORT_REQUESTED");
                writeMetadata(session, metadata);
                sessionRepository.save(session);
                return Optional.of(buildHumanSupportReasonMenu());
            }

            return Optional.of("""
                    Certo. Quando concluir o pagamento deste teste, responda:
                    paguei

                    Se preferir atendimento humano, responda:
                    falar com a secretaria
                    """.trim());
        }

        if ("NEGOTIATION_PLAN".equalsIgnoreCase(expected)) {
            metadata.remove("expectedStep");
            metadata.put("lastIntent", "NEGOTIATION_REQUEST_REGISTERED");
            writeMetadata(session, metadata);
            sessionRepository.save(session);

            return Optional.of(("""
                    Sua solicitação de negociação foi registrada.

                    Proposta recebida: %s
                    Protocolo: NEG-%s

                    A Secretaria Financeira irá analisar a solicitação.

                    Deseja gerar a guia da primeira parcela?
                    1. Sim
                    2. Aguardar aprovação
                    3. Falar com a secretaria
                    """).formatted(message, protocol()).trim());
        }

        if ("HUMAN_REASON".equalsIgnoreCase(expected)) {
            metadata.remove("expectedStep");
            metadata.put("lastIntent", "HUMAN_SUPPORT_REQUESTED");
            writeMetadata(session, metadata);
            sessionRepository.save(session);

            return Optional.of(("""
                    Obrigado. Sua solicitação foi encaminhada para a Secretaria Financeira.

                    Motivo informado: %s
                    Protocolo: ATD-%s

                    Horário de atendimento humano:
                    Segunda a sexta, das 08h às 17h.

                    Enquanto aguarda, você pode continuar usando o atendimento automático.
                    """).formatted(message, protocol()).trim());
        }

        return Optional.empty();
    }

    private Optional<String> handleDirectIntent(
            WhatsappSession session,
            Map<String, String> metadata,
            String fromPhone,
            String message,
            String normalized
    ) {
        if (isOption(normalized, "1") || isCurrentMonthlyIntent(normalized)) {
            return withStudent(session, metadata, "CONSULT_CURRENT_MONTH", message, student -> consultCurrentMonth(session, metadata, student));
        }

        if (isOption(normalized, "2") || isPaymentGuideIntent(normalized)) {
            return withStudent(session, metadata, "GENERATE_PAYMENT_GUIDE", message, student -> generatePaymentGuideForCurrentMonth(session, metadata, student, normalized));
        }

        if (isOption(normalized, "3") || isOverdueIntent(normalized)) {
            return withStudent(session, metadata, "VIEW_OVERDUE", message, student -> buildOverdueChargesReply(session, metadata, student));
        }

        if (isOption(normalized, "4") || isSendProofIntent(normalized)) {
            return Optional.of(askForPaymentProof(session, metadata, message));
        }

        if (isOption(normalized, "5") || isReceiptIntent(normalized)) {
            return withStudent(session, metadata, "REQUEST_RECEIPT", message, student -> buildReceiptReply(student));
        }

        if (isOption(normalized, "6") || isFinancialSummaryIntent(normalized)) {
            return withStudent(session, metadata, "FINANCIAL_SUMMARY", message, this::buildFinancialSummaryReply);
        }

        if (isOption(normalized, "7") || isNegotiationIntent(normalized)) {
            return withStudent(session, metadata, "NEGOTIATION", message, student -> buildNegotiationMenu(session, metadata, student));
        }

        if (isOption(normalized, "8") || isHumanRequestIntent(normalized)) {
            metadata.put("secretariapayFinanceFlow", "true");
            metadata.put("expectedStep", "HUMAN_REASON");
            metadata.put("lastIntent", "HUMAN_SUPPORT_REASON_REQUESTED");
            writeMetadata(session, metadata);
            sessionRepository.save(session);
            return Optional.of(buildHumanSupportReasonMenu());
        }

        if (isOption(normalized, "9") || isAcademicRequestIntent(normalized)) {
            return Optional.of(buildAcademicRequestsMenu());
        }

        if (isPaymentIntent(normalized) && hasChargeContext(metadata)) {
            return Optional.of(confirmAutomaticPayment(session, metadata, fromPhone, normalized));
        }

        return Optional.empty();
    }

    private Optional<String> withStudent(
            WhatsappSession session,
            Map<String, String> metadata,
            String pendingAction,
            String message,
            StudentAction action
    ) {
        Student student = loadStudentFromMetadata(metadata);

        if (student != null) {
            return Optional.of(action.apply(student));
        }

        Optional<Student> byPhone = findStudentByPhone(session.getPhoneNumber());

        if (byPhone.isPresent()) {
            student = byPhone.get();
            rememberStudent(session, metadata, student, message);
            metadata.put("pendingAction", pendingAction);
            metadata.put("expectedStep", "CONFIRM_STUDENT");
            writeMetadata(session, metadata);
            sessionRepository.save(session);
            return Optional.of(buildStudentConfirmation(student));
        }

        metadata.put("secretariapayFinanceFlow", "true");
        metadata.put("pendingAction", pendingAction);
        metadata.put("expectedStep", "STUDENT_IDENTIFIER");
        metadata.put("lastIntent", "WAITING_STUDENT_IDENTIFIER");
        writeMetadata(session, metadata);
        sessionRepository.save(session);

        return Optional.of(askForStudentIdentification());
    }

    private String executeActionForStudent(
            WhatsappSession session,
            Map<String, String> metadata,
            String fromPhone,
            Student student,
            String pendingAction,
            String normalized
    ) {
        if (isBlank(pendingAction)) {
            return buildMainMenuForStudent(student);
        }

        return switch (pendingAction) {
            case "CONSULT_CURRENT_MONTH" -> consultCurrentMonth(session, metadata, student);
            case "GENERATE_PAYMENT_GUIDE" -> generatePaymentGuideForCurrentMonth(session, metadata, student, normalized);
            case "VIEW_OVERDUE" -> buildOverdueChargesReply(session, metadata, student);
            case "REQUEST_RECEIPT" -> buildReceiptReply(student);
            case "FINANCIAL_SUMMARY" -> buildFinancialSummaryReply(student);
            case "NEGOTIATION" -> buildNegotiationMenu(session, metadata, student);
            default -> buildMainMenuForStudent(student);
        };
    }

    private String consultCurrentMonth(
            WhatsappSession session,
            Map<String, String> metadata,
            Student student
    ) {
        Charge charge = resolveOrCreateMonthlyCharge(student, currentReferenceMonth(), "Atendimento WhatsApp");

        metadata.put("secretariapayFinanceFlow", "true");
        metadata.put("expectedStep", "CURRENT_MONTH_ACTION");
        metadata.put("lastIntent", "CURRENT_MONTH_CONSULTED");
        rememberCharge(metadata, charge);
        writeMetadata(session, metadata);
        sessionRepository.save(session);

        return ("""
                A mensalidade referente ao mês atual é:

                Mês: %s
                Valor: %s
                Vencimento: %s
                Estado: %s

                Deseja gerar a guia / referência de pagamento?

                1. Sim, gerar agora
                2. Ver formas de pagamento
                3. Voltar ao menu principal
                """).formatted(
                humanReferenceMonth(charge.getReferenceMonth()),
                formatMoney(charge.getTotalAmount(), charge.getCurrency()),
                formatDate(charge.getDueDate()),
                formatStatus(charge.getStatus())
        ).trim();
    }

    private String generatePaymentGuideForCurrentMonth(
            WhatsappSession session,
            Map<String, String> metadata,
            Student student,
            String normalized
    ) {
        String referenceMonth = resolveReferenceMonthFromText(normalized, currentReferenceMonth());
        Charge charge = resolveOrCreateMonthlyCharge(student, referenceMonth, "Atendimento WhatsApp");
        WhatsAppCloudSendResult sendResult = sendPaymentGuideDocument(student, charge);

        metadata.put("secretariapayFinanceFlow", "true");
        metadata.put("expectedStep", "GUIDE_ACK");
        metadata.put("lastIntent", "PAYMENT_GUIDE_SENT");
        metadata.put("guideProviderMessageId", safe(sendResult.getProviderMessageId()));
        rememberStudent(metadata, student);
        rememberCharge(metadata, charge);
        writeMetadata(session, metadata);
        session.setCurrentStep(WhatsappConversationStep.SECRETARIAPAY_CHARGE_FOUND)
                .setLastMessageText("guia enviada")
                .setExpiresAt(LocalDateTime.now().plusHours(SESSION_DURATION_HOURS));
        sessionRepository.save(session);

        return ("""
                ✅ Guia de pagamento gerada com sucesso.

                Estudante: %s
                Matrícula: %s
                Descrição: %s
                Valor: %s
                Vencimento: %s
                Estado: Aguardando pagamento

                O PDF da guia foi enviado aqui no WhatsApp.

                Formas de pagamento disponíveis:
                1. Multicaixa Express
                2. Referência bancária
                3. Transferência bancária
                4. Unitel Money
                5. Afrimoney
                6. Pagamento presencial na tesouraria

                Quando receber a guia, responda: recebi
                Depois do pagamento simulado, responda: paguei
                """).formatted(
                safe(student.getFullName()),
                firstNonBlank(student.getStudentNumber(), "-"),
                safe(charge.getDescription()),
                formatMoney(charge.getTotalAmount(), charge.getCurrency()),
                formatDate(charge.getDueDate())
        ).trim();
    }

    private String buildOverdueChargesReply(
            WhatsappSession session,
            Map<String, String> metadata,
            Student student
    ) {
        LocalDate today = LocalDate.now();
        List<Charge> overdue = chargeRepository.findByStudentIdOrderByDueDateDesc(student.getId())
                .stream()
                .filter(charge -> charge.getStatus() != ChargeStatus.PAID)
                .filter(charge -> charge.getStatus() != ChargeStatus.CANCELLED)
                .filter(charge -> charge.getDueDate() != null && charge.getDueDate().isBefore(today))
                .sorted(Comparator.comparing(Charge::getDueDate))
                .toList();

        if (overdue.isEmpty()) {
            return """
                    Consultei a sua situação financeira.

                    Não encontrei mensalidades em atraso para este cadastro.

                    Deseja consultar ou gerar a guia do mês atual?
                    1. Consultar mensalidade do mês
                    2. Solicitar guia de pagamento
                    3. Voltar ao menu
                    """.trim();
        }

        BigDecimal total = overdue.stream()
                .map(charge -> charge.getTotalAmount() == null ? BigDecimal.ZERO : charge.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StringBuilder reply = new StringBuilder();
        reply.append("Consultando a sua situação financeira...\n\n");
        reply.append("Você possui mensalidades em atraso:\n\n");

        int index = 1;
        for (Charge charge : overdue) {
            reply.append(index++).append(". ")
                    .append(humanReferenceMonth(charge.getReferenceMonth()))
                    .append(" — ")
                    .append(formatMoney(charge.getTotalAmount(), charge.getCurrency()))
                    .append(" — Vencimento: ")
                    .append(formatDate(charge.getDueDate()))
                    .append("\n");
        }

        reply.append("\nTotal em aberto: ").append(formatMoney(total, "AOA"));
        reply.append("\nEstado académico: Atenção financeira\n\n");
        reply.append("Deseja regularizar agora?\n\n");
        reply.append("1. Gerar guia total\n");
        reply.append("2. Gerar guia parcial\n");
        reply.append("3. Solicitar negociação\n");
        reply.append("4. Ver consequências do atraso\n");
        reply.append("5. Falar com a secretaria");

        metadata.put("secretariapayFinanceFlow", "true");
        metadata.put("lastIntent", "OVERDUE_LISTED");
        writeMetadata(session, metadata);
        sessionRepository.save(session);

        return reply.toString().trim();
    }

    private String askForPaymentProof(
            WhatsappSession session,
            Map<String, String> metadata,
            String message
    ) {
        metadata.put("secretariapayFinanceFlow", "true");
        metadata.put("lastIntent", "WAITING_PAYMENT_PROOF");
        writeMetadata(session, metadata);
        session.setCurrentStep(WhatsappConversationStep.SECRETARIAPAY_WAITING_PAYMENT_PROOF)
                .setLastMessageText(message)
                .setExpiresAt(LocalDateTime.now().plusHours(SESSION_DURATION_HOURS));
        sessionRepository.save(session);

        return """
                Perfeito. Envie agora o comprovativo de pagamento em imagem ou PDF.

                Após o envio, farei o registo e encaminharei para validação da tesouraria.
                """.trim();
    }

    private String buildReceiptReply(Student student) {
        Optional<Receipt> lastReceipt = chargeRepository.findByStudentIdOrderByDueDateDesc(student.getId())
                .stream()
                .filter(charge -> charge.getStatus() == ChargeStatus.PAID)
                .sorted(Comparator.comparing(Charge::getPaidAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(charge -> receiptRepository.findByChargeId(charge.getId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();

        if (lastReceipt.isEmpty()) {
            return """
                    Ainda não encontrei recibos emitidos para este cadastro.

                    Se já pagou recentemente, envie o comprovativo ou aguarde a validação da tesouraria.
                    """.trim();
        }

        Receipt receipt = lastReceipt.get();
        Charge charge = receipt.getCharge();

        return ("""
                Encontrei o seu último pagamento confirmado:

                Descrição: %s
                Valor: %s
                Estado: Confirmado
                Recibo: %s

                Link do recibo PDF:
                %s

                Deseja receber novamente o recibo em PDF?
                1. Sim, enviar PDF
                2. Enviar por e-mail
                3. Voltar ao menu
                """).formatted(
                charge == null ? "Pagamento confirmado" : safe(charge.getDescription()),
                charge == null ? "-" : formatMoney(charge.getTotalAmount(), charge.getCurrency()),
                safe(receipt.getReceiptCode()),
                firstNonBlank(receipt.getPdfUrl(), "-")
        ).trim();
    }

    private String buildFinancialSummaryReply(Student student) {
        List<Charge> charges = chargeRepository.findByStudentIdOrderByDueDateDesc(student.getId());

        long paid = charges.stream().filter(charge -> charge.getStatus() == ChargeStatus.PAID).count();
        long pending = charges.stream().filter(charge -> charge.getStatus() == ChargeStatus.PENDING).count();
        long overdue = charges.stream()
                .filter(charge -> charge.getStatus() != ChargeStatus.PAID)
                .filter(charge -> charge.getStatus() != ChargeStatus.CANCELLED)
                .filter(charge -> charge.getDueDate() != null && charge.getDueDate().isBefore(LocalDate.now()))
                .count();

        BigDecimal openTotal = charges.stream()
                .filter(charge -> charge.getStatus() != ChargeStatus.PAID)
                .filter(charge -> charge.getStatus() != ChargeStatus.CANCELLED)
                .map(charge -> charge.getTotalAmount() == null ? BigDecimal.ZERO : charge.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ("""
                Resumo financeiro do estudante:

                Nome: %s
                Matrícula: %s
                Curso: %s
                Estado financeiro: %s

                Mensalidades pagas: %d
                Mensalidades pendentes: %d
                Mensalidades em atraso: %d
                Total em aberto: %s

                O que deseja fazer?

                1. Gerar guia do mês
                2. Gerar guia das mensalidades em atraso
                3. Enviar comprovativo
                4. Solicitar recibo
                5. Negociar dívida
                6. Falar com a secretaria
                """).formatted(
                safe(student.getFullName()),
                firstNonBlank(student.getStudentNumber(), "-"),
                resolveCourseName(student),
                openTotal.compareTo(BigDecimal.ZERO) > 0 ? "Com pendências" : "Regular",
                paid,
                pending,
                overdue,
                formatMoney(openTotal, "AOA")
        ).trim();
    }

    private String buildNegotiationMenu(
            WhatsappSession session,
            Map<String, String> metadata,
            Student student
    ) {
        BigDecimal openTotal = chargeRepository.findByStudentIdOrderByDueDateDesc(student.getId())
                .stream()
                .filter(charge -> charge.getStatus() != ChargeStatus.PAID)
                .filter(charge -> charge.getStatus() != ChargeStatus.CANCELLED)
                .map(charge -> charge.getTotalAmount() == null ? BigDecimal.ZERO : charge.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        metadata.put("secretariapayFinanceFlow", "true");
        metadata.put("expectedStep", "NEGOTIATION_PLAN");
        metadata.put("lastIntent", "NEGOTIATION_STARTED");
        writeMetadata(session, metadata);
        sessionRepository.save(session);

        return ("""
                Entendi. Você possui pendências financeiras no valor total de %s.

                Como deseja regularizar?

                1. Pagar tudo de uma vez
                2. Pagar uma parte agora
                3. Solicitar parcelamento
                4. Solicitar prazo adicional
                5. Enviar justificativa
                6. Falar com a secretaria

                Exemplo:
                Quero pagar em 2 parcelas.
                """).formatted(formatMoney(openTotal, "AOA")).trim();
    }

    private String confirmAutomaticPayment(
            WhatsappSession session,
            Map<String, String> metadata,
            String fromPhone,
            String normalized
    ) {
        String chargeCode = metadata.get("lastChargeCode");

        if (isBlank(chargeCode)) {
            return """
                    Para confirmar o pagamento, preciso primeiro identificar a cobrança.

                    Envie o código da cobrança ou solicite a guia novamente.
                    """.trim();
        }

        String method = resolvePaymentMethod(normalized);

        MockAutomaticPaymentRequest request = new MockAutomaticPaymentRequest()
                .setPaymentMethod(method)
                .setAmount(DEFAULT_AMOUNT)
                .setExternalTransactionId("WPP-ROTEIRO-" + method + "-" + chargeCode + "-" + System.currentTimeMillis())
                .setPayerPhone(fromPhone)
                .setBankName(resolveMockBankName(method))
                .setBankReference(chargeCode)
                .setNote("Pagamento mock confirmado automaticamente no roteiro financeiro via WhatsApp.");

        MockAutomaticPaymentResponse response = mockAutomaticPaymentService.confirmByChargeCode(
                chargeCode,
                method,
                request
        );

        metadata.put("lastIntent", "PAYMENT_CONFIRMED_AUTOMATICALLY");
        metadata.put("expectedStep", "ATTENDANCE_CLOSE");
        metadata.put("receiptCode", safe(response.getReceiptCode()));
        metadata.put("paymentMethod", safe(response.getPaymentMethod()));
        metadata.put("providerMessageId", safe(response.getProviderMessageId()));
        writeMetadata(session, metadata);
        session.setCurrentStep(WhatsappConversationStep.SECRETARIAPAY_FINISHED)
                .setLastMessageText("pagamento confirmado")
                .setExpiresAt(LocalDateTime.now().plusHours(SESSION_DURATION_HOURS));
        sessionRepository.save(session);

        return ("""
                ✅ Pagamento confirmado automaticamente.

                Cobrança: %s
                Método: %s
                Estado: %s
                Recibo: %s

                O recibo digital em PDF foi emitido e enviado aqui no WhatsApp.

                Quando terminar, responda: obrigado
                """).formatted(
                response.getChargeCode(),
                response.getPaymentMethod(),
                response.getChargeStatus(),
                response.getReceiptCode()
        ).trim();
    }

    private WhatsAppCloudSendResult sendPaymentGuideDocument(Student student, Charge charge) {
        String recipientPhone = firstNonBlank(student.getWhatsapp(), student.getPhone());

        String guideUrl = API_BASE_URL + "/api/v1/public/payment-guides/" + charge.getChargeCode() + "/pdf";
        String fileName = "guia-pagamento-" + charge.getChargeCode() + ".pdf";

        String caption = ("""
                Guia de pagamento emitida.

                Estudante: %s
                Cobrança: %s
                Valor: %s
                Vencimento: %s

                O PDF da guia segue em anexo.
                Após pagar, responda: paguei

                Link público:
                %s

                SecretáriaPay Académico
                """).formatted(
                safe(student.getFullName()),
                safe(charge.getChargeCode()),
                formatMoney(charge.getTotalAmount(), charge.getCurrency()),
                formatDate(charge.getDueDate()),
                guideUrl
        ).trim();

        return whatsAppCloudApiClient.sendDocumentByLink(
                recipientPhone,
                guideUrl,
                fileName,
                caption
        );
    }

    private Charge resolveOrCreateMonthlyCharge(Student student, String referenceMonth, String source) {
        Optional<Charge> existingOpen = chargeRepository.findByStudentIdOrderByDueDateDesc(student.getId())
                .stream()
                .filter(charge -> referenceMonth.equals(charge.getReferenceMonth()))
                .filter(charge -> charge.getStatus() != ChargeStatus.PAID)
                .filter(charge -> charge.getStatus() != ChargeStatus.CANCELLED)
                .min(Comparator.comparing(Charge::getDueDate));

        if (existingOpen.isPresent()) {
            return existingOpen.get();
        }

        LocalDate referenceDate = LocalDate.parse(referenceMonth + "-01");
        String monthName = referenceDate.getMonth()
                .getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-AO"));

        Charge charge = new Charge()
                .setStudent(student)
                .setChargeCode(generateChargeCode())
                .setDescription("Propina de " + capitalize(monthName) + " " + referenceDate.getYear() + " - " + source)
                .setReferenceMonth(referenceMonth)
                .setDueDate(LocalDate.of(referenceDate.getYear(), referenceDate.getMonthValue(), 10))
                .setAmount(DEFAULT_AMOUNT)
                .setFineAmount(BigDecimal.ZERO)
                .setInterestAmount(BigDecimal.ZERO)
                .setDiscountAmount(BigDecimal.ZERO)
                .setTotalAmount(DEFAULT_AMOUNT)
                .setCurrency("AOA")
                .setStatus(ChargeStatus.PENDING);

        return chargeRepository.save(charge);
    }

    private Student loadStudentFromMetadata(Map<String, String> metadata) {
        try {
            String studentId = metadata.get("studentId");
            if (isBlank(studentId)) {
                return null;
            }
            return studentRepository.findById(UUID.fromString(studentId)).orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void rememberStudent(WhatsappSession session, Map<String, String> metadata, Student student, String message) {
        rememberStudent(metadata, student);
        metadata.put("secretariapayFinanceFlow", "true");
        metadata.put("lastStudentInput", safe(message));
        session.setCurrentStep(WhatsappConversationStep.SECRETARIAPAY_STUDENT_FOUND)
                .setLastMessageText(message)
                .setExpiresAt(LocalDateTime.now().plusHours(SESSION_DURATION_HOURS));
    }

    private void rememberStudent(Map<String, String> metadata, Student student) {
        metadata.put("studentId", student.getId().toString());
        metadata.put("studentNumber", safe(student.getStudentNumber()));
        metadata.put("studentName", safe(student.getFullName()));
    }

    private void rememberCharge(Map<String, String> metadata, Charge charge) {
        metadata.put("lastChargeId", charge.getId().toString());
        metadata.put("lastChargeCode", charge.getChargeCode());
        metadata.put("lastReferenceMonth", safe(charge.getReferenceMonth()));
    }

    private String buildStudentConfirmation(Student student) {
        return ("""
                Encontrei este cadastro:

                Nome: %s
                Matrícula: %s
                Curso: %s
                Ano/Turma: %s
                Estado financeiro: %s

                Confirma que esses dados estão corretos?

                1. Sim, sou eu
                2. Não, os dados estão errados
                3. Quero falar com a secretaria
                """).formatted(
                safe(student.getFullName()),
                firstNonBlank(student.getStudentNumber(), "-"),
                resolveCourseName(student),
                resolveClassName(student),
                resolveFinancialStatus(student)
        ).trim();
    }

    private String buildMainMenuGreeting() {
        return """
                Boa tarde! 👋
                Sou a Secretária Financeira Virtual da Universidade.

                Posso ajudar você com pagamentos, propinas, guias, comprovativos, recibos, mensalidades em atraso e outros serviços financeiros académicos.

                Para começar, escolha uma opção:

                1. Consultar mensalidade do mês
                2. Solicitar guia / referência de pagamento
                3. Ver mensalidades em atraso
                4. Enviar comprovativo de pagamento
                5. Solicitar recibo
                6. Ver situação financeira
                7. Negociar dívida ou atraso
                8. Falar com a secretaria
                9. Outras solicitações académicas

                Você pode responder com o número da opção.
                """.trim();
    }

    private String buildMainMenuForStudent(Student student) {
        return ("""
                Perfeito, %s.

                O que deseja fazer agora?

                1. Consultar mensalidade do mês
                2. Solicitar guia / referência de pagamento
                3. Ver mensalidades em atraso
                4. Enviar comprovativo de pagamento
                5. Solicitar recibo
                6. Ver situação financeira
                7. Negociar dívida ou atraso
                8. Falar com a secretaria
                9. Outras solicitações académicas
                """).formatted(firstName(student.getFullName())).trim();
    }

    private String askForStudentIdentification() {
        return """
                Antes de continuar, preciso identificar você.

                Por favor, envie uma das opções abaixo:

                • Número de matrícula
                • BI / Passaporte
                • Nome completo
                • E-mail académico
                • Número de telefone cadastrado

                Exemplo:
                IMETRO-2026-TESTE-002
                """.trim();
    }

    private String buildPaymentTypeMenu() {
        return """
                Qual pagamento deseja solicitar?

                1. Mensalidade do mês atual
                2. Mensalidade em atraso
                3. Propina completa do semestre
                4. Taxa de matrícula
                5. Taxa de exame
                6. Taxa de declaração
                7. Taxa de certificado
                8. Outro pagamento
                """.trim();
    }

    private String buildPaymentMethodsReply() {
        return """
                Formas de pagamento disponíveis:

                1. Multicaixa Express
                2. Referência bancária
                3. Transferência bancária
                4. Unitel Money
                5. Afrimoney
                6. Pagamento presencial na tesouraria

                Após o pagamento, envie o comprovativo por aqui ou, no teste automático, responda: paguei
                """.trim();
    }

    private String buildHumanSupportReasonMenu() {
        return """
                Certo. Vou encaminhar o seu atendimento para a Secretaria Financeira.

                Antes disso, informe o motivo:

                1. Pagamento não confirmado
                2. Guia com erro
                3. Valor incorreto
                4. Solicitação urgente
                5. Negociação especial
                6. Problema com comprovativo
                7. Outro assunto
                """.trim();
    }

    private String buildAcademicRequestsMenu() {
        return """
                Além dos serviços financeiros, posso ajudar com outras solicitações da secretaria universitária.

                Escolha uma opção:

                1. Declaração de frequência
                2. Declaração de matrícula
                3. Histórico académico
                4. Inscrição em exame
                5. Renovação de matrícula
                6. Atualização de dados pessoais
                7. Calendário académico
                8. Situação de documentos
                9. Falar com a secretaria académica
                """.trim();
    }

    private String buildFallbackMenu() {
        return """
                Desculpe, não consegui entender totalmente.

                Você deseja ajuda com:

                1. Guia / pagamento
                2. Mensalidade em atraso
                3. Comprovativo
                4. Recibo
                5. Situação financeira
                6. Secretaria académica
                7. Falar com atendente
                """.trim();
    }

    private String closeAttendance(WhatsappSession session, Map<String, String> metadata, String message) {
        metadata.put("lastIntent", "ATTENDANCE_CLOSED_BY_STUDENT");
        metadata.put("closedAt", LocalDateTime.now().toString());
        metadata.remove("expectedStep");
        writeMetadata(session, metadata);
        session.setCurrentStep(WhatsappConversationStep.SECRETARIAPAY_FINISHED)
                .setLastMessageText(message)
                .setExpiresAt(LocalDateTime.now().plusHours(SESSION_DURATION_HOURS));
        sessionRepository.save(session);

        return """
                Nós é que agradecemos.

                O atendimento foi encerrado automaticamente.
                Sempre que precisar, envie nova mensagem por aqui.

                SecretáriaPay Académico
                """.trim();
    }

    private Optional<Student> findStudentByPhone(String fromPhone) {
        String sanitized = sanitizePhone(fromPhone);

        if (sanitized.isBlank()) {
            return Optional.empty();
        }

        List<String> variants = List.of(
                sanitized,
                "+" + sanitized,
                sanitized.replaceFirst("^244", "0"),
                sanitized.replaceFirst("^55", "0")
        );

        for (String variant : variants) {
            Optional<Student> byWhatsapp = studentRepository.findByWhatsapp(variant);
            if (byWhatsapp.isPresent()) {
                return byWhatsapp;
            }

            Optional<Student> byPhone = studentRepository.findByPhone(variant);
            if (byPhone.isPresent()) {
                return byPhone;
            }
        }

        return Optional.empty();
    }

    private Optional<Student> findStudentByIdentifierOrName(String message) {
        String clean = safe(message).trim();

        if (clean.isBlank()) {
            return Optional.empty();
        }

        Optional<String> bi = extractBi(clean);
        if (bi.isPresent()) {
            Optional<Student> byDocument = studentRepository.findByDocumentNumberIgnoreCase(bi.get());
            if (byDocument.isPresent()) {
                return byDocument;
            }
        }

        String withoutPrefix = clean
                .replaceFirst("(?i)^MATR[IÍ]CULA\\s*[:\\-]?\\s*", "")
                .replaceFirst("(?i)^BI\\s*[:\\-]?\\s*", "")
                .replaceFirst("(?i)^N[º°.]?\\s*[:\\-]?\\s*", "")
                .replaceFirst("(?i)^ESTUDANTE\\s*[:\\-]?\\s*", "")
                .trim();

        Optional<Student> byStudentNumber = studentRepository.findByStudentNumber(withoutPrefix);
        if (byStudentNumber.isPresent()) {
            return byStudentNumber;
        }

        Optional<Student> byDocument = studentRepository.findByDocumentNumberIgnoreCase(withoutPrefix);
        if (byDocument.isPresent()) {
            return byDocument;
        }

        Matcher studentNumberMatcher = STUDENT_NUMBER_PATTERN.matcher(withoutPrefix);
        if (studentNumberMatcher.find()) {
            Optional<Student> byExtractedStudentNumber = studentRepository.findByStudentNumber(studentNumberMatcher.group());
            if (byExtractedStudentNumber.isPresent()) {
                return byExtractedStudentNumber;
            }
        }

        if (withoutPrefix.length() >= 8 && withoutPrefix.split("\\s+").length >= 2) {
            List<Student> students = studentRepository.findTop5ByFullNameContainingIgnoreCaseOrderByFullNameAsc(withoutPrefix);
            if (students.size() == 1) {
                return Optional.of(students.get(0));
            }
        }

        return Optional.empty();
    }

    private Optional<String> extractBi(String message) {
        Matcher matcher = BI_PATTERN.matcher(safe(message));

        if (matcher.find()) {
            return Optional.of(matcher.group().toUpperCase(Locale.ROOT));
        }

        return Optional.empty();
    }

    private WhatsappSession getOrCreateSession(String fromPhone) {
        String phone = sanitizePhone(fromPhone);
        Optional<WhatsappSession> existing = sessionRepository
                .findFirstByPhoneNumberAndSessionTypeAndStatusOrderByUpdatedAtDesc(
                        phone,
                        WhatsappSessionType.SECRETARIAPAY_ACADEMICO,
                        WhatsappSessionStatus.ACTIVE
                );

        if (existing.isPresent()) {
            return existing.get();
        }

        WhatsappSession session = new WhatsappSession()
                .setPhoneNumber(phone)
                .setSessionType(WhatsappSessionType.SECRETARIAPAY_ACADEMICO)
                .setStatus(WhatsappSessionStatus.ACTIVE)
                .setCurrentStep(WhatsappConversationStep.SECRETARIAPAY_START)
                .setExpiresAt(LocalDateTime.now().plusHours(SESSION_DURATION_HOURS));

        return sessionRepository.save(session);
    }

    private Map<String, String> readMetadata(WhatsappSession session) {
        if (session == null || session.getMetadata() == null || session.getMetadata().isBlank()) {
            return new LinkedHashMap<>();
        }

        try {
            return objectMapper.readValue(
                    session.getMetadata(),
                    new TypeReference<LinkedHashMap<String, String>>() {
                    }
            );
        } catch (Exception ignored) {
            return new LinkedHashMap<>();
        }
    }

    private void writeMetadata(WhatsappSession session, Map<String, String> metadata) {
        if (session == null) {
            return;
        }

        try {
            session.setMetadata(objectMapper.writeValueAsString(metadata));
        } catch (Exception ignored) {
            session.setMetadata("{}");
        }
    }

    private void resetFinancialMetadata(WhatsappSession session, Map<String, String> metadata, String message) {
        metadata.clear();
        metadata.put("secretariapayFinanceFlow", "true");
        metadata.put("lastIntent", "MAIN_MENU");
        writeMetadata(session, metadata);
        session.setCurrentStep(WhatsappConversationStep.SECRETARIAPAY_START)
                .setLastMessageText(message)
                .setExpiresAt(LocalDateTime.now().plusHours(SESSION_DURATION_HOURS));
        sessionRepository.save(session);
    }

    private boolean isSecretariaPayFinancialContext(Map<String, String> metadata) {
        return metadata != null && "true".equalsIgnoreCase(metadata.get("secretariapayFinanceFlow"));
    }

    private boolean hasChargeContext(Map<String, String> metadata) {
        return metadata != null && !isBlank(metadata.get("lastChargeCode"));
    }

    private String resolveCourseName(Student student) {
        if (student == null || student.getAcademicClass() == null) {
            return "-";
        }

        AcademicClass academicClass = student.getAcademicClass();
        Course course = academicClass.getCourse();
        return course == null ? "-" : firstNonBlank(course.getName(), "-");
    }

    private String resolveClassName(Student student) {
        if (student == null || student.getAcademicClass() == null) {
            return "-";
        }

        return firstNonBlank(student.getAcademicClass().getName(), "-");
    }

    private String resolveFinancialStatus(Student student) {
        if (student != null && Boolean.TRUE.equals(student.getFinanciallyBlocked())) {
            return "Com restrição financeira";
        }

        if (student == null || student.getId() == null) {
            return "Não informado";
        }

        boolean hasOpenCharge = chargeRepository.findByStudentIdOrderByDueDateDesc(student.getId())
                .stream()
                .anyMatch(charge -> charge.getStatus() != ChargeStatus.PAID && charge.getStatus() != ChargeStatus.CANCELLED);

        return hasOpenCharge ? "Com pendências" : "Regular";
    }

    private String currentReferenceMonth() {
        LocalDate now = LocalDate.now();
        return "%04d-%02d".formatted(now.getYear(), now.getMonthValue());
    }

    private String resolveReferenceMonthFromText(String normalized, String fallback) {
        LocalDate now = LocalDate.now();

        Map<String, Integer> months = Map.ofEntries(
                Map.entry("janeiro", 1),
                Map.entry("fevereiro", 2),
                Map.entry("marco", 3),
                Map.entry("março", 3),
                Map.entry("abril", 4),
                Map.entry("maio", 5),
                Map.entry("junho", 6),
                Map.entry("julho", 7),
                Map.entry("agosto", 8),
                Map.entry("setembro", 9),
                Map.entry("outubro", 10),
                Map.entry("novembro", 11),
                Map.entry("dezembro", 12)
        );

        for (Map.Entry<String, Integer> entry : months.entrySet()) {
            if (normalized.contains(normalize(entry.getKey()))) {
                return "%04d-%02d".formatted(now.getYear(), entry.getValue());
            }
        }

        if (normalized.contains("proximo mes") || normalized.contains("próximo mês")) {
            LocalDate nextMonth = now.plusMonths(1);
            return "%04d-%02d".formatted(nextMonth.getYear(), nextMonth.getMonthValue());
        }

        return firstNonBlank(fallback, currentReferenceMonth());
    }

    private String humanReferenceMonth(String referenceMonth) {
        try {
            LocalDate date = LocalDate.parse(referenceMonth + "-01");
            String month = date.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-AO"));
            return capitalize(month) + " de " + date.getYear();
        } catch (Exception ignored) {
            return firstNonBlank(referenceMonth, "mês atual");
        }
    }

    private String resolvePaymentMethod(String normalized) {
        if (containsAny(normalized, "outro banco", "banco diferente", "iban outro")) {
            return "IBAN_OUTRO_BANCO";
        }

        if (containsAny(normalized, "iban", "transferencia", "transferência", "mesmo banco")) {
            return "IBAN_MESMO_BANCO";
        }

        if (containsAny(normalized, "deposito", "depósito")) {
            return "DEPOSITO_BANCARIO";
        }

        if (containsAny(normalized, "unitel", "unitel money")) {
            return "UNITEL_MONEY";
        }

        if (containsAny(normalized, "afrimoney", "afri money")) {
            return "AFRIMONEY";
        }

        return "MULTICAIXA_EXPRESS";
    }

    private String resolveMockBankName(String method) {
        return switch (method) {
            case "IBAN_OUTRO_BANCO" -> "Banco de origem diferente - Mock";
            case "IBAN_MESMO_BANCO", "MULTICAIXA_EXPRESS", "DEPOSITO_BANCARIO" -> "Banco Angolano de Investimento - Mock";
            case "UNITEL_MONEY" -> "Unitel Money - Mock";
            case "AFRIMONEY" -> "Afrimoney - Mock";
            default -> "Integração bancária mock";
        };
    }

    private String generateChargeCode() {
        String code;

        do {
            code = "CHG" + System.currentTimeMillis();
        } while (chargeRepository.existsByChargeCode(code));

        return code;
    }

    private boolean isTextLike(String type) {
        return "text".equalsIgnoreCase(type)
                || "button".equalsIgnoreCase(type)
                || "interactive".equalsIgnoreCase(type)
                || "unknown".equalsIgnoreCase(type);
    }

    private boolean isMainMenuIntent(String normalized) {
        return containsAny(normalized, "menu", "inicio", "início", "comecar", "começar", "opcoes", "opções");
    }

    private boolean isRestartIntent(String normalized) {
        return containsAny(normalized, "reiniciar", "recomecar", "recomeçar", "voltar ao inicio", "voltar ao início");
    }

    private boolean isGreetingIntent(String normalized) {
        return containsAny(normalized, "ola", "olá", "bom dia", "boa tarde", "boa noite");
    }

    private boolean isCurrentMonthlyIntent(String normalized) {
        return containsAny(normalized, "mensalidade do mes", "mensalidade do mês", "propina do mes", "propina do mês", "consultar mensalidade", "mensalidade atual");
    }

    private boolean isPaymentGuideIntent(String normalized) {
        return containsAny(normalized, "boleto", "guia", "referencia", "referência", "gerar boleto", "solicitar boleto", "quero pagar", "pagar a mensalidade", "pagar propina");
    }

    private boolean isOverdueIntent(String normalized) {
        return containsAny(normalized, "atraso", "atrasadas", "atrasado", "em atraso", "divida", "dívida", "devendo", "quanto devo");
    }

    private boolean isSendProofIntent(String normalized) {
        return containsAny(normalized, "comprovativo", "comprovante", "enviar comprovativo", "ja paguei", "já paguei", "meu pagamento nao aparece", "meu pagamento não aparece");
    }

    private boolean isReceiptIntent(String normalized) {
        return containsAny(normalized, "recibo", "comprovante fiscal", "último pagamento", "ultimo pagamento");
    }

    private boolean isFinancialSummaryIntent(String normalized) {
        return containsAny(normalized, "situacao financeira", "situação financeira", "estou regular", "regular", "resumo financeiro", "pendencia", "pendência");
    }

    private boolean isNegotiationIntent(String normalized) {
        return containsAny(normalized, "negociar", "parcelar", "parcelamento", "nao consigo pagar", "não consigo pagar", "prazo", "pagar uma parte");
    }

    private boolean isHumanRequestIntent(String normalized) {
        return containsAny(normalized, "humano", "atendente", "secretaria", "falar com alguem", "falar com alguém", "tesouraria");
    }

    private boolean isAcademicRequestIntent(String normalized) {
        return containsAny(normalized, "declaração", "declaracao", "historico", "histórico", "exame", "matricula", "matrícula", "calendario", "calendário");
    }

    private boolean isReceiptAcknowledgementIntent(String normalized) {
        return containsAny(normalized, "recebi", "recebido", "ja recebi", "já recebi", "ok recebi", "guia recebida");
    }

    private boolean isPaymentIntent(String normalized) {
        return containsAny(normalized, "paguei", "ja paguei", "já paguei", "pago", "pagamento feito", "pagamento realizado", "paguei multicaixa", "simular pagamento", "pagamento simulado");
    }

    private boolean isThanksIntent(String normalized) {
        return containsAny(normalized, "obrigado", "obrigada", "valeu", "muito obrigado", "muito obrigada", "finalizar", "encerrar");
    }

    private boolean isConfusingMessage(String normalized) {
        return normalized.length() >= 3;
    }

    private boolean isOption(String normalized, String option) {
        return option.equals(normalized);
    }

    private boolean isYes(String normalized) {
        return containsAny(normalized, "sim", "sou eu", "correto", "certo", "confirmo", "confirmar");
    }

    private String formatMoney(BigDecimal value, String currency) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        String safeCurrency = isBlank(currency) ? "AOA" : currency;

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("pt-AO"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);

        if ("AOA".equalsIgnoreCase(safeCurrency)) {
            return formatter.format(safeValue) + " Kz";
        }

        return formatter.format(safeValue) + " " + safeCurrency.toUpperCase(Locale.ROOT);
    }

    private String formatDate(LocalDate date) {
        if (date == null) {
            return "não informado";
        }
        return DATE_FORMATTER.format(date);
    }

    private String formatStatus(ChargeStatus status) {
        if (status == null) {
            return "não informado";
        }

        return switch (status) {
            case PENDING -> "Pendente";
            case OVERDUE -> "Em atraso";
            case PAID -> "Pago";
            case CANCELLED -> "Cancelado";
            case RENEGOTIATED -> "Renegociado";
            case PARTIALLY_PAID -> "Parcialmente pago";
        };
    }

    private String protocol() {
        return String.valueOf(System.currentTimeMillis()).substring(3);
    }

    private String firstName(String fullName) {
        String clean = safe(fullName).trim();
        if (clean.isBlank()) {
            return "estudante";
        }
        return clean.split("\\s+")[0];
    }

    private String sanitizePhone(String phone) {
        if (phone == null) {
            return "";
        }

        return phone.replace("+", "")
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "")
                .trim();
    }

    private boolean containsAny(String value, String... keywords) {
        if (value == null || value.isBlank()) {
            return false;
        }

        for (String keyword : keywords) {
            if (value.contains(normalize(keyword))) {
                return true;
            }
        }

        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return normalized
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }

        return "";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    @FunctionalInterface
    private interface StudentAction {
        String apply(Student student);
    }
}
