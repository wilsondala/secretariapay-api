package com.secretariapay.api.repository.admission;

import com.secretariapay.api.entity.admission.AdmissionLead;
import com.secretariapay.api.entity.enums.admission.AdmissionLeadStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdmissionLeadRepository extends JpaRepository<AdmissionLead, UUID> {
    List<AdmissionLead> findByInstitutionIdOrderByCreatedAtDesc(UUID institutionId);
    List<AdmissionLead> findByInstitutionIdAndStatusOrderByCreatedAtDesc(UUID institutionId, AdmissionLeadStatus status);
    Optional<AdmissionLead> findFirstByInstitutionIdAndWhatsappIgnoreCase(UUID institutionId, String whatsapp);
    Optional<AdmissionLead> findFirstByInstitutionIdAndDocumentNumberIgnoreCase(UUID institutionId, String documentNumber);
    Optional<AdmissionLead> findFirstByInstitutionIdAndEmailIgnoreCase(UUID institutionId, String email);
}
