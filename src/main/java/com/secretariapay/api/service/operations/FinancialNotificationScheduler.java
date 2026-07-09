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
        String message = buildMessage(charge, type);
        if (phone.isBlank()) {
            saveLog(charge, type, "WHATSAPP", "FAILED", businessDate, message, null, "Aluno sem WhatsApp/telefone cadastrado.");
            counter.failed++;
            return;
        }

        if (dryRun) {
            saveLog(charge, type, "WHATSAPP", "PREPARED", businessDate, message, null, null);
            counter.prepared++;
            return;
        }

        WhatsAppCloudSendResult result = whatsAppCloudApiClient.sendText(phone, message);
        saveLog(
                charge,
                type,
                "WHATSAPP",
                result.isSuccess() ? "SENT" : "FAILED",
                businessDate,
                message,
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
        String message = buildMessage(charge, type);
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

    private String buildMessage(Charge charge, String type) {
        Student student = charge.getStudent();
        String greeting = "Olá, " + firstNonBlank(student.getFullName(), "estudante") + ".";
        String intro = switch (type) {
            case "DUE_TOMORROW" -> "A sua mensalidade vence amanhã.";
            case "DUE_TODAY" -> "A sua mensalidade vence hoje.";
            case "OVERDUE" -> "Identificamos uma mensalidade em aberto/vencida.";
            default -> "Existe uma cobrança académica em aberto.";
        };

        return ("""
                %s

                %s

                Referência: %s
                Vencimento: %s
                Valor base: %s
                Multa: %s
                Juros: %s
                Total atualizado: %s

                Para regularizar, responda no WhatsApp:
                1 - Gerar guia de pagamento

                Caso já tenha pago, por favor desconsidere esta mensagem.
                SecretáriaPay Académico · IMETRO/DCR
                """).formatted(
                greeting,
                intro,
                firstNonBlank(charge.getReferenceMonth(), charge.getDescription()),
                charge.getDueDate(),
                money(charge.getAmount(), charge.getCurrency()),
                money(charge.getFineAmount(), charge.getCurrency()),
                money(charge.getInterestAmount(), charge.getCurrency()),
                money(charge.getTotalAmount(), charge.getCurrency())
        ).trim();
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
