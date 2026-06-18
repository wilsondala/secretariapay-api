package com.vairapido.api.entity;

import com.vairapido.api.entity.enums.PaymentMethod;
import com.vairapido.api.entity.enums.PaymentStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "payment_code", nullable = false, unique = true, length = 50)
    private String paymentCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency = "BRL";

    @Column(name = "pix_copy_paste", columnDefinition = "TEXT")
    private String pixCopyPaste;

    @Column(name = "pix_qr_code_url", columnDefinition = "TEXT")
    private String pixQrCodeUrl;

    @Column(name = "gateway_name", length = 80)
    private String gatewayName;

    @Column(name = "gateway_transaction_id", length = 120)
    private String gatewayTransactionId;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = PaymentStatus.PENDING;
        }

        if (this.currency == null || this.currency.isBlank()) {
            this.currency = "BRL";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Booking getBooking() {
        return booking;
    }

    public Payment setBooking(Booking booking) {
        this.booking = booking;
        return this;
    }

    public String getPaymentCode() {
        return paymentCode;
    }

    public Payment setPaymentCode(String paymentCode) {
        this.paymentCode = paymentCode;
        return this;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public Payment setMethod(PaymentMethod method) {
        this.method = method;
        return this;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Payment setStatus(PaymentStatus status) {
        this.status = status;
        return this;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Payment setAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public Payment setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public String getPixCopyPaste() {
        return pixCopyPaste;
    }

    public Payment setPixCopyPaste(String pixCopyPaste) {
        this.pixCopyPaste = pixCopyPaste;
        return this;
    }

    public String getPixQrCodeUrl() {
        return pixQrCodeUrl;
    }

    public Payment setPixQrCodeUrl(String pixQrCodeUrl) {
        this.pixQrCodeUrl = pixQrCodeUrl;
        return this;
    }

    public String getGatewayName() {
        return gatewayName;
    }

    public Payment setGatewayName(String gatewayName) {
        this.gatewayName = gatewayName;
        return this;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public Payment setGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
        return this;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public Payment setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public Payment setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
        return this;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public Payment setCancelledAt(LocalDateTime cancelledAt) {
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