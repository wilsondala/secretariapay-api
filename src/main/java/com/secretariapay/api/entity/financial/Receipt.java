package com.secretariapay.api.entity.financial;

import com.secretariapay.api.entity.enums.financial.ReceiptStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "receipts")
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charge_id", nullable = false, unique = true)
    private Charge charge;

    @Column(name = "receipt_code", nullable = false, unique = true, length = 80)
    private String receiptCode;

    @Column(name = "pdf_url", columnDefinition = "TEXT")
    private String pdfUrl;

    @Column(name = "qr_code_url", columnDefinition = "TEXT")
    private String qrCodeUrl;

    @Column(name = "validation_url", columnDefinition = "TEXT")
    private String validationUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReceiptStatus status = ReceiptStatus.VALID;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (issuedAt == null) {
            issuedAt = now;
        }

        if (status == null) {
            status = ReceiptStatus.VALID;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();

        if (status == null) {
            status = ReceiptStatus.VALID;
        }
    }

    public UUID getId() {
        return id;
    }

    public Charge getCharge() {
        return charge;
    }

    public Receipt setCharge(Charge charge) {
        this.charge = charge;
        return this;
    }

    public String getReceiptCode() {
        return receiptCode;
    }

    public Receipt setReceiptCode(String receiptCode) {
        this.receiptCode = receiptCode;
        return this;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public Receipt setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
        return this;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public Receipt setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
        return this;
    }

    public String getValidationUrl() {
        return validationUrl;
    }

    public Receipt setValidationUrl(String validationUrl) {
        this.validationUrl = validationUrl;
        return this;
    }

    public ReceiptStatus getStatus() {
        return status;
    }

    public Receipt setStatus(ReceiptStatus status) {
        this.status = status;
        return this;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public Receipt setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
        return this;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public Receipt setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}