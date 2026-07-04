package com.secretariapay.api.dto.config;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DcrChargeEvaluationRequest {

    private BigDecimal baseAmount;
    private LocalDate dueDate;
    private LocalDate referenceDate;

    public BigDecimal getBaseAmount() { return baseAmount; }
    public DcrChargeEvaluationRequest setBaseAmount(BigDecimal baseAmount) { this.baseAmount = baseAmount; return this; }
    public LocalDate getDueDate() { return dueDate; }
    public DcrChargeEvaluationRequest setDueDate(LocalDate dueDate) { this.dueDate = dueDate; return this; }
    public LocalDate getReferenceDate() { return referenceDate; }
    public DcrChargeEvaluationRequest setReferenceDate(LocalDate referenceDate) { this.referenceDate = referenceDate; return this; }
}
