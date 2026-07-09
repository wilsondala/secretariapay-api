package com.secretariapay.api.controller.operations;

import com.secretariapay.api.service.operations.AuditService;
import com.secretariapay.api.service.operations.FinancialNotificationScheduler;
import com.secretariapay.api.service.operations.PaymentTransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/operations")
public class InstitutionalOperationsController {

    private final FinancialNotificationScheduler financialNotificationScheduler;
    private final AuditService auditService;
    private final PaymentTransactionService paymentTransactionService;

    public InstitutionalOperationsController(
            FinancialNotificationScheduler financialNotificationScheduler,
            AuditService auditService,
            PaymentTransactionService paymentTransactionService
    ) {
        this.financialNotificationScheduler = financialNotificationScheduler;
        this.auditService = auditService;
        this.paymentTransactionService = paymentTransactionService;
    }

    @PostMapping("/notifications/run")
    public ResponseEntity<Map<String, Object>> runNotificationsNow() {
        return ResponseEntity.ok(financialNotificationScheduler.runDailyNotifications("ADMIN_MANUAL"));
    }

    @GetMapping("/notifications/logs")
    public ResponseEntity<?> notificationLogs() {
        return ResponseEntity.ok(financialNotificationScheduler.recentLogs().stream().map(log -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", log.getId());
            item.put("notificationType", log.getNotificationType());
            item.put("channel", log.getChannel());
            item.put("status", log.getStatus());
            item.put("businessDate", log.getBusinessDate());
            item.put("sentAt", log.getSentAt());
            item.put("createdAt", log.getCreatedAt());
            item.put("errorMessage", log.getErrorMessage());
            return item;
        }).toList());
    }

    @GetMapping("/audit/logs")
    public ResponseEntity<?> auditLogs() {
        return ResponseEntity.ok(auditService.recent().stream().map(log -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", log.getId());
            item.put("actor", log.getActor());
            item.put("action", log.getAction());
            item.put("entityType", log.getEntityType());
            item.put("entityId", log.getEntityId());
            item.put("details", log.getDetails());
            item.put("createdAt", log.getCreatedAt());
            return item;
        }).toList());
    }

    @GetMapping("/payments/transactions")
    public ResponseEntity<?> paymentTransactions() {
        return ResponseEntity.ok(paymentTransactionService.recent().stream().map(transaction -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", transaction.getId());
            item.put("provider", transaction.getProvider());
            item.put("providerTransactionId", transaction.getProviderTransactionId());
            item.put("merchantTransactionId", transaction.getMerchantTransactionId());
            item.put("paymentMethod", transaction.getPaymentMethod());
            item.put("amount", transaction.getAmount());
            item.put("currency", transaction.getCurrency());
            item.put("status", transaction.getStatus());
            item.put("paidAt", transaction.getPaidAt());
            item.put("createdAt", transaction.getCreatedAt());
            return item;
        }).toList());
    }

    @GetMapping("/readiness")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "READY");
        body.put("automation", "FinancialNotificationScheduler instalado");
        body.put("audit", "AuditLog instalado");
        body.put("reconciliation", "PaymentTransaction instalado");
        body.put("nextStep", "Ativar SECRETARIAPAY_NOTIFICATIONS_SCHEDULER_ENABLED=true em produção após teste controlado.");
        return ResponseEntity.ok(body);
    }
}
