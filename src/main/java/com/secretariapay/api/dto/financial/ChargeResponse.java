package com.secretariapay.api.dto.financial;

import com.secretariapay.api.entity.enums.financial.ChargeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class ChargeResponse {

    private UUID id;
    private String chargeCode;
    private UUID studentId;
    private String studentNumber;
    private String studentName;
    private UUID academicClassId;
    private String academicClassName;
    private UUID courseId;
    private String courseName;
    private String description;
    private String referenceMonth;
    private LocalDate dueDate;
    private BigDecimal amount;
    private BigDecimal fineAmount;
    private BigDecimal interestAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String currency;
    private ChargeStatus status;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public ChargeResponse setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getChargeCode() {
        return chargeCode;
    }

    public ChargeResponse setChargeCode(String chargeCode) {
        this.chargeCode = chargeCode;
        return this;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public ChargeResponse setStudentId(UUID studentId) {
        this.studentId = studentId;
        return this;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public ChargeResponse setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
        return this;
    }

    public String getStudentName() {
        return studentName;
    }

    public ChargeResponse setStudentName(String studentName) {
        this.studentName = studentName;
        return this;
    }

    public UUID getAcademicClassId() {
        return academicClassId;
    }

    public ChargeResponse setAcademicClassId(UUID academicClassId) {
        this.academicClassId = academicClassId;
        return this;
    }

    public String getAcademicClassName() {
        return academicClassName;
    }

    public ChargeResponse setAcademicClassName(String academicClassName) {
        this.academicClassName = academicClassName;
        return this;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public ChargeResponse setCourseId(UUID courseId) {
        this.courseId = courseId;
        return this;
    }

    public String getCourseName() {
        return courseName;
    }

    public ChargeResponse setCourseName(String courseName) {
        this.courseName = courseName;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public ChargeResponse setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getReferenceMonth() {
        return referenceMonth;
    }

    public ChargeResponse setReferenceMonth(String referenceMonth) {
        this.referenceMonth = referenceMonth;
        return this;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public ChargeResponse setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
        return this;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public ChargeResponse setAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public BigDecimal getFineAmount() {
        return fineAmount;
    }

    public ChargeResponse setFineAmount(BigDecimal fineAmount) {
        this.fineAmount = fineAmount;
        return this;
    }

    public BigDecimal getInterestAmount() {
        return interestAmount;
    }

    public ChargeResponse setInterestAmount(BigDecimal interestAmount) {
        this.interestAmount = interestAmount;
        return this;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public ChargeResponse setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
        return this;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public ChargeResponse setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public ChargeResponse setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public ChargeStatus getStatus() {
        return status;
    }

    public ChargeResponse setStatus(ChargeStatus status) {
        this.status = status;
        return this;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public ChargeResponse setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
        return this;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public ChargeResponse setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public ChargeResponse setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public ChargeResponse setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}
