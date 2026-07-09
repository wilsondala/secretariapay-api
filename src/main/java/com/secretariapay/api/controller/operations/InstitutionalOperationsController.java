package com.secretariapay.api.controller.operations;

import com.secretariapay.api.entity.operations.AuditLog;
import com.secretariapay.api.entity.operations.NotificationLog;
import com.secretariapay.api.entity.operations.PaymentTransaction;
import com.secretariapay.api.service.operations.AuditService;
import com.secretariapay.api.service.operations.FinancialNotificationScheduler;
import com.secretariapay.api.service.operations.PaymentTransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
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
    public ResponseEntity<List<NotificationLog>> notificationLogs() {
        return ResponseEntity.ok(financialNotificationScheduler.recentLogs());
    }

    @GetMapping("/audit/logs")
    public ResponseEntity<List<AuditLog>> auditLogs() {
        return ResponseEntity.ok(auditService.recent());
    }

    @GetMapping("/payments/transactions")
    public ResponseEntity<List<PaymentTransaction>> paymentTransactions() {
        return ResponseEntity.ok(paymentTransactionService.recent());
    }

    @GetMapping("/readiness")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "READY");
        body.put("automation", "FinancialNotificationScheduler instalado");
        body.put("audit", "AuditLog instalado");
        body.put("reconciliation", "PaymentTransaction instalado");
        body.put("security", "Configuração endurecida por variáveis de ambiente");
        body.put("nextStep", "Ativar SECRETARIAPAY_NOTIFICATIONS_SCHEDULER_ENABLED=true em produção após teste controlado.");
        return ResponseEntity.ok(body);
    }
}
