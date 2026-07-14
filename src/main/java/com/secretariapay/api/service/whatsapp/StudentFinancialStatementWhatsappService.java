package com.secretariapay.api.service.whatsapp;

import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.financial.ChargeCategory;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import com.secretariapay.api.entity.enums.financial.ReceiptStatus;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.entity.financial.Receipt;
import com.secretariapay.api.repository.academic.StudentRepository;
import com.secretariapay.api.repository.financial.ChargeRepository;
import com.secretariapay.api.repository.financial.ReceiptRepository;
import com.secretariapay.api.service.financial.ChargeClassificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StudentFinancialStatementWhatsappService {

    private static final int SESSION_MINUTES = 30;

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final StudentRepository studentRepository;
    private final ChargeRepository chargeRepository;
    private final ReceiptRepository receiptRepository;
    private final ChargeClassificationService classificationService;
    private final WhatsAppCloudApiClient whatsAppCloudApiClient;
    private final String apiBaseUrl;

    public StudentFinancialStatementWhatsappService(
            StudentRepository studentRepository,
            ChargeRepository chargeRepository,
            ReceiptRepository receiptRepository,
            ChargeClassificationService classificationService,
            WhatsAppCloudApiClient whatsAppCloudApiClient,
            @Value("${secretariapay.public-api-base-url:https://secretariapay-api.paixaoangola.com}") String apiBaseUrl
    ) {
        this.studentRepository = studentRepository;
        this.chargeRepository = chargeRepository;
        this.receiptRepository = receiptRepository;
        this.classificationService = classificationService;
        this.whatsAppCloudApiClient = whatsAppCloudApiClient;
        this.apiBaseUrl = stripTrailingSlash(apiBaseUrl);
    }

    public String startSummary(String fromPhone) {
        String phone = sanitizePhone(fromPhone);
        sessions.put(phone, Session.waitingStudent("SUMMARY"));
        return askStudent("📊 Situação financeira separada por conta");
    }

    public String startDocuments(String fromPhone) {
        String phone = sanitizePhone(fromPhone);
        sessions.put(phone, Session.waitingStudent("DOCUMENTS"));
        return askStudent("📄 Recibos e documentos financeiros");
    }

    public Optional<String> handleIfActive(String fromPhone, String messageType, String rawMessage) {
        String phone = sanitizePhone(fromPhone);
        Session session = sessions.get(phone);
        if (session == null) return Optional.empty();
        if (session.expiresAt().isBefore(LocalDateTime.now())) {
            sessions.remove(phone);
            return Optional.empty();
        }

        String normalized = normalize(rawMessage);
        if (containsAny(normalized, "menu", "voltar ao menu", "inicio")) {
            sessions.remove(phone);
            return Optional.of(mainMenu());
        }

        if ("WAITING_STUDENT".equals(session.step())) {
            Optional<Student> student = findStudent(rawMessage, phone);
            if (student.isEmpty()) {
                return Optional.of("⚠️ Não encontrei o estudante. Informe novamente a matrícula, o BI ou o telefone cadastrado.\n\n[1] Tentar novamente\n[2] Voltar ao menu principal");
            }
            Session identified = session.withStudent(student.get());
            if ("SUMMARY".equals(session.action())) {
                sessions.remove(phone);
                return Optional.of(buildSummary(student.get()));
            }
            sessions.put(phone, identified.withStep("WAITING_DOCUMENT_CHOICE"));
            return Optional.of(buildDocumentMenu(student.get()));
        }

        if ("WAITING_DOCUMENT_CHOICE".equals(session.step())) {
            if ("1".equals(normalized) || containsAny(normalized, "bordero de propinas", "borderô de propinas", "propinas")) {
                sessions.remove(phone);
                return Optional.of(sendStatement(phone, session.studentId(), ChargeCategory.TUITION));
            }
            if ("2".equals(normalized) || containsAny(normalized, "historico de servicos", "histórico de serviços", "servicos academicos", "serviços académicos")) {
                sessions.remove(phone);
                return Optional.of(sendStatement(phone, session.studentId(), ChargeCategory.ACADEMIC_SERVICE));
            }
            if ("3".equals(normalized) || containsAny(normalized, "voltar")) {
                sessions.remove(phone);
                return Optional.of(mainMenu());
            }
            return Optional.of(buildDocumentMenu(loadStudent(session.studentId())));
        }

        sessions.remove(phone);
        return Optional.of(mainMenu());
    }

    private String buildSummary(Student student) {
        List<Charge> charges = chargeRepository.findByStudentIdOrderByDueDateDesc(student.getId()).stream()
                .filter(charge -> charge.getStatus() != ChargeStatus.CANCELLED && charge.getStatus() != ChargeStatus.RENEGOTIATED)
                .toList();
        Summary tuition = summarize(charges, ChargeCategory.TUITION);
        Summary services = summarize(charges, ChargeCategory.ACADEMIC_SERVICE);
        BigDecimal totalOpen = tuition.openAmount().add(services.openAmount());

        return """
                📊 Situação financeira de %s
                Matrícula: %s

                PROPINAS
                Pagas: %d — %s
                Em aberto: %d — %s
                Vencidas: %d

                SERVIÇOS ACADÉMICOS
                Pagos: %d — %s
                Em aberto: %d — %s

                TOTAL EM ABERTO
                %s

                O borderô de propinas não inclui matrícula, declarações, exames, certificado ou diploma.
                """.formatted(
                student.getFullName(), student.getStudentNumber(),
                tuition.paidCount(), money(tuition.paidAmount()), tuition.openCount(), money(tuition.openAmount()), tuition.overdueCount(),
                services.paidCount(), money(services.paidAmount()), services.openCount(), money(services.openAmount()),
                money(totalOpen)
        ).trim();
    }

    private String buildDocumentMenu(Student student) {
        return """
                📄 Recibos e documentos financeiros

                Estudante: %s
                Matrícula: %s

                [1] Emitir borderô de propinas
                [2] Emitir histórico de serviços académicos
                [3] Voltar ao menu principal

                Os documentos são separados por natureza financeira.
                """.formatted(student.getFullName(), student.getStudentNumber()).trim();
    }

    private String sendStatement(String phone, java.util.UUID studentId, ChargeCategory category) {
        Student student = loadStudent(studentId);
        List<Receipt> receipts = receiptRepository
                .findByChargeStudentIdAndStatusOrderByChargePaidAtAsc(studentId, ReceiptStatus.VALID)
                .stream()
                .filter(receipt -> classificationService.resolveCategory(receipt.getCharge()) == category)
                .sorted(Comparator.comparing(
                        (Receipt receipt) -> receipt.getCharge().getPaidAt(),
                        Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder())
                ).reversed())
                .toList();

        if (receipts.isEmpty()) {
            return category == ChargeCategory.TUITION
                    ? "⚠️ Não existem propinas pagas com recibo válido para emitir o borderô."
                    : "⚠️ Não existem serviços académicos pagos com recibo válido para emitir o histórico.";
        }

        Receipt anchor = receipts.getFirst();
        BigDecimal total = receipts.stream()
                .map(receipt -> receipt.getCharge().getTotalAmount())
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String pdfUrl = apiBaseUrl + "/api/v1/public/receipts/" + encode(anchor.getReceiptCode()) + "/pdf?v=" + System.currentTimeMillis();
        String fileName = category == ChargeCategory.TUITION
                ? "Bordero_Propinas_" + sanitizeFilePart(student.getStudentNumber()) + ".pdf"
                : "Historico_Servicos_Academicos_" + sanitizeFilePart(student.getStudentNumber()) + ".pdf";
        String documentName = category == ChargeCategory.TUITION ? "Borderô de propinas" : "Histórico de serviços académicos";
        String caption = """
                ✅ %s emitido com sucesso.

                Estudante: %s
                Matrícula: %s
                Registos incluídos: %d
                Total histórico: %s

                %s
                """.formatted(
                documentName, student.getFullName(), student.getStudentNumber(), receipts.size(), money(total),
                category == ChargeCategory.TUITION
                        ? "Este documento contém somente pagamentos de propinas."
                        : "Este documento contém somente pagamentos de serviços académicos."
        ).trim();
        whatsAppCloudApiClient.sendDocumentByLink(phone, pdfUrl, fileName, caption);
        return caption + "\n\nO PDF foi enviado neste WhatsApp.";
    }

    private Summary summarize(List<Charge> charges, ChargeCategory category) {
        List<Charge> selected = charges.stream()
                .filter(charge -> classificationService.resolveCategory(charge) == category)
                .toList();
        List<Charge> paid = selected.stream().filter(charge -> charge.getStatus() == ChargeStatus.PAID).toList();
        List<Charge> open = selected.stream().filter(this::isOpen).toList();
        long overdue = open.stream().filter(this::isOverdue).count();
        return new Summary(
                paid.size(), open.size(), overdue,
                sum(paid), sum(open)
        );
    }

    private BigDecimal sum(List<Charge> charges) {
        return charges.stream().map(Charge::getTotalAmount).filter(value -> value != null).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean isOpen(Charge charge) {
        return charge.getStatus() != ChargeStatus.PAID
                && charge.getStatus() != ChargeStatus.CANCELLED
                && charge.getStatus() != ChargeStatus.RENEGOTIATED;
    }

    private boolean isOverdue(Charge charge) {
        return charge.getStatus() == ChargeStatus.OVERDUE
                || (charge.getDueDate() != null && charge.getDueDate().isBefore(java.time.LocalDate.now()) && isOpen(charge));
    }

    private Optional<Student> findStudent(String input, String fromPhone) {
        String clean = input == null ? "" : input.trim();
        if (!clean.isBlank()) {
            Optional<Student> result = studentRepository.findByStudentNumber(clean);
            if (result.isPresent()) return result;
            result = studentRepository.findByDocumentNumberIgnoreCase(clean);
            if (result.isPresent()) return result;
            result = studentRepository.findByEmailIgnoreCase(clean);
            if (result.isPresent()) return result;
            result = findByPhone(clean);
            if (result.isPresent()) return result;
        }
        return findByPhone(fromPhone);
    }

    private Optional<Student> findByPhone(String rawPhone) {
        String digits = sanitizePhone(rawPhone);
        if (digits.isBlank()) return Optional.empty();
        for (String variant : List.of(digits, "+" + digits, digits.replaceFirst("^244", "0"), digits.replaceFirst("^55", "0"))) {
            Optional<Student> result = studentRepository.findByWhatsapp(variant);
            if (result.isPresent()) return result;
            result = studentRepository.findByPhone(variant);
            if (result.isPresent()) return result;
            result = studentRepository.findByGuardianPhone(variant);
            if (result.isPresent()) return result;
        }
        return Optional.empty();
    }

    private Student loadStudent(java.util.UUID id) {
        return studentRepository.findById(id).orElseThrow(() -> new IllegalStateException("Estudante não encontrado."));
    }

    private String askStudent(String title) {
        return title + "\n\nInforme a matrícula, o BI ou o telefone cadastrado.\n\nExemplo: 202301404";
    }

    private String mainMenu() {
        return """
                Secretaria Pay (IMETRO) 👋

                [1] Propinas
                [2] Situação financeira
                [3] Recibos e documentos
                [4] Serviços académicos
                [5] Falar com a DCR
                """.trim();
    }

    private String money(BigDecimal value) {
        return String.format(Locale.forLanguageTag("pt-AO"), "%,.2f", value == null ? BigDecimal.ZERO : value)
                .replace(',', '#').replace('.', ',').replace('#', '.') + " Kz";
    }

    private String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
    }

    private boolean containsAny(String value, String... terms) {
        for (String term : terms) if (value.contains(normalize(term))) return true;
        return false;
    }

    private String sanitizePhone(String value) { return value == null ? "" : value.replaceAll("[^0-9]", ""); }
    private String sanitizeFilePart(String value) { String safe = value == null ? "" : value.replaceAll("[^A-Za-z0-9._-]", "-").replaceAll("-+", "-"); return safe.isBlank() ? "estudante" : safe; }
    private String encode(String value) { return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8); }
    private String stripTrailingSlash(String value) { String resolved = value == null || value.isBlank() ? "https://secretariapay-api.paixaoangola.com" : value.trim(); return resolved.endsWith("/") ? resolved.substring(0, resolved.length() - 1) : resolved; }

    private record Summary(int paidCount, int openCount, long overdueCount, BigDecimal paidAmount, BigDecimal openAmount) { }

    private record Session(String step, String action, java.util.UUID studentId, LocalDateTime expiresAt) {
        static Session waitingStudent(String action) { return new Session("WAITING_STUDENT", action, null, LocalDateTime.now().plusMinutes(SESSION_MINUTES)); }
        Session withStudent(Student student) { return new Session(step, action, student.getId(), LocalDateTime.now().plusMinutes(SESSION_MINUTES)); }
        Session withStep(String newStep) { return new Session(newStep, action, studentId, LocalDateTime.now().plusMinutes(SESSION_MINUTES)); }
    }
}
