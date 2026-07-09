package com.secretariapay.api.service.operations;

import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.entity.operations.PaymentTransaction;
import com.secretariapay.api.repository.operations.PaymentTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentTransactionService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final AuditService auditService;

    public PaymentTransactionService(PaymentTransactionRepository paymentTransactionRepository, AuditService auditService) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.auditService = auditService;
    }

    @Transactional
    public PaymentTransaction recordIfAbsent(
            String provider,
            String providerTransactionId,
            String merchantTransactionId,
            String paymentMethod,
            BigDecimal amount,
            String currency,
            String status,
            Charge charge,
            Student student,
            String rawPayload,
            LocalDateTime paidAt
    ) {
        String safeProvider = clean(provider, "UNKNOWN");
        String safeProviderTransactionId = clean(providerTransactionId, merchantTransactionId);

        if (!safeProviderTransactionId.isBlank()) {
            return paymentTransactionRepository
                    .findByProviderAndProviderTransactionId(safeProvider, safeProviderTransactionId)
                    .orElseGet(() -> create(safeProvider, safeProviderTransactionId, merchantTransactionId, paymentMethod, amount, currency, status, charge, student, rawPayload, paidAt));
        }

        return create(safeProvider, safeProviderTransactionId, merchantTransactionId, paymentMethod, amount, currency, status, charge, student, rawPayload, paidAt);
    }

    public List<PaymentTransaction> recent() {
        return paymentTransactionRepository.findTop100ByOrderByCreatedAtDesc();
    }

    private PaymentTransaction create(
            String provider,
            String providerTransactionId,
            String merchantTransactionId,
            String paymentMethod,
            BigDecimal amount,
            String currency,
            String status,
            Charge charge,
            Student student,
            String rawPayload,
            LocalDateTime paidAt
    ) {
        PaymentTransaction transaction = new PaymentTransaction()
                .setProvider(provider)
                .setProviderTransactionId(clean(providerTransactionId, null))
                .setMerchantTransactionId(clean(merchantTransactionId, null))
                .setPaymentMethod(clean(paymentMethod, null))
                .setAmount(amount == null ? BigDecimal.ZERO : amount)
                .setCurrency(clean(currency, "AOA"))
                .setStatus(clean(status, "PENDING"))
                .setCharge(charge)
                .setStudent(student)
                .setRawPayload(clean(rawPayload, null))
                .setPaidAt(paidAt);

        PaymentTransaction saved = paymentTransactionRepository.save(transaction);
        auditService.record("SYSTEM", "PAYMENT_TRANSACTION_RECORDED", "PaymentTransaction", saved.getId().toString(), "provider=" + provider + ", merchant=" + clean(merchantTransactionId, ""));
        return saved;
    }

    private String clean(String value, String fallback) {
        if (value == null || value.trim().isBlank()) return fallback == null ? "" : fallback;
        return value.trim();
    }
}
