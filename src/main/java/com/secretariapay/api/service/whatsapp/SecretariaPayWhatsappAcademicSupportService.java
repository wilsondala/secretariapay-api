package com.secretariapay.api.service.whatsapp;

import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.repository.academic.StudentRepository;
import com.secretariapay.api.repository.financial.ChargeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SecretariaPayWhatsappAcademicSupportService {

    private static final Pattern CHARGE_CODE_PATTERN =
            Pattern.compile("\\bCHG\\d{6,}\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern BI_PATTERN =
            Pattern.compile("\\b\\d{8,9}[a-zA-Z]{2}\\d{3}\\b");

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final StudentRepository studentRepository;
    private final ChargeRepository chargeRepository;

    public SecretariaPayWhatsappAcademicSupportService(
            StudentRepository studentRepository,
            ChargeRepository chargeRepository
    ) {
        this.studentRepository = studentRepository;
        this.chargeRepository = chargeRepository;
    }

    public Optional<String> buildDatabaseAwareReply(
            String fromPhone,
            String messageType,
            String rawMessage
    ) {
        String type = safe(messageType).toLowerCase(Locale.ROOT);
        String message = safe(rawMessage).trim();
        String normalized = normalize(message);

        if ("image".equals(type) || "document".equals(type)) {
            return Optional.empty();
        }

        Optional<String> chargeCode = extractChargeCode(message);
        if (chargeCode.isPresent()) {
            return chargeRepository.findByChargeCode(chargeCode.get().toUpperCase(Locale.ROOT))
                    .map(this::buildChargeDetailsReply)
                    .or(() -> Optional.of("""
                            Não encontrei uma cobrança com esse código.

                            Confirme se o código está correto. Exemplo:
                            CHG1783012061065
                            """.trim()));
        }

        Optional<Student> studentByIdentifier = findStudentByMessageIdentifier(message);

        if (studentByIdentifier.isPresent()) {
            return Optional.of(buildStudentFinancialSummaryReply(studentByIdentifier.get()));
        }

        if (isFinancialIntent(normalized)) {
            Optional<Student> studentByPhone = findStudentByPhone(fromPhone);

            if (studentByPhone.isPresent()) {
                return Optional.of(buildStudentFinancialSummaryReply(studentByPhone.get()));
            }

            return Optional.of("""
                    Claro. Para consultar a sua situação financeira, envie um destes dados:

                    • Número de estudante
                    • BI
                    • Código da cobrança

                    Exemplo:
                    BI 000000000LA000
                    """.trim());
        }

        if (looksLikeFullName(message)) {
            List<Student> students = studentRepository.findTop5ByFullNameContainingIgnoreCaseOrderByFullNameAsc(message);

            if (students.size() == 1) {
                return Optional.of(buildStudentFinancialSummaryReply(students.get(0)));
            }

            if (students.size() > 1) {
                return Optional.of(buildMultipleStudentsReply(students));
            }
        }

        return Optional.empty();
    }

    private Optional<Student> findStudentByMessageIdentifier(String message) {
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

        return Optional.empty();
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

    private String buildStudentFinancialSummaryReply(Student student) {
        List<Charge> charges = chargeRepository.findByStudentIdOrderByDueDateDesc(student.getId());

        List<Charge> openCharges = charges.stream()
                .filter(this::isOpenCharge)
                .sorted(Comparator.comparing(Charge::getDueDate))
                .limit(5)
                .toList();

        StringBuilder reply = new StringBuilder();

        reply.append("Encontrei o estudante:\n\n");
        reply.append(student.getFullName()).append("\n");

        if (!isBlank(student.getStudentNumber())) {
            reply.append("Nº estudante: ").append(student.getStudentNumber()).append("\n");
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

                    Caso já tenha pago recentemente, envie o comprovativo ou fale com a secretaria para confirmação.
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
                
                Deseja continuar?
                1. Enviar comprovativo
                2. Ver dados de pagamento
                3. Falar com a secretaria
                """);

        return reply.toString().trim();
    }

    private String buildChargeDetailsReply(Charge charge) {
        StringBuilder reply = new StringBuilder();

        reply.append("Encontrei a cobrança:\n\n");
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
                reply.append("\nNº estudante: ").append(student.getStudentNumber());
            }
        }

        reply.append("""
                
                
                Deseja continuar?
                1. Enviar comprovativo
                2. Falar com a secretaria
                """);

        return reply.toString().trim();
    }

    private String buildMultipleStudentsReply(List<Student> students) {
        StringBuilder reply = new StringBuilder();

        reply.append("Encontrei mais de um estudante com esse nome.\n\n");
        reply.append("Para evitar erro, envie o BI ou número de estudante.\n\n");
        reply.append("Possíveis cadastros:\n");

        for (Student student : students) {
            reply.append("\n• ").append(student.getFullName());

            if (!isBlank(student.getStudentNumber())) {
                reply.append(" — Nº ").append(student.getStudentNumber());
            }
        }

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
                "propina",
                "mensalidade",
                "consultar",
                "quanto devo",
                "divida",
                "dívida",
                "cobranca",
                "cobrança",
                "valor em aberto",
                "pagamento em aberto",
                "situacao financeira",
                "situação financeira",
                "estou em atraso",
                "atraso",
                "bloqueado",
                "regularizar"
        ));
    }

    private boolean looksLikeFullName(String message) {
        String clean = safe(message).trim();

        if (clean.length() < 8) {
            return false;
        }

        if (extractBi(clean).isPresent() || extractChargeCode(clean).isPresent()) {
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