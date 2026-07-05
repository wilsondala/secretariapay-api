package com.secretariapay.api.service.whatsapp;

import com.secretariapay.api.dto.financial.TuitionChargeGuideDeliveryRequest;
import com.secretariapay.api.dto.financial.TuitionChargeGuideDeliveryResponse;
import com.secretariapay.api.entity.academic.AcademicClass;
import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.academic.Institution;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.repository.academic.StudentRepository;
import com.secretariapay.api.repository.financial.ChargeRepository;
import com.secretariapay.api.service.financial.TuitionChargeGuideDeliveryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SecretariaPayWhatsappAcademicSupportService {

    private static final String IMETRO_NAME = "Instituto Superior Politécnico Metropolitano de Angola (IMETRO)";

    private static final Pattern CHARGE_CODE_PATTERN =
            Pattern.compile("\\b(?:CHG\\d{6,}|IMT-[A-Z0-9_\\-]+)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern BI_PATTERN =
            Pattern.compile("\\b\\d{8,9}[a-zA-Z]{2}\\d{3}\\b");

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("(?:\\+?244|\\+?55)?[0-9][0-9\\s().-]{7,}");

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final StudentRepository studentRepository;
    private final ChargeRepository chargeRepository;
    private final SecretariaPayWhatsappConversationContextService conversationContextService;
    private final TuitionChargeGuideDeliveryService guideDeliveryService;

    public SecretariaPayWhatsappAcademicSupportService(
            StudentRepository studentRepository,
            ChargeRepository chargeRepository,
            SecretariaPayWhatsappConversationContextService conversationContextService,
            TuitionChargeGuideDeliveryService guideDeliveryService
    ) {
        this.studentRepository = studentRepository;
        this.chargeRepository = chargeRepository;
        this.conversationContextService = conversationContextService;
        this.guideDeliveryService = guideDeliveryService;
    }

    @Transactional
    public Optional<String> buildDatabaseAwareReply(
            String fromPhone,
            String messageType,
            String rawMessage
    ) {
        return buildDatabaseAwareReply(
                fromPhone,
                messageType,
                rawMessage,
                null,
                null,
                null
        );
    }

    @Transactional
    public Optional<String> buildDatabaseAwareReply(
            String fromPhone,
            String messageType,
            String rawMessage,
            String mediaId,
            String fileName,
            String mimeType
    ) {
        String type = safe(messageType).toLowerCase(Locale.ROOT);
        String message = safe(rawMessage).trim();
        String normalized = normalize(message);

        Optional<String> contextualReply = conversationContextService.resolveContextualReply(
                fromPhone,
                type,
                message,
                mediaId,
                fileName,
                mimeType
        );

        if (contextualReply.isPresent()) {
            return contextualReply;
        }

        if ("image".equals(type) || "document".equals(type)) {
            return Optional.empty();
        }

        Optional<String> chargeCode = extractChargeCode(message);

        if (chargeCode.isPresent()) {
            return chargeRepository.findByChargeCode(chargeCode.get().toUpperCase(Locale.ROOT))
                    .map(charge -> handleChargeLocated(fromPhone, charge))
                    .or(() -> Optional.of("""
                            Não encontrei uma cobrança com esse código no cadastro do IMETRO.

                            Confirme se o código está correto. Exemplo:
                            IMT-PROPINA-2026_07-20200629
                            """.trim()));
        }

        Optional<Student> studentByIdentifier = findStudentByMessageIdentifier(message);

        if (studentByIdentifier.isPresent()) {
            return Optional.of(handleStudentLocated(fromPhone, studentByIdentifier.get()));
        }

        if (isFinancialIntent(normalized)) {
            Optional<Student> studentByPhone = findStudentByPhone(fromPhone);

            if (studentByPhone.isPresent()) {
                return Optional.of(handleStudentLocated(fromPhone, studentByPhone.get()));
            }

            conversationContextService.rememberWaitingIdentifier(
                    fromPhone,
                    message
            );

            return Optional.of("""
                    Claro. Para consultar propina, dívida ou guia de pagamento do IMETRO, envie um destes dados cadastrados na universidade:

                    • Número de estudante/carteira
                    • E-mail cadastrado
                    • Telefone cadastrado
                    • Código da cobrança

                    Regra de segurança: mesmo que a solicitação venha de outro telefone, a informação financeira será enviada apenas para os contactos oficiais cadastrados no IMETRO.
                    """.trim());
        }

        if (looksLikeFullName(message)) {
            return Optional.of("""
                    Para proteger os dados dos estudantes, a busca por nome completo foi desativada neste canal.

                    Envie o número de estudante/carteira, e-mail cadastrado, telefone cadastrado ou código da cobrança.
                    """.trim());
        }

        return Optional.empty();
    }

    private String handleChargeLocated(String fromPhone, Charge charge) {
        Student student = charge.getStudent();

        if (student == null) {
            return "Cobrança localizada, mas sem estudante vinculado. A DCR deve revisar este registo.";
        }

        boolean requesterIsRegistered = isIncomingRegisteredForStudent(fromPhone, student);
        Optional<Charge> firstOpenCharge = findFirstOpenCharge(student);

        conversationContextService.rememberStudentContext(
                fromPhone,
                student,
                firstOpenCharge.isPresent() ? firstOpenCharge : Optional.of(charge)
        );

        TuitionChargeGuideDeliveryResponse delivery = deliverChargeGuidesToRegisteredContacts(student, List.of(charge));

        if (requesterIsRegistered) {
            return buildChargeDetailsReply(charge)
                    + "\n\n"
                    + buildDeliverySummaryForRegisteredRequester(delivery);
        }

        return buildSecureForwardedReply(delivery);
    }

    private String handleStudentLocated(String fromPhone, Student student) {
        boolean requesterIsRegistered = isIncomingRegisteredForStudent(fromPhone, student);
        List<Charge> openCharges = findOpenCharges(student);
        Optional<Charge> firstOpenCharge = openCharges.stream().findFirst();

        conversationContextService.rememberStudentContext(
                fromPhone,
                student,
                firstOpenCharge
        );

        TuitionChargeGuideDeliveryResponse delivery = deliverChargeGuidesToRegisteredContacts(student, openCharges);

        if (requesterIsRegistered) {
            return buildStudentFinancialSummaryReply(student)
                    + "\n\n"
                    + buildDeliverySummaryForRegisteredRequester(delivery);
        }

        return buildSecureForwardedReply(delivery);
    }

    private TuitionChargeGuideDeliveryResponse deliverChargeGuidesToRegisteredContacts(Student student, List<Charge> charges) {
        List<UUID> chargeIds = charges == null
                ? List.of()
                : charges.stream()
                .filter(charge -> charge != null && charge.getId() != null)
                .sorted(Comparator.comparing(Charge::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(5)
                .map(Charge::getId)
                .toList();

        TuitionChargeGuideDeliveryRequest request = new TuitionChargeGuideDeliveryRequest()
                .setInstitutionId(resolveInstitutionId(student))
                .setChargeIds(chargeIds)
                .setChargeCodePrefix("")
                .setOnlyPending(true)
                .setSendWhatsapp(true)
                .setSendEmail(true)
                .setSendSms(true)
                .setForceResend(false)
                .setMaxItems(5);

        return guideDeliveryService.sendGuides(request);
    }

    private String buildSecureForwardedReply(TuitionChargeGuideDeliveryResponse delivery) {
        if (delivery == null || delivery.getSelectedCharges() == 0) {
            return """
                    Cadastro localizado no IMETRO.

                    Por segurança, não posso mostrar dados financeiros neste telefone porque ele não corresponde ao contacto cadastrado na universidade.

                    Não há cobranças pendentes para envio automático neste momento. Para mais detalhes, contacte a DCR/Secretaria com o número de estudante/carteira.
                    """.trim();
        }

        if (delivery.getSentWhatsapp() > 0 || delivery.getSentEmail() > 0 || delivery.getSentSms() > 0 || delivery.getSkippedAlreadySent() > 0) {
            return """
                    Cadastro localizado no IMETRO.

                    Por segurança, a informação financeira não será enviada para este telefone.

                    A guia ou situação solicitada foi enviada, ou já estava enviada, para os contactos oficiais cadastrados na universidade: WhatsApp, SMS/telefone ou e-mail.
                    """.trim();
        }

        if (delivery.getSkippedNoContact() > 0) {
            return """
                    Cadastro localizado no IMETRO.

                    Porém, o estudante não possui WhatsApp, telefone ou e-mail cadastrado para envio seguro da guia.

                    A DCR/Secretaria deve atualizar o contacto oficial no cadastro académico antes do envio.
                    """.trim();
        }

        return """
                Cadastro localizado no IMETRO.

                Não foi possível entregar a guia nos contactos oficiais cadastrados. A DCR deve revisar o cadastro e o histórico de envio.
                """.trim();
    }

    private String buildDeliverySummaryForRegisteredRequester(TuitionChargeGuideDeliveryResponse delivery) {
        if (delivery == null || delivery.getSelectedCharges() == 0) {
            return "Não há guia pendente para envio automático neste momento.";
        }

        if (delivery.getSentWhatsapp() > 0 || delivery.getSentEmail() > 0 || delivery.getSentSms() > 0) {
            return "A guia foi enviada para os contactos cadastrados no IMETRO.";
        }

        if (delivery.getSkippedAlreadySent() > 0) {
            return "A guia já tinha sido enviada anteriormente. Para reenvio manual, a DCR deve autorizar.";
        }

        if (delivery.getSkippedNoContact() > 0) {
            return "O cadastro não possui contacto oficial suficiente para envio da guia. Atualize o WhatsApp, telefone ou e-mail na secretaria.";
        }

        return "Não foi possível entregar a guia automaticamente. A DCR deve verificar o histórico de envio.";
    }

    private Optional<Student> findStudentByMessageIdentifier(String message) {
        String clean = safe(message).trim();

        if (clean.isBlank()) {
            return Optional.empty();
        }

        Optional<String> email = extractEmail(clean);

        if (email.isPresent()) {
            Optional<Student> byEmail = studentRepository.findByEmailIgnoreCase(email.get());
            if (byEmail.isPresent()) return byEmail;

            Optional<Student> byGuardianEmail = studentRepository.findByGuardianEmailIgnoreCase(email.get());
            if (byGuardianEmail.isPresent()) return byGuardianEmail;
        }

        Optional<String> phone = extractPhone(clean);

        if (phone.isPresent()) {
            Optional<Student> byPhone = findStudentByPhone(phone.get());
            if (byPhone.isPresent()) return byPhone;
        }

        Optional<String> bi = extractBi(clean);

        if (bi.isPresent()) {
            Optional<Student> byDocument = studentRepository.findByDocumentNumberIgnoreCase(bi.get());

            if (byDocument.isPresent()) {
                return byDocument;
            }
        }

        String withoutPrefix = clean
                .replaceFirst("(?i)^BI\\s*[:\\-]?\\s*", "")
                .replaceFirst("(?i)^N[º°.]?\\s*[:\\-]?\\s*", "")
                .replaceFirst("(?i)^NUMERO\\s*[:\\-]?\\s*", "")
                .replaceFirst("(?i)^NÚMERO\\s*[:\\-]?\\s*", "")
                .replaceFirst("(?i)^ESTUDANTE\\s*[:\\-]?\\s*", "")
                .replaceFirst("(?i)^CARTEIRA\\s*[:\\-]?\\s*", "")
                .replaceFirst("(?i)^EMAIL\\s*[:\\-]?\\s*", "")
                .replaceFirst("(?i)^TELEFONE\\s*[:\\-]?\\s*", "")
                .replaceFirst("(?i)^TELEM[ÓO]VEL\\s*[:\\-]?\\s*", "")
                .trim();

        Optional<Student> byStudentNumber = studentRepository.findByStudentNumber(withoutPrefix);

        if (byStudentNumber.isPresent()) {
            return byStudentNumber;
        }

        Optional<Student> byDocument = studentRepository.findByDocumentNumberIgnoreCase(withoutPrefix);

        if (byDocument.isPresent()) {
            return byDocument;
        }

        return Optional.empty();
    }

    private Optional<Student> findStudentByPhone(String fromPhone) {
        String sanitized = sanitizePhone(fromPhone);

        if (sanitized.isBlank()) {
            return Optional.empty();
        }

        for (String variant : phoneVariants(sanitized)) {
            Optional<Student> byWhatsapp = studentRepository.findByWhatsapp(variant);
            if (byWhatsapp.isPresent()) return byWhatsapp;

            Optional<Student> byPhone = studentRepository.findByPhone(variant);
            if (byPhone.isPresent()) return byPhone;

            Optional<Student> byGuardianPhone = studentRepository.findByGuardianPhone(variant);
            if (byGuardianPhone.isPresent()) return byGuardianPhone;
        }

        return Optional.empty();
    }

    private boolean isIncomingRegisteredForStudent(String fromPhone, Student student) {
        String sanitized = sanitizePhone(fromPhone);

        if (sanitized.isBlank() || student == null) {
            return false;
        }

        LinkedHashSet<String> registered = new LinkedHashSet<>();
        registered.add(sanitizePhone(student.getWhatsapp()));
        registered.add(sanitizePhone(student.getPhone()));
        registered.add(sanitizePhone(student.getGuardianPhone()));

        registered.removeIf(String::isBlank);

        return registered.stream().anyMatch(phone -> phone.equals(sanitized));
    }

    private List<String> phoneVariants(String sanitized) {
        LinkedHashSet<String> variants = new LinkedHashSet<>();
        variants.add(sanitized);
        variants.add("+" + sanitized);

        if (sanitized.startsWith("244")) {
            variants.add(sanitized.substring(3));
            variants.add("0" + sanitized.substring(3));
        }

        if (sanitized.startsWith("55")) {
            variants.add(sanitized.substring(2));
            variants.add("0" + sanitized.substring(2));
        }

        return variants.stream().filter(value -> !value.isBlank()).toList();
    }

    private List<Charge> findOpenCharges(Student student) {
        if (student == null || student.getId() == null) {
            return List.of();
        }

        return chargeRepository.findByStudentIdOrderByDueDateDesc(student.getId())
                .stream()
                .filter(this::isOpenCharge)
                .sorted(Comparator.comparing(Charge::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(5)
                .toList();
    }

    private Optional<Charge> findFirstOpenCharge(Student student) {
        return findOpenCharges(student).stream().findFirst();
    }

    private String buildStudentFinancialSummaryReply(Student student) {
        List<Charge> openCharges = findOpenCharges(student);

        StringBuilder reply = new StringBuilder();

        reply.append(IMETRO_NAME).append("\n");
        reply.append("DCR — Divisão de Cobranças e Recebimentos\n\n");
        reply.append("Estudante localizado no cadastro oficial:\n\n");
        reply.append(student.getFullName()).append("\n");

        if (!isBlank(student.getStudentNumber())) {
            reply.append("Nº estudante/carteira: ").append(student.getStudentNumber()).append("\n");
        }

        if (!isBlank(student.getDocumentNumber())) {
            reply.append("Documento: ").append(student.getDocumentNumber()).append("\n");
        }

        if (Boolean.TRUE.equals(student.getFinanciallyBlocked())) {
            reply.append("\nSituação académica: com restrição financeira");

            if (!isBlank(student.getBlockedReason())) {
                reply.append("\nMotivo: ").append(student.getBlockedReason());
            }

            reply.append("\n");
        }

        if (openCharges.isEmpty()) {
            reply.append("""

                    Não encontrei cobranças em aberto para este cadastro.

                    Caso já tenha pago recentemente, envie o comprovativo para validação manual da DCR.
                    """);

            return reply.toString().trim();
        }

        reply.append("\nCobranças em aberto:\n");

        for (Charge charge : openCharges) {
            reply.append("\n• ").append(charge.getDescription());

            if (!isBlank(charge.getReferenceMonth())) {
                reply.append(" - ").append(charge.getReferenceMonth());
            }

            reply.append("\n  Código: ").append(charge.getChargeCode());
            reply.append("\n  Valor: ").append(formatMoney(charge.getTotalAmount(), charge.getCurrency()));
            reply.append("\n  Vencimento: ").append(formatDate(charge.getDueDate()));
            reply.append("\n  Estado: ").append(formatStatus(charge.getStatus() == null ? null : charge.getStatus().name()));
            reply.append("\n");
        }

        reply.append("""

                O recibo institucional só será emitido após confirmação manual da DCR.
                """);

        return reply.toString().trim();
    }

    private String buildChargeDetailsReply(Charge charge) {
        StringBuilder reply = new StringBuilder();

        reply.append(IMETRO_NAME).append("\n");
        reply.append("DCR — Divisão de Cobranças e Recebimentos\n\n");
        reply.append("Cobrança localizada:\n\n");
        reply.append(charge.getDescription()).append("\n");
        reply.append("Código: ").append(charge.getChargeCode()).append("\n");

        if (!isBlank(charge.getReferenceMonth())) {
            reply.append("Referência: ").append(charge.getReferenceMonth()).append("\n");
        }

        reply.append("Valor: ").append(formatMoney(charge.getTotalAmount(), charge.getCurrency())).append("\n");
        reply.append("Vencimento: ").append(formatDate(charge.getDueDate())).append("\n");
        reply.append("Estado: ").append(formatStatus(charge.getStatus() == null ? null : charge.getStatus().name())).append("\n");

        if (charge.getStudent() != null) {
            Student student = charge.getStudent();

            reply.append("\nEstudante: ").append(student.getFullName());

            if (!isBlank(student.getStudentNumber())) {
                reply.append("\nNº estudante/carteira: ").append(student.getStudentNumber());
            }
        }

        reply.append("""

                O recibo institucional só será emitido após confirmação manual da DCR.
                """);

        return reply.toString().trim();
    }

    private boolean isOpenCharge(Charge charge) {
        if (charge == null || charge.getStatus() == null) {
            return false;
        }

        String status = charge.getStatus().name();

        return !"PAID".equalsIgnoreCase(status)
                && !"CANCELLED".equalsIgnoreCase(status)
                && !"CANCELED".equalsIgnoreCase(status);
    }

    private boolean isFinancialIntent(String normalized) {
        return containsAny(normalized, List.of(
                "propina", "mensalidade", "consultar", "quanto devo", "divida", "dívida", "cobranca", "cobrança", "valor em aberto", "pagamento em aberto", "situacao financeira", "situação financeira", "estou em atraso", "atraso", "bloqueado", "regularizar", "guia", "boleto", "recibo"
        ));
    }

    private boolean looksLikeFullName(String message) {
        String clean = safe(message).trim();

        if (clean.length() < 8) {
            return false;
        }

        if (extractBi(clean).isPresent() || extractChargeCode(clean).isPresent() || extractEmail(clean).isPresent() || extractPhone(clean).isPresent()) {
            return false;
        }

        if (clean.matches(".*\\d.*")) {
            return false;
        }

        return clean.split("\\s+").length >= 2;
    }

    private Optional<String> extractChargeCode(String message) {
        Matcher matcher = CHARGE_CODE_PATTERN.matcher(safe(message));

        if (matcher.find()) {
            return Optional.of(matcher.group().toUpperCase(Locale.ROOT));
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

    private Optional<String> extractEmail(String message) {
        Matcher matcher = EMAIL_PATTERN.matcher(safe(message));

        if (matcher.find()) {
            return Optional.of(matcher.group().trim());
        }

        return Optional.empty();
    }

    private Optional<String> extractPhone(String message) {
        Matcher matcher = PHONE_PATTERN.matcher(safe(message));

        if (matcher.find()) {
            String phone = matcher.group().trim();
            String sanitized = sanitizePhone(phone);

            if (sanitized.length() >= 8) {
                return Optional.of(phone);
            }
        }

        return Optional.empty();
    }

    private UUID resolveInstitutionId(Student student) {
        if (student == null || student.getAcademicClass() == null) return null;
        AcademicClass academicClass = student.getAcademicClass();
        if (academicClass.getCourse() == null) return null;
        Course course = academicClass.getCourse();
        Institution institution = course.getInstitution();
        return institution != null ? institution.getId() : null;
    }

    private String formatMoney(BigDecimal value, String currency) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        String safeCurrency = isBlank(currency) ? "AOA" : currency;

        return safeValue.stripTrailingZeros().toPlainString() + " " + safeCurrency;
    }

    private String formatDate(LocalDate date) {
        if (date == null) {
            return "não informado";
        }

        return DATE_FORMATTER.format(date);
    }

    private String formatStatus(String status) {
        if (status == null || status.isBlank()) {
            return "não informado";
        }

        return switch (status.toUpperCase(Locale.ROOT)) {
            case "PENDING" -> "Pendente";
            case "OVERDUE" -> "Em atraso";
            case "PAID" -> "Pago";
            case "CANCELLED", "CANCELED" -> "Cancelado";
            default -> status;
        };
    }

    private boolean containsAny(String value, List<String> keywords) {
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
