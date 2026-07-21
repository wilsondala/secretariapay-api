package com.secretariapay.api.service.whatsapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secretariapay.api.entity.WhatsappSession;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.WhatsappConversationStep;
import com.secretariapay.api.entity.enums.WhatsappSessionStatus;
import com.secretariapay.api.entity.enums.WhatsappSessionType;
import com.secretariapay.api.entity.enums.financial.PaymentProofStatus;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.entity.financial.PaymentProof;
import com.secretariapay.api.repository.WhatsappSessionRepository;
import com.secretariapay.api.repository.financial.ChargeRepository;
import com.secretariapay.api.repository.financial.PaymentProofRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class SecretariaPayWhatsappConversationContextService {

    private static final int SESSION_DURATION_HOURS = 24;

    private final WhatsappSessionRepository sessionRepository;
    private final ChargeRepository chargeRepository;
    private final PaymentProofRepository paymentProofRepository;
    private final ObjectMapper objectMapper;

    public SecretariaPayWhatsappConversationContextService(
            WhatsappSessionRepository sessionRepository,
            ChargeRepository chargeRepository,
            PaymentProofRepository paymentProofRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.chargeRepository = chargeRepository;
        this.paymentProofRepository = paymentProofRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public Optional<String> resolveContextualReply(
            String fromPhone,
            String messageType,
            String rawMessage
    ) {
        return resolveContextualReply(
                fromPhone,
                messageType,
                rawMessage,
                null,
                null,
                null
        );
    }

    @Transactional
    public Optional<String> resolveContextualReply(
            String fromPhone,
            String messageType,
            String rawMessage,
            String mediaId,
            String fileName,
            String mimeType
    ) {
        WhatsappSession session = getOrCreateSession(fromPhone);

        String type = safe(messageType).toLowerCase(Locale.ROOT);
        String message = safe(rawMessage).trim();
        String normalized = normalize(message);

        if (isRestartIntent(normalized)) {
            resetSession(session, message);
            return Optional.empty();
        }

        if ("image".equals(type) || "document".equals(type)) {
            return handleIncomingPaymentProof(
                    session,
                    type,
                    mediaId,
                    fileName,
                    mimeType
            );
        }

        WhatsappConversationStep step = session.getCurrentStep();

        if (step == WhatsappConversationStep.SECRETARIAPAY_CHARGE_FOUND
                || step == WhatsappConversationStep.SECRETARIAPAY_STUDENT_FOUND) {

            if ("1".equals(normalized) || containsAny(normalized, "comprovativo", "comprovante", "paguei", "enviar pagamento")) {
                return Optional.of(askForPaymentProof(session, message));
            }

            if ("2".equals(normalized)) {
                Map<String, String> metadata = readMetadata(session);

                if ("STUDENT_SUMMARY".equalsIgnoreCase(metadata.get("contextSource"))) {
                    return Optional.of(buildPaymentInstructions(session, message));
                }

                return Optional.of(askForHumanSupport(session, message));
            }

            if ("3".equals(normalized)
                    || containsAny(normalized, "secretaria", "tesouraria", "humano", "atendente", "falar")) {
                return Optional.of(askForHumanSupport(session, message));
            }
        }

        if (step == WhatsappConversationStep.SECRETARIAPAY_WAITING_PAYMENT_PROOF) {
            return Optional.of("""
                    Certo. Estou aguardando o comprovativo.

                    Envie por favor uma imagem ou PDF do comprovativo para que a tesouraria possa validar o pagamento.
                    """.trim());
        }

        if (step == WhatsappConversationStep.SECRETARIAPAY_WAITING_HUMAN_SUPPORT) {
            session.setLastMessageText(message)
                    .setUpdatedAt(LocalDateTime.now())
                    .setExpiresAt(LocalDateTime.now().plusHours(SESSION_DURATION_HOURS));

            sessionRepository.save(session);

            return Optional.of("""
                    Recebi a sua mensagem.

                    A solicitação está marcada para acompanhamento da secretaria/tesouraria. Para agilizar, envie também o seu nome completo, BI ou número de estudante, se ainda não enviou.
                    """.trim());
        }

        return Optional.empty();
    }

    @Transactional
    public void rememberWaitingIdentifier(String fromPhone, String lastMessageText) {
        WhatsappSession session = getOrCreateSession(fromPhone);

        session.setCurrentStep(WhatsappConversationStep.SECRETARIAPAY_WAITING_IDENTIFIER)
                .setLastMessageText(lastMessageText)
                .setExpiresAt(LocalDateTime.now().plusHours(SESSION_DURATION_HOURS));

        Map<String, String> metadata = readMetadata(session);
        metadata.put("expectedData", "STUDENT_IDENTIFIER");
        writeMetadata(session, metadata);

        sessionRepository.save(session);
    }

    @Transactional
    public void rememberChargeContext(String fromPhone, Charge charge, String contextSource) {
        WhatsappSession session = getOrCreateSession(fromPhone);

        session.setCurrentStep(WhatsappConversationStep.SECRETARIAPAY_CHARGE_FOUND)
                .setLastMessageText(charge == null ? null : charge.getChargeCode())
                .setExpiresAt(LocalDateTime.now().plusHours(SESSION_DURATION_HOURS));

        Map<String, String> metadata = readMetadata(session);

        metadata.put("contextSource", safe(contextSource));
        metadata.put("lastIntent", "CHARGE_LOOKUP");

        if (charge != null) {
            putUuid(metadata, "lastChargeId", charge.getId());
            put(metadata, "lastChargeCode", charge.getChargeCode());

            if (charge.getStudent() != null) {
                Student student = charge.getStudent();

                putUuid(metadata, "lastStudentId", student.getId());
                put(metadata, "lastStudentNumber", student.getStudentNumber());
                put(metadata, "lastStudentName", student.getFullName());
            }
        }

        writeMetadata(session, metadata);
        sessionRepository.save(session);
    }

    @Transactional
    public void rememberStudentContext(String fromPhone, Student student, Optional<Charge> firstOpenCharge) {
        WhatsappSession session = getOrCreateSession(fromPhone);

        session.setCurrentStep(WhatsappConversationStep.SECRETARIAPAY_STUDENT_FOUND)
                .setLastMessageText(student == null ? null : student.getFullName())
                .setExpiresAt(LocalDateTime.now().plusHours(SESSION_DURATION_HOURS));

        Map<String, String> metadata = readMetadata(session);

        metadata.put("contextSource", "STUDENT_SUMMARY");
        metadata.put("lastIntent", "STUDENT_FINANCIAL_SUMMARY");

        if (student != null) {
            putUuid(metadata, "lastStudentId", student.getId());
            put(metadata, "lastStudentNumber", student.getStudentNumber());
            put(metadata, "lastStudentName", student.getFullName());
            put(metadata, "lastStudentDocument", student.getDocumentNumber());
        }

        if (firstOpenCharge != null && firstOpenCharge.isPresent()) {
            Charge charge = firstOpenCharge.get();

            putUuid(metadata, "lastChargeId", charge.getId());
            put(metadata, "lastChargeCode", charge.getChargeCode());
        }

        writeMetadata(session, metadata);
        sessionRepository.save(session);
    }

    private Optional<String> handleIncomingPaymentProof(
            WhatsappSession session,
            String type,
            String mediaId,
            String fileName,
            String mimeType
    ) {
        Map<String, String> metadata = readMetadata(session);

        String chargeCode = metadata.get("lastChargeCode");

        session.setLastMessageText("[" + type + " recebido]")
                .setExpiresAt(LocalDateTime.now().plusHours(SESSION_DURATION_HOURS));

        if (!isBlank(chargeCode)) {
            Optional<Charge> chargeOptional = chargeRepository.findByChargeCode(chargeCode);

            if (chargeOptional.isEmpty()) {
                session.setCurrentStep(WhatsappConversationStep.SECRETARIAPAY_WAITING_IDENTIFIER);
                metadata.put("lastIntent", "PAYMENT_PROOF_WITH_INVALID_CHARGE_CONTEXT");
                metadata.put("paymentProofReceivedAt", LocalDateTime.now().toString());

                writeMetadata(session, metadata);
                sessionRepository.save(session);

                return Optional.of("""
                        Comprovativo recebido.

                        Não consegui associar automaticamente à cobrança anterior. Envie por favor o código da cobrança ou o seu BI/número de estudante.
                        """.trim());
            }

            Charge charge = chargeOptional.get();
            String mediaReference = buildWhatsappMediaReference(mediaId, type);

            if (paymentProofRepository.existsByFileUrl(mediaReference)) {
                session.setCurrentStep(WhatsappConversationStep.SECRETARIAPAY_CHARGE_FOUND);
                metadata.put("lastIntent", "PAYMENT_PROOF_ALREADY_REGISTERED");
                metadata.put("paymentProofReceivedAt", LocalDateTime.now().toString());
                writeMetadata(session, metadata);
                sessionRepository.save(session);

                return Optional.of(("""
                        Este comprovativo já estava registado para a cobrança %s.

                        Estado: Pendente de validação pela DCR.
                        """).formatted(chargeCode).trim());
            }

            PaymentProof proof = new PaymentProof()
                    .setCharge(charge)
                    .setFileUrl(mediaReference)
                    .setFileName(resolveFileName(type, mediaId, fileName, mimeType))
                    .setMimeType(resolveMimeType(type, mimeType))
                    .setSubmittedByPhone(session.getPhoneNumber())
                    .setStatus(PaymentProofStatus.PENDING_REVIEW);

            PaymentProof savedProof = paymentProofRepository.save(proof);

            session.setCurrentStep(WhatsappConversationStep.SECRETARIAPAY_CHARGE_FOUND);

            metadata.put("lastIntent", "PAYMENT_PROOF_REGISTERED");
            metadata.put("paymentProofReceivedAt", LocalDateTime.now().toString());
            putUuid(metadata, "lastPaymentProofId", savedProof.getId());

            if (!isBlank(mediaId)) {
                metadata.put("lastWhatsappMediaId", mediaId);
            }

            writeMetadata(session, metadata);
            sessionRepository.save(session);

            return Optional.of(("""
                    Comprovativo recebido e registado para a cobrança %s.

                    Estado: Pendente de validação pela tesouraria.

                    Assim que aprovado, o sistema enviará o recibo digital e atualizará a sua situação académica.
                    """).formatted(chargeCode).trim());
        }

        session.setCurrentStep(WhatsappConversationStep.SECRETARIAPAY_WAITING_IDENTIFIER);
        metadata.put("lastIntent", "UNLINKED_PAYMENT_PROOF_RECEIVED");
        metadata.put("paymentProofReceivedAt", LocalDateTime.now().toString());

        if (!isBlank(mediaId)) {
            metadata.put("lastWhatsappMediaId", mediaId);
        }

        writeMetadata(session, metadata);
        sessionRepository.save(session);

        return Optional.of("""
                Comprovativo recebido.

                Para associar corretamente ao aluno e à cobrança, envie por favor o seu BI, número de estudante ou código da cobrança.
                """.trim());
    }

    private String askForPaymentProof(WhatsappSession session, String message) {
        Map<String, String> metadata = readMetadata(session);

        String chargeCode = metadata.get("lastChargeCode");

        session.setCurrentStep(WhatsappConversationStep.SECRETARIAPAY_WAITING_PAYMENT_PROOF)
                .setLastMessageText(message)
                .setExpiresAt(LocalDateTime.now().plusHours(SESSION_DURATION_HOURS));

        metadata.put("lastIntent", "WAITING_PAYMENT_PROOF");
        writeMetadata(session, metadata);

        sessionRepository.save(session);

        if (!isBlank(chargeCode)) {
            return ("""
                    Perfeito. Envie o comprovativo em imagem ou PDF para a cobrança %s.

                    Assim que recebermos, a tesouraria fará a validação e o sistema enviará o recibo digital após aprovação.
                    """).formatted(chargeCode).trim();
        }

        return """
                Perfeito. Envie o comprovativo em imagem ou PDF.

                Para facilitar a validação, envie também o seu BI, número de estudante ou código da cobrança.
                """.trim();
    }

    private String buildPaymentInstructions(WhatsappSession session, String message) {
        Map<String, String> metadata = readMetadata(session);

        String chargeCode = metadata.get("lastChargeCode");

        session.setCurrentStep(WhatsappConversationStep.SECRETARIAPAY_CHARGE_FOUND)
                .setLastMessageText(message)
                .setExpiresAt(LocalDateTime.now().plusHours(SESSION_DURATION_HOURS));

        metadata.put("lastIntent", "PAYMENT_INSTRUCTIONS");
        writeMetadata(session, metadata);

        sessionRepository.save(session);

        if (!isBlank(chargeCode)) {
            return ("""
                    Para regularizar a cobrança %s, faça o pagamento pelo meio orientado pela instituição.

                    Depois do pagamento, envie o comprovativo aqui no WhatsApp em imagem ou PDF.

                    Deseja continuar?
                    1. Enviar comprovativo
                    2. Falar com a secretaria
                    """).formatted(chargeCode).trim();
        }

        return """
                Para regularizar a sua situação, faça o pagamento pelo meio orientado pela instituição.

                Depois do pagamento, envie o comprovativo aqui no WhatsApp em imagem ou PDF.

                Deseja continuar?
                1. Enviar comprovativo
                2. Falar com a secretaria
                """.trim();
    }

    private String askForHumanSupport(WhatsappSession session, String message) {
        Map<String, String> metadata = readMetadata(session);

        session.setCurrentStep(WhatsappConversationStep.SECRETARIAPAY_WAITING_HUMAN_SUPPORT)
                .setLastMessageText(message)
                .setExpiresAt(LocalDateTime.now().plusHours(SESSION_DURATION_HOURS));

        metadata.put("lastIntent", "HUMAN_SUPPORT_REQUESTED");
        writeMetadata(session, metadata);

        sessionRepository.save(session);

        return """
                Certo. Vou encaminhar a sua solicitação para a secretaria/tesouraria.

                Para agilizar o atendimento, envie por favor:
                • Nome completo
                • BI ou número de estudante
                • Motivo do contacto
                """.trim();
    }

    private WhatsappSession getOrCreateSession(String fromPhone) {
        String phone = sanitizePhone(fromPhone);
        LocalDateTime now = LocalDateTime.now();

        Optional<WhatsappSession> existing = sessionRepository
                .findFirstByPhoneNumberAndSessionTypeAndStatusOrderByUpdatedAtDesc(
                        phone,
                        WhatsappSessionType.SECRETARIAPAY_ACADEMICO,
                        WhatsappSessionStatus.ACTIVE
                );

        if (existing.isPresent()) {
            WhatsappSession session = existing.get();

            if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(now)) {
                session.setCurrentStep(WhatsappConversationStep.SECRETARIAPAY_START)
                        .setLastMessageText(null)
                        .setMetadata(null)
                        .setExpiresAt(now.plusHours(SESSION_DURATION_HOURS));

                return sessionRepository.save(session);
            }

            return session;
        }

        WhatsappSession session = new WhatsappSession()
                .setPhoneNumber(phone)
                .setSessionType(WhatsappSessionType.SECRETARIAPAY_ACADEMICO)
                .setStatus(WhatsappSessionStatus.ACTIVE)
                .setCurrentStep(WhatsappConversationStep.SECRETARIAPAY_START)
                .setExpiresAt(now.plusHours(SESSION_DURATION_HOURS));

        return sessionRepository.save(session);
    }

    private void resetSession(WhatsappSession session, String message) {
        session.setCurrentStep(WhatsappConversationStep.SECRETARIAPAY_START)
                .setLastMessageText(message)
                .setMetadata(null)
                .setExpiresAt(LocalDateTime.now().plusHours(SESSION_DURATION_HOURS));

        sessionRepository.save(session);
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

    private String buildWhatsappMediaReference(String mediaId, String type) {
        if (!isBlank(mediaId)) {
            return "whatsapp-cloud-media://" + mediaId;
        }

        return "whatsapp-cloud-media://unknown/" + safe(type) + "/" + System.currentTimeMillis();
    }

    private String resolveFileName(
            String type,
            String mediaId,
            String fileName,
            String mimeType
    ) {
        if (!isBlank(fileName)) {
            return fileName;
        }

        String extension = resolveFileExtension(type, mimeType);
        String suffix = isBlank(mediaId) ? String.valueOf(System.currentTimeMillis()) : mediaId;

        if (suffix.length() > 16) {
            suffix = suffix.substring(0, 16);
        }

        return "secretariapay-whatsapp-" + safe(type) + "-" + suffix + extension;
    }

    private String resolveMimeType(String type, String mimeType) {
        if (!isBlank(mimeType)) {
            return mimeType;
        }

        if ("image".equalsIgnoreCase(type)) {
            return "image/jpeg";
        }

        if ("document".equalsIgnoreCase(type)) {
            return "application/octet-stream";
        }

        return "application/octet-stream";
    }

    private String resolveFileExtension(String type, String mimeType) {
        String normalizedMimeType = safe(mimeType).toLowerCase(Locale.ROOT);

        if (normalizedMimeType.contains("png")) {
            return ".png";
        }

        if (normalizedMimeType.contains("pdf")) {
            return ".pdf";
        }

        if (normalizedMimeType.contains("jpeg") || normalizedMimeType.contains("jpg")) {
            return ".jpg";
        }

        if ("image".equalsIgnoreCase(type)) {
            return ".jpg";
        }

        if ("document".equalsIgnoreCase(type)) {
            return ".bin";
        }

        return ".dat";
    }

    private void put(Map<String, String> metadata, String key, String value) {
        if (metadata == null || key == null || key.isBlank() || value == null || value.isBlank()) {
            return;
        }

        metadata.put(key, value);
    }

    private void putUuid(Map<String, String> metadata, String key, UUID value) {
        if (value == null) {
            return;
        }

        put(metadata, key, value.toString());
    }

    private boolean isRestartIntent(String normalized) {
        return containsAny(
                normalized,
                "menu",
                "inicio",
                "início",
                "comecar",
                "começar",
                "reiniciar",
                "voltar"
        );
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

        return value
                .toLowerCase(Locale.ROOT)
                .trim();
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
