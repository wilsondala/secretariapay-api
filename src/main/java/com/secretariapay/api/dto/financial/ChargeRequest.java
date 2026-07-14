package com.secretariapay.api.dto.financial;

import com.secretariapay.api.entity.enums.financial.ChargeCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class ChargeRequest {

    @NotNull(message = "O estudante é obrigatório.")
    private UUID studentId;

    @NotBlank(message = "A descrição da cobrança é obrigatória.")
    @Size(max = 120, message = "A descrição deve ter no máximo 120 caracteres.")
    private String description;

    @Size(max = 20, message = "O mês de referência deve ter no máximo 20 caracteres.")
    private String referenceMonth;

    private ChargeCategory chargeCategory;

    @Size(max = 80, message = "O código do serviço deve ter no máximo 80 caracteres.")
    private String serviceCode;

    @NotNull(message = "A data de vencimento é obrigatória.")
    private LocalDate dueDate;

    @NotNull(message = "O valor é obrigatório.")
    @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
    private BigDecimal amount;

    private BigDecimal fineAmount = BigDecimal.ZERO;
    private BigDecimal interestAmount = BigDecimal.ZERO;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private String currency = "AOA";

    public UUID getStudentId() { return studentId; }
    public ChargeRequest setStudentId(UUID studentId) { this.studentId = studentId; return this; }
    public String getDescription() { return description; }
    public ChargeRequest setDescription(String description) { this.description = description; return this; }
    public String getReferenceMonth() { return referenceMonth; }
    public ChargeRequest setReferenceMonth(String referenceMonth) { this.referenceMonth = referenceMonth; return this; }
    public ChargeCategory getChargeCategory() { return chargeCategory; }
    public ChargeRequest setChargeCategory(ChargeCategory chargeCategory) { this.chargeCategory = chargeCategory; return this; }
    public String getServiceCode() { return serviceCode; }
    public ChargeRequest setServiceCode(String serviceCode) { this.serviceCode = serviceCode; return this; }
    public LocalDate getDueDate() { return dueDate; }
    public ChargeRequest setDueDate(LocalDate dueDate) { this.dueDate = dueDate; return this; }
    public BigDecimal getAmount() { return amount; }
    public ChargeRequest setAmount(BigDecimal amount) { this.amount = amount; return this; }
    public BigDecimal getFineAmount() { return fineAmount; }
    public ChargeRequest setFineAmount(BigDecimal fineAmount) { this.fineAmount = fineAmount; return this; }
    public BigDecimal getInterestAmount() { return interestAmount; }
    public ChargeRequest setInterestAmount(BigDecimal interestAmount) { this.interestAmount = interestAmount; return this; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public ChargeRequest setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; return this; }
    public String getCurrency() { return currency; }
    public ChargeRequest setCurrency(String currency) { this.currency = currency; return this; }
}
