package com.secretariapay.api.dto.financial;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TuitionChargeGenerationResponse {

    private UUID institutionId;
    private String academicYear;
    private String referenceMonth;
    private LocalDate dueDate;
    private LocalDate referenceDate;
    private String serviceCode;
    private String currency;
    private BigDecimal baseAmount;
    private BigDecimal penaltyPercent;
    private BigDecimal fineAmountPerStudent;
    private BigDecimal discountAmountPerStudent;
    private BigDecimal totalAmountPerStudent;
    private Integer selectedStudents = 0;
    private Integer createdCharges = 0;
    private Integer reusedCharges = 0;
    private Integer skippedStudents = 0;
    private Boolean provisionalAutomaticConfirmation = true;
    private Boolean manualDcrConfirmationRequired = true;
    private String dcrApprovalRole;
    private String status;
    private String message;
    private List<TuitionChargeGeneratedItem> items = new ArrayList<>();

    public UUID getInstitutionId() { return institutionId; }
    public TuitionChargeGenerationResponse setInstitutionId(UUID institutionId) { this.institutionId = institutionId; return this; }
    public String getAcademicYear() { return academicYear; }
    public TuitionChargeGenerationResponse setAcademicYear(String academicYear) { this.academicYear = academicYear; return this; }
    public String getReferenceMonth() { return referenceMonth; }
    public TuitionChargeGenerationResponse setReferenceMonth(String referenceMonth) { this.referenceMonth = referenceMonth; return this; }
    public LocalDate getDueDate() { return dueDate; }
    public TuitionChargeGenerationResponse setDueDate(LocalDate dueDate) { this.dueDate = dueDate; return this; }
    public LocalDate getReferenceDate() { return referenceDate; }
    public TuitionChargeGenerationResponse setReferenceDate(LocalDate referenceDate) { this.referenceDate = referenceDate; return this; }
    public String getServiceCode() { return serviceCode; }
    public TuitionChargeGenerationResponse setServiceCode(String serviceCode) { this.serviceCode = serviceCode; return this; }
    public String getCurrency() { return currency; }
    public TuitionChargeGenerationResponse setCurrency(String currency) { this.currency = currency; return this; }
    public BigDecimal getBaseAmount() { return baseAmount; }
    public TuitionChargeGenerationResponse setBaseAmount(BigDecimal baseAmount) { this.baseAmount = baseAmount; return this; }
    public BigDecimal getPenaltyPercent() { return penaltyPercent; }
    public TuitionChargeGenerationResponse setPenaltyPercent(BigDecimal penaltyPercent) { this.penaltyPercent = penaltyPercent; return this; }
    public BigDecimal getFineAmountPerStudent() { return fineAmountPerStudent; }
    public TuitionChargeGenerationResponse setFineAmountPerStudent(BigDecimal fineAmountPerStudent) { this.fineAmountPerStudent = fineAmountPerStudent; return this; }
    public BigDecimal getDiscountAmountPerStudent() { return discountAmountPerStudent; }
    public TuitionChargeGenerationResponse setDiscountAmountPerStudent(BigDecimal discountAmountPerStudent) { this.discountAmountPerStudent = discountAmountPerStudent; return this; }
    public BigDecimal getTotalAmountPerStudent() { return totalAmountPerStudent; }
    public TuitionChargeGenerationResponse setTotalAmountPerStudent(BigDecimal totalAmountPerStudent) { this.totalAmountPerStudent = totalAmountPerStudent; return this; }
    public Integer getSelectedStudents() { return selectedStudents; }
    public TuitionChargeGenerationResponse setSelectedStudents(Integer selectedStudents) { this.selectedStudents = selectedStudents; return this; }
    public Integer getCreatedCharges() { return createdCharges; }
    public TuitionChargeGenerationResponse setCreatedCharges(Integer createdCharges) { this.createdCharges = createdCharges; return this; }
    public Integer getReusedCharges() { return reusedCharges; }
    public TuitionChargeGenerationResponse setReusedCharges(Integer reusedCharges) { this.reusedCharges = reusedCharges; return this; }
    public Integer getSkippedStudents() { return skippedStudents; }
    public TuitionChargeGenerationResponse setSkippedStudents(Integer skippedStudents) { this.skippedStudents = skippedStudents; return this; }
    public Boolean getProvisionalAutomaticConfirmation() { return provisionalAutomaticConfirmation; }
    public TuitionChargeGenerationResponse setProvisionalAutomaticConfirmation(Boolean provisionalAutomaticConfirmation) { this.provisionalAutomaticConfirmation = provisionalAutomaticConfirmation; return this; }
    public Boolean getManualDcrConfirmationRequired() { return manualDcrConfirmationRequired; }
    public TuitionChargeGenerationResponse setManualDcrConfirmationRequired(Boolean manualDcrConfirmationRequired) { this.manualDcrConfirmationRequired = manualDcrConfirmationRequired; return this; }
    public String getDcrApprovalRole() { return dcrApprovalRole; }
    public TuitionChargeGenerationResponse setDcrApprovalRole(String dcrApprovalRole) { this.dcrApprovalRole = dcrApprovalRole; return this; }
    public String getStatus() { return status; }
    public TuitionChargeGenerationResponse setStatus(String status) { this.status = status; return this; }
    public String getMessage() { return message; }
    public TuitionChargeGenerationResponse setMessage(String message) { this.message = message; return this; }
    public List<TuitionChargeGeneratedItem> getItems() { return items; }
    public TuitionChargeGenerationResponse setItems(List<TuitionChargeGeneratedItem> items) { this.items = items; return this; }
}
