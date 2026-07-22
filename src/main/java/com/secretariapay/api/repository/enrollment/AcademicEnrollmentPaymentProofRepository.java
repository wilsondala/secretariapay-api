package com.secretariapay.api.repository.enrollment;

import com.secretariapay.api.entity.enrollment.AcademicEnrollmentPaymentProof;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AcademicEnrollmentPaymentProofRepository extends JpaRepository<AcademicEnrollmentPaymentProof, UUID> {

    Optional<AcademicEnrollmentPaymentProof> findFirstByInvoiceIdOrderByCreatedAtDesc(UUID invoiceId);
}
