
package com.secretariapay.api.entity.core;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity @Table(name="payment_method_configs")
public class PaymentMethodConfig { @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id; @Column(name="institution_id") private UUID institutionId; @Column(name="method_code",nullable=false,length=80) private String methodCode; @Column(name="method_name",nullable=false,length=140) private String methodName; @Column(columnDefinition="text") private String description; @Column(nullable=false) private Boolean active=true; @Column(columnDefinition="text") private String instructions; @Column(name="created_at",nullable=false) private LocalDateTime createdAt; @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt; @PrePersist public void prePersist(){LocalDateTime n=LocalDateTime.now();createdAt=n;updatedAt=n;if(active==null)active=true;} @PreUpdate public void preUpdate(){updatedAt=LocalDateTime.now();} public UUID getId(){return id;} public UUID getInstitutionId(){return institutionId;} public PaymentMethodConfig setInstitutionId(UUID v){institutionId=v;return this;} public String getMethodCode(){return methodCode;} public PaymentMethodConfig setMethodCode(String v){methodCode=v;return this;} public String getMethodName(){return methodName;} public PaymentMethodConfig setMethodName(String v){methodName=v;return this;} public String getDescription(){return description;} public PaymentMethodConfig setDescription(String v){description=v;return this;} public Boolean getActive(){return active;} public PaymentMethodConfig setActive(Boolean v){active=v;return this;} public String getInstructions(){return instructions;} public PaymentMethodConfig setInstructions(String v){instructions=v;return this;} public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;} }
