package com.secretariapay.api.dto.financial;

import com.secretariapay.api.entity.enums.financial.ReceiptStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class ReceiptResponse {

    private UUID id;
    private UUID chargeId;
    private String chargeCode;
    private String studentName;
    private String studentNumber;
    private String referenceMonth;
    private LocalDate dueDate;
    private BigDecimal amount;
    private BigDecimal baseAmount;
    private BigDecimal fineAmount;
    private BigDecimal interestAmount;
    private BigDecimal discountAmount;
    private String currency;
    private String paymentMethod;
    private LocalDateTime paidAt;
    private String receiptCode;
    private String pdfUrl;
    private String qrCodeUrl;
    private String validationUrl;
    private ReceiptStatus status;
    private LocalDateTime issuedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public ReceiptResponse setId(UUID id) { this.id = id; return this; }

    public UUID getChargeId() { return chargeId; }
    public ReceiptResponse setChargeId(UUID chargeId) { this.chargeId = chargeId; return this; }

    public String getChargeCode() { return chargeCode; }
    public ReceiptResponse setChargeCode(String chargeCode) { this.chargeCode = chargeCode; return this; }

    public String getStudentName() { return studentName; }
    public ReceiptResponse setStudentName(String studentName) { this.studentName = studentName; return this; }

    public String getStudentNumber() { return studentNumber; }
    public ReceiptResponse setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; return this; }

    public String getReferenceMonth() { return referenceMonth; }
    public ReceiptResponse setReferenceMonth(String referenceMonth) { this.referenceMonth = referenceMonth; return this; }

    public LocalDate getDueDate() { return dueDate; }
    public ReceiptResponse setDueDate(LocalDate dueDate) { this.dueDate = dueDate; return this; }

    public BigDecimal getAmount() { return amount; }
    public ReceiptResponse setAmount(BigDecimal amount) { this.amount = amount; return this; }

    public BigDecimal getBaseAmount() { return baseAmount; }
    public ReceiptResponse setBaseAmount(BigDecimal baseAmount) { this.baseAmount = baseAmount; return this; }

    public BigDecimal getFineAmount() { return fineAmount; }
    public ReceiptResponse setFineAmount(BigDecimal fineAmount) { this.fineAmount = fineAmount; return this; }

    public BigDecimal getInterestAmount() { return interestAmount; }
    public ReceiptResponse setInterestAmount(BigDecimal interestAmount) { this.interestAmount = interestAmount; return this; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public ReceiptResponse setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; return this; }

    public String getCurrency() { return currency; }
    public ReceiptResponse setCurrency(String currency) { this.currency = currency; return this; }

    public String getPaymentMethod() { return paymentMethod; }
    public ReceiptResponse setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; return this; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public ReceiptResponse setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; return this; }

    public String getReceiptCode() { return receiptCode; }
    public ReceiptResponse setReceiptCode(String receiptCode) { this.receiptCode = receiptCode; return this; }

    public String getPdfUrl() { return pdfUrl; }
    public ReceiptResponse setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; return this; }

    public String getQrCodeUrl() { return qrCodeUrl; }
    public ReceiptResponse setQrCodeUrl(String qrCodeUrl) { this.qrCodeUrl = qrCodeUrl; return this; }

    public String getValidationUrl() { return validationUrl; }
    public ReceiptResponse setValidationUrl(String validationUrl) { this.validationUrl = validationUrl; return this; }

    public ReceiptStatus getStatus() { return status; }
    public ReceiptResponse setStatus(ReceiptStatus status) { this.status = status; return this; }

    public LocalDateTime getIssuedAt() { return issuedAt; }
    public ReceiptResponse setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; return this; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public ReceiptResponse setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; return this; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public ReceiptResponse setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public ReceiptResponse setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
}
