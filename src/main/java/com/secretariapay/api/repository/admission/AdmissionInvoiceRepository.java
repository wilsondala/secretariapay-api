package com.secretariapay.api.repository.admission;

import com.secretariapay.api.entity.admission.AdmissionInvoice;
import com.secretariapay.api.entity.enums.admission.AdmissionInvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdmissionInvoiceRepository extends JpaRepository<AdmissionInvoice, UUID> {
    Optional<AdmissionInvoice> findByApplicationId(UUID applicationId);
    Optional<AdmissionInvoice> findByInvoiceCodeIgnoreCase(String invoiceCode);
    List<AdmissionInvoice> findByApplicationInstitutionIdAndStatusOrderByCreatedAtDesc(UUID institutionId, AdmissionInvoiceStatus status);
    List<AdmissionInvoice> findAllByStatusAndDueDateBefore(AdmissionInvoiceStatus status, LocalDate dueDate);
    boolean existsByInvoiceCode(String invoiceCode);
}
