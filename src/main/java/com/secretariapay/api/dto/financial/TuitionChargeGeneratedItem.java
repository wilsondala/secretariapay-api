package com.secretariapay.api.dto.financial;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class TuitionChargeGeneratedItem {

    private UUID studentId;
    private String studentNumber;
    private String studentName;
    private UUID chargeId;
    private String chargeCode;
    private String courseName;
    private String academicClassName;
    private String referenceMonth;
    private LocalDate dueDate;
    private BigDecimal baseAmount;
    private BigDecimal fineAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String currency;
    private String status;
    private String action;

    public UUID getStudentId() { return studentId; }
    public TuitionChargeGeneratedItem setStudentId(UUID studentId) { this.studentId = studentId; return this; }
    public String getStudentNumber() { return studentNumber; }
    public TuitionChargeGeneratedItem setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; return this; }
    public String getStudentName() { return studentName; }
    public TuitionChargeGeneratedItem setStudentName(String studentName) { this.studentName = studentName; return this; }
    public UUID getChargeId() { return chargeId; }
    public TuitionChargeGeneratedItem setChargeId(UUID chargeId) { this.chargeId = chargeId; return this; }
    public String getChargeCode() { return chargeCode; }
    public TuitionChargeGeneratedItem setChargeCode(String chargeCode) { this.chargeCode = chargeCode; return this; }
    public String getCourseName() { return courseName; }
    public TuitionChargeGeneratedItem setCourseName(String courseName) { this.courseName = courseName; return this; }
    public String getAcademicClassName() { return academicClassName; }
    public TuitionChargeGeneratedItem setAcademicClassName(String academicClassName) { this.academicClassName = academicClassName; return this; }
    public String getReferenceMonth() { return referenceMonth; }
    public TuitionChargeGeneratedItem setReferenceMonth(String referenceMonth) { this.referenceMonth = referenceMonth; return this; }
    public LocalDate getDueDate() { return dueDate; }
    public TuitionChargeGeneratedItem setDueDate(LocalDate dueDate) { this.dueDate = dueDate; return this; }
    public BigDecimal getBaseAmount() { return baseAmount; }
    public TuitionChargeGeneratedItem setBaseAmount(BigDecimal baseAmount) { this.baseAmount = baseAmount; return this; }
    public BigDecimal getFineAmount() { return fineAmount; }
    public TuitionChargeGeneratedItem setFineAmount(BigDecimal fineAmount) { this.fineAmount = fineAmount; return this; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public TuitionChargeGeneratedItem setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; return this; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public TuitionChargeGeneratedItem setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; return this; }
    public String getCurrency() { return currency; }
    public TuitionChargeGeneratedItem setCurrency(String currency) { this.currency = currency; return this; }
    public String getStatus() { return status; }
    public TuitionChargeGeneratedItem setStatus(String status) { this.status = status; return this; }
    public String getAction() { return action; }
    public TuitionChargeGeneratedItem setAction(String action) { this.action = action; return this; }
}
