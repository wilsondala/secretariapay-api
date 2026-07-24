package com.secretariapay.api.entity.enrollment;

import com.secretariapay.api.entity.enums.enrollment.EnrollmentInvoiceStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "academic_enrollment_invoices")
public class AcademicEnrollmentInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_request_id", nullable = false, unique = true)
    private AcademicEnrollmentRequest enrollmentRequest;

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
    private EnrollmentInvoiceStatus status = EnrollmentInvoiceStatus.PENDING;

    @Column(name = "payment_method", length = 60)
    private String paymentMethod;

    @Column(name = "payment_reference", length = 180)
    private String paymentReference;

    @Column(length = 80)
    private String provider;

    @Column(name = "external_transaction_id", length = 180)
    private String externalTransactionId;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (currency == null || currency.isBlank()) currency = "AOA";
        if (status == null) status = EnrollmentInvoiceStatus.PENDING;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        if (currency == null || currency.isBlank()) currency = "AOA";
        if (status == null) status = EnrollmentInvoiceStatus.PENDING;
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public AcademicEnrollmentRequest getEnrollmentRequest() { return enrollmentRequest; }
    public AcademicEnrollmentInvoice setEnrollmentRequest(AcademicEnrollmentRequest enrollmentRequest) { this.enrollmentRequest = enrollmentRequest; return this; }
    public String getInvoiceCode() { return invoiceCode; }
    public AcademicEnrollmentInvoice setInvoiceCode(String invoiceCode) { this.invoiceCode = invoiceCode; return this; }
    public BigDecimal getAmount() { return amount; }
    public AcademicEnrollmentInvoice setAmount(BigDecimal amount) { this.amount = amount; return this; }
    public String getCurrency() { return currency; }
    public AcademicEnrollmentInvoice setCurrency(String currency) { this.currency = currency; return this; }
    public LocalDate getDueDate() { return dueDate; }
    public AcademicEnrollmentInvoice setDueDate(LocalDate dueDate) { this.dueDate = dueDate; return this; }
    public EnrollmentInvoiceStatus getStatus() { return status; }
    public AcademicEnrollmentInvoice setStatus(EnrollmentInvoiceStatus status) { this.status = status; return this; }
    public String getPaymentMethod() { return paymentMethod; }
    public AcademicEnrollmentInvoice setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; return this; }
    public String getPaymentReference() { return paymentReference; }
    public AcademicEnrollmentInvoice setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; return this; }
    public String getProvider() { return provider; }
    public AcademicEnrollmentInvoice setProvider(String provider) { this.provider = provider; return this; }
    public String getExternalTransactionId() { return externalTransactionId; }
    public AcademicEnrollmentInvoice setExternalTransactionId(String externalTransactionId) { this.externalTransactionId = externalTransactionId; return this; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public AcademicEnrollmentInvoice setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; return this; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
