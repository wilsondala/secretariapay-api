package com.secretariapay.api.entity.admission;

import com.secretariapay.api.entity.enums.admission.AdmissionInvoiceStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "admission_invoices")
public class AdmissionInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private AdmissionApplication application;

    @Column(name = "invoice_code", nullable = false, unique = true, length = 80)
    private String invoiceCode;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency = "AOA";

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AdmissionInvoiceStatus status = AdmissionInvoiceStatus.PENDING;

    @Column(name = "payment_method", length = 80)
    private String paymentMethod;

    @Column(name = "payment_reference", length = 120)
    private String paymentReference;

    @Column(length = 80)
    private String provider;

    @Column(name = "external_transaction_id", length = 160)
    private String externalTransactionId;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (currency == null || currency.isBlank()) currency = "AOA";
        if (status == null) status = AdmissionInvoiceStatus.PENDING;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        if (currency == null || currency.isBlank()) currency = "AOA";
        if (status == null) status = AdmissionInvoiceStatus.PENDING;
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public AdmissionApplication getApplication() { return application; }
    public AdmissionInvoice setApplication(AdmissionApplication application) { this.application = application; return this; }
    public String getInvoiceCode() { return invoiceCode; }
    public AdmissionInvoice setInvoiceCode(String invoiceCode) { this.invoiceCode = invoiceCode; return this; }
    public BigDecimal getAmount() { return amount; }
    public AdmissionInvoice setAmount(BigDecimal amount) { this.amount = amount; return this; }
    public String getCurrency() { return currency; }
    public AdmissionInvoice setCurrency(String currency) { this.currency = currency; return this; }
    public LocalDate getDueDate() { return dueDate; }
    public AdmissionInvoice setDueDate(LocalDate dueDate) { this.dueDate = dueDate; return this; }
    public AdmissionInvoiceStatus getStatus() { return status; }
    public AdmissionInvoice setStatus(AdmissionInvoiceStatus status) { this.status = status; return this; }
    public String getPaymentMethod() { return paymentMethod; }
    public AdmissionInvoice setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; return this; }
    public String getPaymentReference() { return paymentReference; }
    public AdmissionInvoice setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; return this; }
    public String getProvider() { return provider; }
    public AdmissionInvoice setProvider(String provider) { this.provider = provider; return this; }
    public String getExternalTransactionId() { return externalTransactionId; }
    public AdmissionInvoice setExternalTransactionId(String externalTransactionId) { this.externalTransactionId = externalTransactionId; return this; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public AdmissionInvoice setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; return this; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
