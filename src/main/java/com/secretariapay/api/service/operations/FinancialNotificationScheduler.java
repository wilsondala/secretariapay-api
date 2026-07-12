package com.secretariapay.api.service.operations;

import com.secretariapay.api.dto.notification.GuideFallbackRequest;
import com.secretariapay.api.dto.whatsapp.WhatsAppCloudSendResult;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.entity.operations.NotificationLog;
import com.secretariapay.api.repository.financial.ChargeRepository;
import com.secretariapay.api.repository.operations.NotificationLogRepository;
import com.secretariapay.api.service.FallbackNotificationService;
import com.secretariapay.api.service.financial.FinancialChargeCalculation;
import com.secretariapay.api.service.financial.FinancialPenaltyCalculatorService;
import com.secretariapay.api.service.whatsapp.WhatsAppCloudApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class FinancialNotificationScheduler {

    private static final String API_BASE_URL = "https://secretariapay-api.paixaoangola.com";
    private static final String PANEL_BASE_URL = "https://painel-secretariapay.paixaoangola.com";

    private final ChargeRepository chargeRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final WhatsAppCloudApiClient whatsAppCloudApiClient;
    private final FallbackNotificationService fallbackNotificationService;
    private final FinancialPenaltyCalculatorService penaltyCalculatorService;
    private final AuditService auditService;
    private final boolean schedulerEnabled;
    private final boolean dryRun;
    private final boolean sendWhatsapp;
    private final boolean sendEmail;
    private final ZoneId zoneId;

    public FinancialNotificationScheduler(
            ChargeRepository chargeRepository,
            NotificationLogRepository notificationLogRepository,
            WhatsAppCloudApiClient whatsAppCloudApiClient,
            FallbackNotificationService fallbackNotificationService,
            FinancialPenaltyCalculatorService penaltyCalculatorService,
            AuditService auditService,
            @Value("${secretariapay.notifications.scheduler-enabled:false}") boolean schedulerEnabled,
            @Value("${secretariapay.notifications.scheduler-dry-run:false}") boolean dryRun,
            @Value("${secretariapay.notifications.scheduler-send-whatsapp:true}") boolean sendWhatsapp,
            @Value("${secretariapay.notifications.scheduler-send-email:true}") boolean sendEmail,
            @Value("${secretariapay.notifications.scheduler-zone:Africa/Luanda}") String schedulerZone
    ) {
        this.chargeRepository = chargeRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.whatsAppCloudApiClient = whatsAppCloudApiClient;
        this.fallbackNotificationService = fallbackNotificationService;
        this.penaltyCalculatorService = penaltyCalculatorService;
        this.auditService = auditService;
        this.schedulerEnabled = schedulerEnabled;
        this.dryRun = dryRun;
        this.sendWhatsapp = sendWhatsapp;
        this.sendEmail = sendEmail;
        this.zoneId = ZoneId.of(schedulerZone == null || schedulerZone.isBlank() ? "Africa/Luanda" : schedulerZone.trim());
    }

    @Scheduled(cron = "${secretariapay.notifications.scheduler-cron:0 0 8 * * *}", zone = "${secretariapay.notifications.scheduler-zone:Africa/Luanda}")
    public void runScheduled() {
        if (!schedulerEnabled) return;
        runDailyNotifications("SCHEDULER");
    }

    @Transactional
    public Map<String, Object> runDailyNotifications(String actor) {
        LocalDate today = LocalDate.now(zoneId);
        Map<String, Object> summary = baseSummary(today, actor);

        int recalculated = recalculateOverdueCharges(today);
        summary.put("recalculatedCharges", recalculated);

        List<Charge> dueTomorrow = chargeRepository.findByDueDateBetweenOrderByDueDateAsc(today.plusDays(1), today.plusDays(1));
        List<Charge> dueToday = chargeRepository.findByDueDateBetweenOrderByDueDateAsc(today, today);
        List<Charge> overdue = new ArrayList<>();
        overdue.addAll(chargeRepository.findByDueDateBeforeAndStatusOrderByDueDateAsc(today, ChargeStatus.PENDING));
        overdue.addAll(chargeRepository.findByDueDateBeforeAndStatusOrderByDueDateAsc(today, ChargeStatus.OVERDUE));

        DispatchCounter counter = new DispatchCounter();
        dueTomorrow.stream().filter(this::isOpen).forEach(charge -> notifyCharge(charge, "DUE_TOMORROW", today, counter));
        dueToday.stream().filter(this::isOpen).forEach(charge -> notifyCharge(charge, "DUE_TODAY", today, counter));
        overdue.stream().filter(this::isOpen).forEach(charge -> notifyCharge(charge, "OVERDUE", today, counter));

        summary.put("chargesDueTomorrow", dueTomorrow.stream().filter(this::isOpen).count());
        summary.put("chargesDueToday", dueToday.stream().filter(this::isOpen).count());
        summary.put("chargesOverdue", overdue.stream().filter(this::isOpen).count());
        summary.put("sent", counter.sent);
        summary.put("prepared", counter.prepared);
        summary.put("failed", counter.failed);
        summary.put("skipped", counter.skipped);
        summary.put("dryRun", dryRun);
        auditService.record(clean(actor, "SYSTEM"), "FINANCIAL_NOTIFICATIONS_RUN", "FinancialNotificationScheduler", today.toString(), summary.toString());
        return summary;
    }

    public List<NotificationLog> recentLogs() {
        return notificationLogRepository.findTop50ByOrderByCreatedAtDesc();
    }

    private int recalculateOverdueCharges(LocalDate today) {
        List<Charge> pendingOverdue = chargeRepository.findByDueDateBeforeAndStatusOrderByDueDateAsc(today, ChargeStatus.PENDING);
        int count = 0;
        for (Charge charge : pendingOverdue) {
            FinancialChargeCalculation calculation = penaltyCalculatorService.calculate(
                    firstNonBlank(charge.getReferenceMonth(), formatMonth(charge.getDueDate())),
                    charge.getAmount(),
                    YearMonth.from(charge.getDueDate()),
                    today
            );
            charge
                    .setFineAmount(calculation.getFineAmount())
                    .setInterestAmount(calculation.getInterestAmount())
                    .setDiscountAmount(calculation.getDiscountAmount())
                    .setStatus(ChargeStatus.OVERDUE);
            chargeRepository.save(charge);
            count++;
        }
        if (count > 0) {
            auditService.record("SYSTEM", "OVERDUE_CHARGES_RECALCULATED", "Charge", today.toString(), "charges=" + count);
        }
        return count;
    }

    private void notifyCharge(Charge charge, String type, LocalDate businessDate, DispatchCounter counter) {
        if (charge.getId() == null || charge.getStudent() == null) {
            counter.skipped++;
            return;
        }

        if (sendWhatsapp) {
            dispatchWhatsapp(charge, type, businessDate, counter);
        }
        if (sendEmail) {
            dispatchEmail(charge, type, businessDate, counter);
        }
    }

    private void dispatchWhatsapp(Charge charge, String type, LocalDate businessDate, DispatchCounter counter) {
        if (alreadySent(charge.getId(), type, "WHATSAPP", businessDate)) {
            counter.skipped++;
            return;
        }

        Student student = charge.getStudent();
        String phone = firstNonBlank(student.getWhatsapp(), student.getPhone(), student.getGuardianPhone());
        String caption = buildWhatsappCaption(charge, type);
        if (phone.isBlank()) {
            saveLog(charge, type, "WHATSAPP", "FAILED", businessDate, caption, null, "Aluno sem WhatsApp/telefone cadastrado.");
            counter.failed++;
            return;
        }

        if (dryRun) {
            saveLog(charge, type, "WHATSAPP", "PREPARED", businessDate, caption, null, null);
            counter.prepared++;
            return;
        }

        String pdfUrl = buildOfficialGuidePdfUrl(charge, true);
        WhatsAppCloudSendResult result = whatsAppCloudApiClient.sendDocumentByLink(
                phone,
                pdfUrl,
                buildGuideFileName(charge),
                caption
        );
        saveLog(
                charge,
                type,
                "WHATSAPP",
                result.isSuccess() ? "SENT" : "FAILED",
                businessDate,
                caption,
                result.getProviderMessageId(),
                result.getErrorMessage()
        );
        if (result.isSuccess()) counter.sent++; else counter.failed++;
    }

    private void dispatchEmail(Charge charge, String type, LocalDate businessDate, DispatchCounter counter) {
        if (alreadySent(charge.getId(), type, "EMAIL", businessDate)) {
            counter.skipped++;
            return;
        }

        Student student = charge.getStudent();
        String email = firstNonBlank(student.getEmail(), student.getGuardianEmail());
        String message = buildEmailMessage(charge, type);
        if (email.isBlank()) {
            saveLog(charge, type, "EMAIL", "FAILED", businessDate, message, null, "Aluno sem e-mail cadastrado.");
            counter.failed++;
            return;
        }

        if (dryRun) {
            saveLog(charge, type, "EMAIL", "PREPARED", businessDate, message, null, null);
            counter.prepared++;
            return;
        }

        GuideFallbackRequest request = new GuideFallbackRequest();
        request.setStudentName(student.getFullName());
        request.setStudentNumber(student.getStudentNumber());
        request.setEmail(email);
        request.setPhoneNumber(firstNonBlank(student.getPhone(), student.getWhatsapp(), student.getGuardianPhone()));
        request.setGuideCode(charge.getChargeCode());
        request.setGuideUrl(buildOfficialGuidePublicUrl(charge));
        request.setAmount(charge.getTotalAmount());
        request.setCurrency(charge.getCurrency());
        request.setDueDate(charge.getDueDate());
        request.setMessage(message);

        Map<String, Object> result = fallbackNotificationService.sendGuideByEmail(request);
        boolean sent = Boolean.TRUE.equals(result.get("sent"));
        saveLog(charge, type, "EMAIL", sent ? "SENT" : String.valueOf(result.getOrDefault("status", "FAILED")), businessDate, message, null, String.valueOf(result.getOrDefault("message", "")));
        if (sent) counter.sent++; else counter.failed++;
    }

    private boolean alreadySent(UUID chargeId, String type, String channel, LocalDate businessDate) {
        return notificationLogRepository.existsByChargeIdAndNotificationTypeAndChannelAndBusinessDateAndStatus(
                chargeId,
                type,
                channel,
                businessDate,
                "SENT"
        );
    }

    private void saveLog(Charge charge, String type, String channel, String status, LocalDate businessDate, String message, String providerMessageId, String errorMessage) {
        NotificationLog log = notificationLogRepository
                .findByChargeIdAndNotificationTypeAndChannelAndBusinessDate(charge.getId(), type, channel, businessDate)
                .orElseGet(NotificationLog::new);

        log.setCharge(charge)
                .setStudent(charge.getStudent())
                .setNotificationType(type)
                .setChannel(channel)
                .setStatus(status)
                .setBusinessDate(businessDate)
                .setMessage(message)
                .setProviderMessageId(providerMessageId)
                .setErrorMessage(clean(errorMessage, null))
                .setSentAt("SENT".equalsIgnoreCase(status) ? LocalDateTime.now(zoneId) : null);
        notificationLogRepository.save(log);
    }

    private String buildWhatsappCaption(Charge charge, String type) {
        Student student = charge.getStudent();
        String intro = switch (type) {
            case "DUE_TOMORROW" -> "Informamos que a sua guia de pagamento referente ao período abaixo já se encontra disponível. O vencimento ocorre amanhã.";
            case "DUE_TODAY" -> "Informamos que a sua guia de pagamento vence hoje. Recomendamos a regularização ainda hoje para evitar juros e outros constrangimentos.";
            case "OVERDUE" -> "Identificamos um pagamento em dívida/pendente. Regularize com urgência para evitar multas, bloqueios ou outros constrangimentos no acesso aos serviços académicos.";
            default -> "Existe uma cobrança académica pendente para regularização.";
        };

        return ("Caro(a) estudante,\n\n"
                + "%s\n\n"
                + "Dados da cobrança:\n\n"
                + "Estudante: %s\n"
                + "Matrícula: %s\n"
                + "Referência: %s\n"
                + "Descrição: %s\n"
                + "Valor base: %s\n"
                + "Multa: %s\n"
                + "Juros: %s\n"
                + "Total a pagar: %s\n"
                + "Data de vencimento: %s\n\n"
                + "A guia oficial segue em anexo neste WhatsApp.\n"
                + "Link público: %s\n\n"
                + "Caso o pagamento já tenha sido realizado, pedimos que envie o respetivo comprovativo para validação e atualização da sua situação financeira.\n\n"
                + "Atenciosamente,\n\n"
                + "Secretaria Financeira\n"
                + "IMETRO\n"
                + "SecretáriaPay Académico")
                .formatted(
                        intro,
                        firstNonBlank(student.getFullName(), "Estudante"),
                        firstNonBlank(student.getStudentNumber(), "-"),
                        firstNonBlank(charge.getReferenceMonth(), charge.getDescription()),
                        firstNonBlank(charge.getDescription(), "Cobrança académica"),
                        money(charge.getAmount(), charge.getCurrency()),
                        money(charge.getFineAmount(), charge.getCurrency()),
                        money(charge.getInterestAmount(), charge.getCurrency()),
                        money(charge.getTotalAmount(), charge.getCurrency()),
                        charge.getDueDate(),
                        buildOfficialGuidePdfUrl(charge, false)
                );
    }

    private String buildEmailMessage(Charge charge, String type) {
        String intro = switch (type) {
            case "DUE_TOMORROW" -> "Informamos que a sua guia de pagamento referente ao período abaixo já se encontra disponível. O vencimento ocorre amanhã.";
            case "DUE_TODAY" -> "Informamos que a sua guia de pagamento vence hoje. Recomendamos a regularização ainda hoje para evitar juros e outros constrangimentos.";
            case "OVERDUE" -> "Identificamos um pagamento em dívida/pendente. Regularize com urgência para evitar multas, bloqueios ou outros constrangimentos no acesso aos serviços académicos.";
            default -> "Existe uma cobrança académica pendente para regularização.";
        };

        return (intro + " Guia oficial emitida pelo SecretáriaPay Académico. "
                + "Referência: " + firstNonBlank(charge.getReferenceMonth(), charge.getDescription()) + ". "
                + "Descrição: " + firstNonBlank(charge.getDescription(), "Cobrança académica") + ". "
                + "Valor base: " + money(charge.getAmount(), charge.getCurrency()) + ". "
                + "Multa: " + money(charge.getFineAmount(), charge.getCurrency()) + ". "
                + "Juros: " + money(charge.getInterestAmount(), charge.getCurrency()) + ". "
                + "Total: " + money(charge.getTotalAmount(), charge.getCurrency()) + ". "
                + "Vencimento: " + charge.getDueDate() + ".");
    }

    private boolean isOpen(Charge charge) {
        if (charge == null || charge.getStatus() == null) return false;
        return charge.getStatus() != ChargeStatus.PAID && charge.getStatus() != ChargeStatus.CANCELLED;
    }

    private Map<String, Object> baseSummary(LocalDate today, String actor) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "OK");
        map.put("actor", clean(actor, "SYSTEM"));
        map.put("businessDate", today.toString());
        map.put("startedAt", LocalDateTime.now(zoneId).toString());
        map.put("enabled", schedulerEnabled);
        return map;
    }

    private String buildOfficialGuidePdfUrl(Charge charge, boolean cacheBust) {
        String base = API_BASE_URL + "/api/v1/public/payment-guides/" + encode(firstNonBlank(charge.getChargeCode(), "GUIDE")) + "/pdf";
        return cacheBust ? base + "?v=" + System.currentTimeMillis() : base;
    }

    private String buildOfficialGuidePublicUrl(Charge charge) {
        return PANEL_BASE_URL + "/guias/" + encode(firstNonBlank(charge.getChargeCode(), "GUIDE"));
    }

    private String buildGuideFileName(Charge charge) {
        String studentNumber = charge.getStudent() != null ? firstNonBlank(charge.getStudent().getStudentNumber(), "estudante") : "estudante";
        return "Guia_Pagamento_Academico_" + sanitizeFilePart(studentNumber) + "_" + sanitizeFilePart(firstNonBlank(charge.getChargeCode(), "GUIDE")) + ".pdf";
    }

    private String encode(String value) {
        return URLEncoder.encode(firstNonBlank(value), StandardCharsets.UTF_8);
    }

    private String sanitizeFilePart(String value) {
        String sanitized = clean(value, "documento")
                .replaceAll("[^A-Za-z0-9._-]", "-")
                .replaceAll("-+", "-");
        return sanitized.isBlank() ? "documento" : sanitized;
    }

    private String money(BigDecimal value, String currency) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        String suffix = currency == null || currency.isBlank() ? "AOA" : currency;
        return String.format(Locale.forLanguageTag("pt-AO"), "%,.2f", safeValue).replace(',', '#').replace('.', ',').replace('#', '.') + " " + ("AOA".equalsIgnoreCase(suffix) ? "Kz" : suffix);
    }

    private String formatMonth(LocalDate date) {
        if (date == null) return "";
        return String.format("%02d/%d", date.getMonthValue(), date.getYear());
    }

    private String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isBlank()) return value.trim();
        }
        return "";
    }

    private String clean(String value, String fallback) {
        if (value == null || value.trim().isBlank()) return fallback == null ? "" : fallback;
        return value.trim();
    }

    private static class DispatchCounter {
        int sent;
        int prepared;
        int failed;
        int skipped;
    }
}
