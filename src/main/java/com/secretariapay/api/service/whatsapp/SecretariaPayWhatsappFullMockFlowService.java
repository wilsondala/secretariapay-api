package com.secretariapay.api.service.whatsapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secretariapay.api.dto.financial.MockAutomaticPaymentRequest;
import com.secretariapay.api.dto.financial.MockAutomaticPaymentResponse;
import com.secretariapay.api.entity.WhatsappSession;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.WhatsappConversationStep;
import com.secretariapay.api.entity.enums.WhatsappSessionStatus;
import com.secretariapay.api.entity.enums.WhatsappSessionType;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.repository.WhatsappSessionRepository;
import com.secretariapay.api.repository.academic.StudentRepository;
import com.secretariapay.api.repository.financial.ChargeRepository;
import com.secretariapay.api.service.financial.SecretariaPayMockAutomaticPaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class SecretariaPayWhatsappFullMockFlowService {

    private static final String API_BASE_URL = "https://secretariapay-api.paixaoangola.com";
    private static final BigDecimal DEFAULT_AMOUNT = new BigDecimal("45000.00");

    private final StudentRepository studentRepository;
    private final ChargeRepository chargeRepository;
    private final WhatsappSessionRepository sessionRepository;
    private final WhatsAppCloudApiClient whatsAppCloudApiClient;
    private final SecretariaPayMockAutomaticPaymentService mockAutomaticPaymentService;
    private final ObjectMapper objectMapper;

    public SecretariaPayWhatsappFullMockFlowService(
            StudentRepository studentRepository,
            ChargeRepository chargeRepository,
            WhatsappSessionRepository sessionRepository,
            WhatsAppCloudApiClient whatsAppCloudApiClient,
            SecretariaPayMockAutomaticPaymentService mockAutomaticPaymentService
    ) {
        this.studentRepository = studentRepository;
        this.chargeRepository = chargeRepository;
        this.sessionRepository = sessionRepository;
        this.whatsAppCloudApiClient = whatsAppCloudApiClient;
        this.mockAutomaticPaymentService = mockAutomaticPaymentService;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public Optional<String> handle(
            String fromPhone,
            String messageType,
            String rawMessage
    ) {
        String type = safe(messageType).toLowerCase(Locale.ROOT);
        String message = safe(rawMessage).trim();
        String normalized = normalize(message);

        if (!isTextLike(type) || normalized.isBlank()) {
            return Optional.empty();
        }

        WhatsappSession session = getOrCreateSession(fromPhone);
        Map<String, String> metadata = readMetadata(session);

        if (isThanksIntent(normalized) && isFullMockContext(metadata)) {
            return Optional.of(closeAttendance(session, metadata));
        }

        if (isGuideRequestIntent(normalized)) {
            Optional<Student> studentOptional = findStudentByPhone(fromPhone);

            if (studentOptional.isEmpty()) {
                metadata.put("fullMockFlow", "true");
                metadata.put("lastIntent", "WAITING_STUDENT_IDENTIFIER_FOR_PAYMENT_GUIDE");
                writeMetadata(session, metadata);
                session.setCurrentStep(WhatsappConversationStep.SECRETARIAPAY_WAITING_IDENTIFIER)
                        .setLastMessageText(message)
                        .setExpiresAt(LocalDate.now().atStartOfDay().plusDays(2));
                sessionRepository.save(session);

                return Optional.of("""
                        Claro. Para emitir a sua guia de pagamento, envie o seu número de estudante ou BI.

                        Exemplo:
                        IMETRO-2026-TESTE-002
                        """.trim());
            }

            Student student = studentOptional.get();
            Charge charge = resolveOrCreateMockCharge(student, normalized);
            sendPaymentGuideDocument(student, charge);
            rememberGuideSent(session, metadata, student, charge, message);

            return Optional.of(buildGuideSentReply(student, charge));
        }

        if (isReceiptAcknowledgementIntent(normalized) && isGuideContext(metadata)) {
            return Optional.of(acknowledgePaymentGuide(session, metadata));
        }

        if (isMockPaymentByConversationIntent(normalized, metadata)) {
            String chargeCode = metadata.get("lastChargeCode");

            if (chargeCode == null || chargeCode.isBlank()) {
                return Optional.empty();
            }

            String method = resolveMockPaymentMethod(normalized);
            MockAutomaticPaymentResponse paymentResponse = confirmMockPaymentFromConversation(
                    fromPhone,
                    chargeCode,
                    method
            );

            metadata.put("lastIntent", "MOCK_PAYMENT_CONFIRMED_BY_WHATSAPP");
            metadata.put("paymentMethod", safe(paymentResponse.getPaymentMethod()));
            metadata.put("receiptCode", safe(paymentResponse.getReceiptCode()));
            metadata.put("receiptMessageStatus", safe(paymentResponse.getReceiptMessageStatus()));
            metadata.put("providerMessageId", safe(paymentResponse.getProviderMessageId()));
            writeMetadata(session, metadata);

            session.setCurrentStep(WhatsappConversationStep.SECRETARIAPAY_FINISHED)
                    .setLastMessageText(message)
                    .setExpiresAt(LocalDate.now().atStartOfDay().plusDays(2));
            sessionRepository.save(session);

            return Optional.of(buildMockPaymentConfirmedReply(paymentResponse));
        }

        return Optional.empty();
    }

    private Charge resolveOrCreateMockCharge(Student student, String normalizedMessage) {
        LocalDate referenceDate = resolveReferenceDate(normalizedMessage);
        String referenceMonth = "%04d-%02d".formatted(referenceDate.getYear(), referenceDate.getMonthValue());

        Optional<Charge> existingOpen = chargeRepository.findByStudentIdOrderByDueDateDesc(student.getId())
                .stream()
                .filter(charge -> referenceMonth.equals(charge.getReferenceMonth()))
                .filter(charge -> charge.getStatus() != ChargeStatus.PAID)
                .filter(charge -> charge.getStatus() != ChargeStatus.CANCELLED)
                .min(Comparator.comparing(Charge::getDueDate));

        if (existingOpen.isPresent()) {
            return existingOpen.get();
        }

        String monthName = referenceDate.getMonth()
                .getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-AO"));

        Charge charge = new Charge()
                .setStudent(student)
                .setChargeCode(generateChargeCode())
                .setDescription("Propina de " + capitalize(monthName) + " " + referenceDate.getYear() + " - Atendimento WhatsApp Mock")
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

    private void sendPaymentGuideDocument(Student student, Charge charge) {
        String guideUrl = API_BASE_URL + "/api/v1/public/payment-guides/" + charge.getChargeCode() + "/pdf";
        String fileName = "guia-pagamento-" + charge.getChargeCode() + ".pdf";

        String caption = ("""
                Guia de pagamento emitida.

                Estudante: %s
                Cobrança: %s
                Valor: %s
                Vencimento: %s

                O PDF da guia segue em anexo.
                Após pagar, envie: paguei

                Link público:
                %s

                SecretáriaPay Académico
                """).formatted(
                safe(student.getFullName()),
                safe(charge.getChargeCode()),
                formatMoney(charge.getTotalAmount(), charge.getCurrency()),
                charge.getDueDate(),
                guideUrl
        ).trim();

        whatsAppCloudApiClient.sendDocumentByLink(
                firstNonBlank(student.getWhatsapp(), student.getPhone(), ""),
                guideUrl,
                fileName,
                caption
        );
    }

    private MockAutomaticPaymentResponse confirmMockPaymentFromConversation(
            String fromPhone,
            String chargeCode,
            String method
    ) {
        MockAutomaticPaymentRequest request = new MockAutomaticPaymentRequest()
                .setPaymentMethod(method)
                .setAmount(DEFAULT_AMOUNT)
                .setExternalTransactionId("WPP-FULL-FLOW-" + method + "-" + chargeCode + "-" + System.currentTimeMillis())
                .setPayerPhone(fromPhone)
                .setBankName(resolveMockBankName(method))
                .setBankReference(chargeCode)
                .setNote("Pagamento mock confirmado automaticamente durante atendimento completo via WhatsApp.");

        return mockAutomaticPaymentService.confirmByChargeCode(
                chargeCode,
                method,
                request
        );
    }

    private void rememberGuideSent(
            WhatsappSession session,
            Map<String, String> metadata,
            Student student,
            Charge charge,
            String lastMessage
    ) {
        metadata.put("fullMockFlow", "true");
        metadata.put("contextSource", "WHATSAPP_FULL_MOCK_FLOW");
        metadata.put("lastIntent", "PAYMENT_GUIDE_SENT_BY_WHATSAPP");
        metadata.put("lastStudentId", student.getId().toString());
        metadata.put("lastStudentNumber", safe(student.getStudentNumber()));
        metadata.put("lastStudentName", safe(student.getFullName()));
        metadata.put("lastChargeId", charge.getId().toString());
        metadata.put("lastChargeCode", charge.getChargeCode());
        metadata.put("lastReferenceMonth", safe(charge.getReferenceMonth()));

        writeMetadata(session, metadata);

        session.setCurrentStep(WhatsappConversationStep.SECRETARIAPAY_CHARGE_FOUND)
                .setLastMessageText(lastMessage)
                .setExpiresAt(LocalDate.now().atStartOfDay().plusDays(2));

        sessionRepository.save(session);
    }

    private String buildGuideSentReply(Student student, Charge charge) {
        return ("""
                Perfeito. Emiti a sua guia de pagamento.

                Estudante: %s
                Cobrança: %s
                Valor: %s
                Vencimento: %s

                O PDF foi enviado aqui no WhatsApp.

                Quando receber a guia, responda: recebi
                Depois do pagamento simulado, responda: paguei
                """).formatted(
                safe(student.getFullName()),
                safe(charge.getChargeCode()),
                formatMoney(charge.getTotalAmount(), charge.getCurrency()),
                charge.getDueDate()
        ).trim();
    }

    private String acknowledgePaymentGuide(WhatsappSession session, Map<String, String> metadata) {
        String chargeCode = metadata.get("lastChargeCode");

        metadata.put("lastIntent", "PAYMENT_GUIDE_ACKNOWLEDGED");
        metadata.put("guideAcknowledgedAt", java.time.LocalDateTime.now().toString());
        writeMetadata(session, metadata);

        session.setCurrentStep(WhatsappConversationStep.SECRETARIAPAY_CHARGE_FOUND)
                .setLastMessageText("recebi")
                .setExpiresAt(LocalDate.now().atStartOfDay().plusDays(2));
        sessionRepository.save(session);

        return ("""
                Perfeito. Confirmamos o recebimento da guia de pagamento.

                Cobrança: %s

                Agora aguardamos o pagamento.

                Para este teste de atendimento automático, responda:
                paguei

                O sistema irá simular a confirmação bancária, emitir o recibo e enviar o PDF automaticamente.
                """).formatted(firstNonBlank(chargeCode, "-")).trim();
    }

    private String buildMockPaymentConfirmedReply(MockAutomaticPaymentResponse response) {
        return ("""
                Pagamento confirmado automaticamente.

                Cobrança: %s
                Método: %s
                Estado: %s
                Recibo: %s

                O recibo digital em PDF foi emitido e enviado aqui no WhatsApp.

                Quando terminar, responda: obrigado
                """).formatted(
                safe(response.getChargeCode()),
                safe(response.getPaymentMethod()),
                safe(response.getChargeStatus()),
                safe(response.getReceiptCode())
        ).trim();
    }

    private String closeAttendance(WhatsappSession session, Map<String, String> metadata) {
        metadata.put("lastIntent", "ATTENDANCE_CLOSED_BY_STUDENT");
        metadata.put("closedAt", java.time.LocalDateTime.now().toString());
        writeMetadata(session, metadata);

        session.setCurrentStep(WhatsappConversationStep.SECRETARIAPAY_FINISHED)
                .setLastMessageText("obrigado")
                .setExpiresAt(LocalDate.now().atStartOfDay().plusDays(1));
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
                .setExpiresAt(LocalDate.now().atStartOfDay().plusDays(1));

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

    private boolean isTextLike(String type) {
        return "text".equalsIgnoreCase(type)
                || "button".equalsIgnoreCase(type)
                || "interactive".equalsIgnoreCase(type)
                || "unknown".equalsIgnoreCase(type);
    }

    private boolean isGuideRequestIntent(String normalized) {
        return containsAny(normalized, "boleto", "guia", "referencia", "referência", "cobranca", "cobrança")
                && containsAny(normalized, "mes", "mês", "propina", "mensalidade", "pagamento", "pagar", "deste mes", "desse mes");
    }

    private boolean isReceiptAcknowledgementIntent(String normalized) {
        return containsAny(normalized, "recebi", "recebido", "ja recebi", "já recebi", "ok recebi", "guia recebida");
    }

    private boolean isMockPaymentByConversationIntent(String normalized, Map<String, String> metadata) {
        if (!isFullMockContext(metadata)) {
            return false;
        }

        if (metadata.get("lastChargeCode") == null || metadata.get("lastChargeCode").isBlank()) {
            return false;
        }

        boolean afterGuide = "PAYMENT_GUIDE_SENT_BY_WHATSAPP".equalsIgnoreCase(metadata.get("lastIntent"))
                || "PAYMENT_GUIDE_ACKNOWLEDGED".equalsIgnoreCase(metadata.get("lastIntent"));

        return afterGuide && containsAny(
                normalized,
                "paguei",
                "ja paguei",
                "já paguei",
                "pago",
                "pagamento feito",
                "paguei multicaixa",
                "simular pagamento",
                "pagamento simulado"
        );
    }

    private boolean isThanksIntent(String normalized) {
        return containsAny(
                normalized,
                "obrigado",
                "obrigada",
                "valeu",
                "muito obrigado",
                "muito obrigada",
                "finalizar",
                "encerrar"
        );
    }

    private boolean isGuideContext(Map<String, String> metadata) {
        return isFullMockContext(metadata)
                && "PAYMENT_GUIDE_SENT_BY_WHATSAPP".equalsIgnoreCase(metadata.get("lastIntent"));
    }

    private boolean isFullMockContext(Map<String, String> metadata) {
        return metadata != null && "true".equalsIgnoreCase(metadata.get("fullMockFlow"));
    }

    private LocalDate resolveReferenceDate(String normalizedMessage) {
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
            if (normalizedMessage.contains(normalize(entry.getKey()))) {
                return LocalDate.of(now.getYear(), entry.getValue(), 1);
            }
        }

        if (normalizedMessage.contains("proximo mes") || normalizedMessage.contains("próximo mês")) {
            return now.plusMonths(1).withDayOfMonth(1);
        }

        return now.withDayOfMonth(1);
    }

    private String resolveMockPaymentMethod(String normalized) {
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

    private String formatMoney(BigDecimal value, String currency) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        String safeCurrency = currency == null || currency.isBlank() ? "AOA" : currency;
        return safeValue.stripTrailingZeros().toPlainString() + " " + safeCurrency;
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

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
