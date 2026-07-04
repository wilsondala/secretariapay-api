package com.secretariapay.api.repository.core;
import com.secretariapay.api.entity.core.FinancialNegotiation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface FinancialNegotiationRepository extends JpaRepository<FinancialNegotiation, UUID> { List<FinancialNegotiation> findByStatusOrderByCreatedAtDesc(String status); List<FinancialNegotiation> findByStudentIdOrderByCreatedAtDesc(UUID studentId); Optional<FinancialNegotiation> findByNegotiationCode(String negotiationCode); }
