package com.secretariapay.api.entity.operations;

import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.financial.Charge;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "payment_transactions",
        uniqueConstraints = @UniqueConstraint(name = "uk_payment_provider_transaction", columnNames = {"provider", "provider_transaction_id"})
)
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charge_id")
    private Charge charge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(nullable = false, length = 40)
    private String provider;

    @Column(name = "provider_transaction_id", length = 160)
    private String providerTransactionId;

    @Column(name = "merchant_transaction_id", length = 80)
    private String merchantTransactionId;

    @Column(name = "payment_method", length = 80)
    private String paymentMethod;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(nullable = false, length = 10)
    private String currency = "AOA";

    @Column(nullable = false, length = 40)
    private String status;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        normalize();
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        normalize();
        updatedAt = LocalDateTime.now();
    }

    private void normalize() {
        amount = (amount == null ? BigDecimal.ZERO : amount).setScale(2, RoundingMode.HALF_UP);
        if (currency == null || currency.isBlank()) currency = "AOA";
        if (status == null || status.isBlank()) status = "PENDING";
    }

    public UUID getId() { return id; }
    public Charge getCharge() { return charge; }
    public PaymentTransaction setCharge(Charge charge) { this.charge = charge; return this; }
    public Student getStudent() { return student; }
    public PaymentTransaction setStudent(Student student) { this.student = student; return this; }
    public String getProvider() { return provider; }
    public PaymentTransaction setProvider(String provider) { this.provider = provider; return this; }
    public String getProviderTransactionId() { return providerTransactionId; }
    public PaymentTransaction setProviderTransactionId(String providerTransactionId) { this.providerTransactionId = providerTransactionId; return this; }
    public String getMerchantTransactionId() { return merchantTransactionId; }
    public PaymentTransaction setMerchantTransactionId(String merchantTransactionId) { this.merchantTransactionId = merchantTransactionId; return this; }
    public String getPaymentMethod() { return paymentMethod; }
    public PaymentTransaction setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; return this; }
    public BigDecimal getAmount() { return amount; }
    public PaymentTransaction setAmount(BigDecimal amount) { this.amount = amount; return this; }
    public String getCurrency() { return currency; }
    public PaymentTransaction setCurrency(String currency) { this.currency = currency; return this; }
    public String getStatus() { return status; }
    public PaymentTransaction setStatus(String status) { this.status = status; return this; }
    public String getRawPayload() { return rawPayload; }
    public PaymentTransaction setRawPayload(String rawPayload) { this.rawPayload = rawPayload; return this; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public PaymentTransaction setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; return this; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
