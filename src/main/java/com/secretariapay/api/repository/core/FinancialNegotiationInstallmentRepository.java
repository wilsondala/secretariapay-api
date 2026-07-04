package com.secretariapay.api.repository.core;
import com.secretariapay.api.entity.core.FinancialNegotiationInstallment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface FinancialNegotiationInstallmentRepository extends JpaRepository<FinancialNegotiationInstallment, UUID> { List<FinancialNegotiationInstallment> findByNegotiationIdOrderByInstallmentNumberAsc(UUID negotiationId); }
