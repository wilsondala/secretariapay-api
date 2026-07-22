package com.secretariapay.api.repository.admission;

import com.secretariapay.api.entity.admission.AdmissionPaymentProof;
import com.secretariapay.api.entity.enums.admission.AdmissionPaymentProofStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdmissionPaymentProofRepository extends JpaRepository<AdmissionPaymentProof, UUID> {
    List<AdmissionPaymentProof> findByInvoiceApplicationInstitutionIdOrderByCreatedAtDesc(UUID institutionId);
    List<AdmissionPaymentProof> findByInvoiceApplicationInstitutionIdAndStatusOrderByCreatedAtDesc(UUID institutionId, AdmissionPaymentProofStatus status);
    Optional<AdmissionPaymentProof> findFirstByInvoiceIdOrderByCreatedAtDesc(UUID invoiceId);
}
