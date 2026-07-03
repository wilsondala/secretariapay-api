package com.secretariapay.api.dto.financial;

import java.math.BigDecimal;

public class MockAutomaticPaymentRequest {

    private String paymentMethod;
    private String settlementStatus;
    private BigDecimal amount;
    private String externalTransactionId;
    private String payerName;
    private String payerPhone;
    private String bankName;
    private String bankReference;
    private String note;

    public String getPaymentMethod() { return paymentMethod; }
    public MockAutomaticPaymentRequest setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; return this; }
    public String getSettlementStatus() { return settlementStatus; }
    public MockAutomaticPaymentRequest setSettlementStatus(String settlementStatus) { this.settlementStatus = settlementStatus; return this; }
    public BigDecimal getAmount() { return amount; }
    public MockAutomaticPaymentRequest setAmount(BigDecimal amount) { this.amount = amount; return this; }
    public String getExternalTransactionId() { return externalTransactionId; }
    public MockAutomaticPaymentRequest setExternalTransactionId(String externalTransactionId) { this.externalTransactionId = externalTransactionId; return this; }
    public String getPayerName() { return payerName; }
    public MockAutomaticPaymentRequest setPayerName(String payerName) { this.payerName = payerName; return this; }
    public String getPayerPhone() { return payerPhone; }
    public MockAutomaticPaymentRequest setPayerPhone(String payerPhone) { this.payerPhone = payerPhone; return this; }
    public String getBankName() { return bankName; }
    public MockAutomaticPaymentRequest setBankName(String bankName) { this.bankName = bankName; return this; }
    public String getBankReference() { return bankReference; }
    public MockAutomaticPaymentRequest setBankReference(String bankReference) { this.bankReference = bankReference; return this; }
    public String getNote() { return note; }
    public MockAutomaticPaymentRequest setNote(String note) { this.note = note; return this; }
}
