
package com.secretariapay.api.entity.core;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity @Table(name="financial_negotiation_installments")
public class FinancialNegotiationInstallment {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="negotiation_id",nullable=false) private FinancialNegotiation negotiation;
 @Column(name="installment_number",nullable=false) private Integer installmentNumber; @Column(nullable=false,precision=14,scale=2) private BigDecimal amount; @Column(nullable=false,length=10) private String currency="AOA"; @Column(name="due_date") private LocalDate dueDate; @Column(nullable=false,length=30) private String status="PENDING"; @Column(name="charge_id") private UUID chargeId; @Column(name="created_at",nullable=false) private LocalDateTime createdAt; @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
 @PrePersist public void prePersist(){LocalDateTime n=LocalDateTime.now();createdAt=n;updatedAt=n;if(status==null)status="PENDING";if(currency==null)currency="AOA";} @PreUpdate public void preUpdate(){updatedAt=LocalDateTime.now();}
 public UUID getId(){return id;} public FinancialNegotiation getNegotiation(){return negotiation;} public FinancialNegotiationInstallment setNegotiation(FinancialNegotiation v){negotiation=v;return this;} public Integer getInstallmentNumber(){return installmentNumber;} public FinancialNegotiationInstallment setInstallmentNumber(Integer v){installmentNumber=v;return this;} public BigDecimal getAmount(){return amount;} public FinancialNegotiationInstallment setAmount(BigDecimal v){amount=v;return this;} public String getCurrency(){return currency;} public FinancialNegotiationInstallment setCurrency(String v){currency=v;return this;} public LocalDate getDueDate(){return dueDate;} public FinancialNegotiationInstallment setDueDate(LocalDate v){dueDate=v;return this;} public String getStatus(){return status;} public FinancialNegotiationInstallment setStatus(String v){status=v;return this;} public UUID getChargeId(){return chargeId;} public FinancialNegotiationInstallment setChargeId(UUID v){chargeId=v;return this;} public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
