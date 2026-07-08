package com.secretariapay.api.entity.financial;

import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "charges")
public class Charge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "charge_code", nullable = false, unique = true, length = 60)
    private String chargeCode;

    @Column(nullable = false, length = 120)
    private String description;

    @Column(name = "reference_month", length = 20)
    private String referenceMonth;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "fine_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal fineAmount = BigDecimal.ZERO;

    @Column(name = "interest_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal interestAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 10)
    private String currency = "AOA";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChargeStatus status = ChargeStatus.PENDING;

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
        normalizeDefaults();
        recalculateTotalAmount();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        normalizeDefaults();
        recalculateTotalAmount();
        updatedAt = LocalDateTime.now();
    }

    public void recalculateTotalAmount() {
        BigDecimal safeAmount = money(amount);
        BigDecimal safeFine = money(fineAmount);
        BigDecimal safeInterest = money(interestAmount);
        BigDecimal safeDiscount = money(discountAmount);
        totalAmount = money(safeAmount.add(safeFine).add(safeInterest).subtract(safeDiscount));
    }

    private void normalizeDefaults() {
        if (status == null) {
            status = ChargeStatus.PENDING;
        }

        if (currency == null || currency.isBlank()) {
            currency = "AOA";
        }

        amount = money(amount);
        fineAmount = money(fineAmount);
        interestAmount = money(interestAmount);
        discountAmount = money(discountAmount);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    public UUID getId() {
        return id;
    }

    public Student getStudent() {
        return student;
    }

    public Charge setStudent(Student student) {
        this.student = student;
        return this;
    }

    public String getChargeCode() {
        return chargeCode;
    }

    public Charge setChargeCode(String chargeCode) {
        this.chargeCode = chargeCode;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Charge setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getReferenceMonth() {
        return referenceMonth;
    }

    public Charge setReferenceMonth(String referenceMonth) {
        this.referenceMonth = referenceMonth;
        return this;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Charge setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
        return this;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Charge setAmount(BigDecimal amount) {
        this.amount = amount;
        recalculateTotalAmount();
        return this;
    }

    public BigDecimal getFineAmount() {
        return fineAmount;
    }

    public Charge setFineAmount(BigDecimal fineAmount) {
        this.fineAmount = fineAmount;
        recalculateTotalAmount();
        return this;
    }

    public BigDecimal getInterestAmount() {
        return interestAmount;
    }

    public Charge setInterestAmount(BigDecimal interestAmount) {
        this.interestAmount = interestAmount;
        recalculateTotalAmount();
        return this;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public Charge setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
        recalculateTotalAmount();
        return this;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Charge setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = money(totalAmount);
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public Charge setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public ChargeStatus getStatus() {
        return status;
    }

    public Charge setStatus(ChargeStatus status) {
        this.status = status;
        return this;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public Charge setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
        return this;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public Charge setCancelledAt(LocalDateTime cancelledAt) {
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
