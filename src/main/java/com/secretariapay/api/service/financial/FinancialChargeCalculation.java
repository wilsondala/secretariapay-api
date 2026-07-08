package com.secretariapay.api.service.financial;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FinancialChargeCalculation {

    private final String referenceMonth;
    private final LocalDate dueDate;
    private final long daysLate;
    private final BigDecimal baseAmount;
    private final BigDecimal finePercent;
    private final BigDecimal fineAmount;
    private final BigDecimal dailyInterestPercent;
    private final BigDecimal interestAmount;
    private final BigDecimal discountAmount;
    private final BigDecimal totalAmount;

    public FinancialChargeCalculation(
            String referenceMonth,
            LocalDate dueDate,
            long daysLate,
            BigDecimal baseAmount,
            BigDecimal finePercent,
            BigDecimal fineAmount,
            BigDecimal dailyInterestPercent,
            BigDecimal interestAmount,
            BigDecimal discountAmount,
            BigDecimal totalAmount
    ) {
        this.referenceMonth = referenceMonth;
        this.dueDate = dueDate;
        this.daysLate = daysLate;
        this.baseAmount = baseAmount;
        this.finePercent = finePercent;
        this.fineAmount = fineAmount;
        this.dailyInterestPercent = dailyInterestPercent;
        this.interestAmount = interestAmount;
        this.discountAmount = discountAmount;
        this.totalAmount = totalAmount;
    }

    public String getReferenceMonth() {
        return referenceMonth;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public long getDaysLate() {
        return daysLate;
    }

    public boolean isOverdue() {
        return daysLate > 0;
    }

    public BigDecimal getBaseAmount() {
        return baseAmount;
    }

    public BigDecimal getFinePercent() {
        return finePercent;
    }

    public BigDecimal getFineAmount() {
        return fineAmount;
    }

    public BigDecimal getDailyInterestPercent() {
        return dailyInterestPercent;
    }

    public BigDecimal getInterestAmount() {
        return interestAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}
