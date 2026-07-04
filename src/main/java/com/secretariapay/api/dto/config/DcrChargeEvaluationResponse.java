package com.secretariapay.api.dto.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class DcrChargeEvaluationResponse {

    private UUID institutionId;
    private String policyCode;
    private String currency;
    private BigDecimal baseAmount;
    private BigDecimal penaltyPercent;
    private BigDecimal penaltyAmount;
    private BigDecimal totalAmount;
    private LocalDate dueDate;
    private LocalDate referenceDate;
    private Long daysLate;
    private String status;
    private Boolean debt;
    private Boolean delinquent;
    private Boolean canSendWhatsappNow;
    private Boolean provisionalAutomaticConfirmation;
    private Boolean manualDcrConfirmationRequired;
    private String dcrApprovalRole;
    private String message;

    public UUID getInstitutionId() { return institutionId; }
    public DcrChargeEvaluationResponse setInstitutionId(UUID institutionId) { this.institutionId = institutionId; return this; }
    public String getPolicyCode() { return policyCode; }
    public DcrChargeEvaluationResponse setPolicyCode(String policyCode) { this.policyCode = policyCode; return this; }
    public String getCurrency() { return currency; }
    public DcrChargeEvaluationResponse setCurrency(String currency) { this.currency = currency; return this; }
    public BigDecimal getBaseAmount() { return baseAmount; }
    public DcrChargeEvaluationResponse setBaseAmount(BigDecimal baseAmount) { this.baseAmount = baseAmount; return this; }
    public BigDecimal getPenaltyPercent() { return penaltyPercent; }
    public DcrChargeEvaluationResponse setPenaltyPercent(BigDecimal penaltyPercent) { this.penaltyPercent = penaltyPercent; return this; }
    public BigDecimal getPenaltyAmount() { return penaltyAmount; }
    public DcrChargeEvaluationResponse setPenaltyAmount(BigDecimal penaltyAmount) { this.penaltyAmount = penaltyAmount; return this; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public DcrChargeEvaluationResponse setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; return this; }
    public LocalDate getDueDate() { return dueDate; }
    public DcrChargeEvaluationResponse setDueDate(LocalDate dueDate) { this.dueDate = dueDate; return this; }
    public LocalDate getReferenceDate() { return referenceDate; }
    public DcrChargeEvaluationResponse setReferenceDate(LocalDate referenceDate) { this.referenceDate = referenceDate; return this; }
    public Long getDaysLate() { return daysLate; }
    public DcrChargeEvaluationResponse setDaysLate(Long daysLate) { this.daysLate = daysLate; return this; }
    public String getStatus() { return status; }
    public DcrChargeEvaluationResponse setStatus(String status) { this.status = status; return this; }
    public Boolean getDebt() { return debt; }
    public DcrChargeEvaluationResponse setDebt(Boolean debt) { this.debt = debt; return this; }
    public Boolean getDelinquent() { return delinquent; }
    public DcrChargeEvaluationResponse setDelinquent(Boolean delinquent) { this.delinquent = delinquent; return this; }
    public Boolean getCanSendWhatsappNow() { return canSendWhatsappNow; }
    public DcrChargeEvaluationResponse setCanSendWhatsappNow(Boolean canSendWhatsappNow) { this.canSendWhatsappNow = canSendWhatsappNow; return this; }
    public Boolean getProvisionalAutomaticConfirmation() { return provisionalAutomaticConfirmation; }
    public DcrChargeEvaluationResponse setProvisionalAutomaticConfirmation(Boolean provisionalAutomaticConfirmation) { this.provisionalAutomaticConfirmation = provisionalAutomaticConfirmation; return this; }
    public Boolean getManualDcrConfirmationRequired() { return manualDcrConfirmationRequired; }
    public DcrChargeEvaluationResponse setManualDcrConfirmationRequired(Boolean manualDcrConfirmationRequired) { this.manualDcrConfirmationRequired = manualDcrConfirmationRequired; return this; }
    public String getDcrApprovalRole() { return dcrApprovalRole; }
    public DcrChargeEvaluationResponse setDcrApprovalRole(String dcrApprovalRole) { this.dcrApprovalRole = dcrApprovalRole; return this; }
    public String getMessage() { return message; }
    public DcrChargeEvaluationResponse setMessage(String message) { this.message = message; return this; }
}
