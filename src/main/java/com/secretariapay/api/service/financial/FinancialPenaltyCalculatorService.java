package com.secretariapay.api.service.financial;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

@Service
public class FinancialPenaltyCalculatorService {

    private final int dueDay;
    private final int firstPenaltyStartDay;
    private final BigDecimal firstPenaltyPercent;
    private final int secondPenaltyStartDay;
    private final BigDecimal secondPenaltyPercent;
    private final BigDecimal dailyInterestPercent;
    private final String referenceDateOverride;

    public FinancialPenaltyCalculatorService(
            @Value("${SECRETARIAPAY_FINANCIAL_DUE_DAY:10}") int dueDay,
            @Value("${SECRETARIAPAY_FINANCIAL_FIRST_PENALTY_START_DAY:11}") int firstPenaltyStartDay,
            @Value("${SECRETARIAPAY_FINANCIAL_FIRST_PENALTY_PERCENT:20}") BigDecimal firstPenaltyPercent,
            @Value("${SECRETARIAPAY_FINANCIAL_SECOND_PENALTY_START_DAY:15}") int secondPenaltyStartDay,
            @Value("${SECRETARIAPAY_FINANCIAL_SECOND_PENALTY_PERCENT:30}") BigDecimal secondPenaltyPercent,
            @Value("${SECRETARIAPAY_FINANCIAL_DAILY_INTEREST_PERCENT:0.10}") BigDecimal dailyInterestPercent,
            @Value("${SECRETARIAPAY_FINANCIAL_REFERENCE_DATE:}") String referenceDateOverride
    ) {
        this.dueDay = dueDay;
        this.firstPenaltyStartDay = firstPenaltyStartDay;
        this.firstPenaltyPercent = firstPenaltyPercent == null ? BigDecimal.ZERO : firstPenaltyPercent;
        this.secondPenaltyStartDay = secondPenaltyStartDay;
        this.secondPenaltyPercent = secondPenaltyPercent == null ? BigDecimal.ZERO : secondPenaltyPercent;
        this.dailyInterestPercent = dailyInterestPercent == null ? BigDecimal.ZERO : dailyInterestPercent;
        this.referenceDateOverride = referenceDateOverride == null ? "" : referenceDateOverride.trim();
    }

    public FinancialChargeCalculation calculate(String referenceMonth, BigDecimal baseAmount, YearMonth yearMonth) {
        return calculate(referenceMonth, baseAmount, yearMonth, resolveReferenceDate());
    }

    public FinancialChargeCalculation calculate(String referenceMonth, BigDecimal baseAmount, YearMonth yearMonth, LocalDate referenceDate) {
        BigDecimal safeBaseAmount = money(baseAmount == null ? BigDecimal.ZERO : baseAmount);
        LocalDate dueDate = yearMonth.atDay(Math.min(Math.max(dueDay, 1), yearMonth.lengthOfMonth()));
        LocalDate safeReferenceDate = referenceDate == null ? LocalDate.now() : referenceDate;

        long daysLate = Math.max(0, ChronoUnit.DAYS.between(dueDate, safeReferenceDate));
        BigDecimal finePercent = resolveFinePercent(dueDate, safeReferenceDate, daysLate);
        BigDecimal fineAmount = percentage(safeBaseAmount, finePercent);
        BigDecimal interestAmount = daysLate <= 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : money(safeBaseAmount
                .multiply(dailyInterestPercent)
                .divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(daysLate)));
        BigDecimal discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = money(safeBaseAmount.add(fineAmount).add(interestAmount).subtract(discountAmount));

        return new FinancialChargeCalculation(
                referenceMonth,
                dueDate,
                daysLate,
                safeBaseAmount,
                money(finePercent),
                fineAmount,
                dailyInterestPercent,
                interestAmount,
                discountAmount,
                totalAmount
        );
    }

    public BigDecimal total(FinancialChargeCalculation... calculations) {
        BigDecimal total = BigDecimal.ZERO;
        if (calculations != null) {
            for (FinancialChargeCalculation calculation : calculations) {
                if (calculation != null) {
                    total = total.add(calculation.getTotalAmount());
                }
            }
        }
        return money(total);
    }

    private BigDecimal resolveFinePercent(LocalDate dueDate, LocalDate referenceDate, long daysLate) {
        if (daysLate <= 0) {
            return BigDecimal.ZERO;
        }

        LocalDate firstPenaltyDate = dueDate.withDayOfMonth(Math.min(firstPenaltyStartDay, dueDate.lengthOfMonth()));
        LocalDate secondPenaltyDate = dueDate.withDayOfMonth(Math.min(secondPenaltyStartDay, dueDate.lengthOfMonth()));

        if (!referenceDate.isBefore(secondPenaltyDate)) {
            return secondPenaltyPercent;
        }

        if (!referenceDate.isBefore(firstPenaltyDate)) {
            return firstPenaltyPercent;
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal percentage(BigDecimal amount, BigDecimal percent) {
        if (percent == null || percent.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return money(amount.multiply(percent).divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP));
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private LocalDate resolveReferenceDate() {
        if (!referenceDateOverride.isBlank()) {
            try {
                return LocalDate.parse(referenceDateOverride);
            } catch (Exception ignored) {
                return LocalDate.now();
            }
        }
        return LocalDate.now();
    }
}
