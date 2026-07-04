package com.secretariapay.api.dto.financial;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class TuitionChargeGenerationRequest {

    @NotNull(message = "A instituição é obrigatória.")
    private UUID institutionId;

    private String academicYear;

    private UUID courseId;

    private UUID academicClassId;

    private List<UUID> studentIds;

    @Size(max = 20, message = "O mês de referência deve ter no máximo 20 caracteres.")
    private String referenceMonth;

    @NotNull(message = "A data de vencimento é obrigatória.")
    private LocalDate dueDate;

    private LocalDate referenceDate;

    @NotNull(message = "O valor base é obrigatório.")
    @DecimalMin(value = "0.01", message = "O valor base deve ser maior que zero.")
    private BigDecimal baseAmount;

    private BigDecimal discountAmount = BigDecimal.ZERO;

    private String currency = "AOA";

    private String serviceCode = "PROPINA";

    private String descriptionPrefix = "Propina";

    private Boolean onlyActiveStudents = true;

    public UUID getInstitutionId() {
        return institutionId;
    }

    public TuitionChargeGenerationRequest setInstitutionId(UUID institutionId) {
        this.institutionId = institutionId;
        return this;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public TuitionChargeGenerationRequest setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
        return this;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public TuitionChargeGenerationRequest setCourseId(UUID courseId) {
        this.courseId = courseId;
        return this;
    }

    public UUID getAcademicClassId() {
        return academicClassId;
    }

    public TuitionChargeGenerationRequest setAcademicClassId(UUID academicClassId) {
        this.academicClassId = academicClassId;
        return this;
    }

    public List<UUID> getStudentIds() {
        return studentIds;
    }

    public TuitionChargeGenerationRequest setStudentIds(List<UUID> studentIds) {
        this.studentIds = studentIds;
        return this;
    }

    public String getReferenceMonth() {
        return referenceMonth;
    }

    public TuitionChargeGenerationRequest setReferenceMonth(String referenceMonth) {
        this.referenceMonth = referenceMonth;
        return this;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public TuitionChargeGenerationRequest setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
        return this;
    }

    public LocalDate getReferenceDate() {
        return referenceDate;
    }

    public TuitionChargeGenerationRequest setReferenceDate(LocalDate referenceDate) {
        this.referenceDate = referenceDate;
        return this;
    }

    public BigDecimal getBaseAmount() {
        return baseAmount;
    }

    public TuitionChargeGenerationRequest setBaseAmount(BigDecimal baseAmount) {
        this.baseAmount = baseAmount;
        return this;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public TuitionChargeGenerationRequest setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public TuitionChargeGenerationRequest setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public TuitionChargeGenerationRequest setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
        return this;
    }

    public String getDescriptionPrefix() {
        return descriptionPrefix;
    }

    public TuitionChargeGenerationRequest setDescriptionPrefix(String descriptionPrefix) {
        this.descriptionPrefix = descriptionPrefix;
        return this;
    }

    public Boolean getOnlyActiveStudents() {
        return onlyActiveStudents;
    }

    public TuitionChargeGenerationRequest setOnlyActiveStudents(Boolean onlyActiveStudents) {
        this.onlyActiveStudents = onlyActiveStudents;
        return this;
    }
}
