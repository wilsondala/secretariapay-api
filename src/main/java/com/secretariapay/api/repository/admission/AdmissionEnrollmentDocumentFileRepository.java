package com.secretariapay.api.repository.admission;

import com.secretariapay.api.entity.admission.AdmissionEnrollmentDocumentFile;
import com.secretariapay.api.entity.enums.admission.AdmissionEnrollmentDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdmissionEnrollmentDocumentFileRepository
        extends JpaRepository<AdmissionEnrollmentDocumentFile, UUID> {

    List<AdmissionEnrollmentDocumentFile> findByApplicationIdOrderByUploadedAtAsc(UUID applicationId);

    List<AdmissionEnrollmentDocumentFile> findByApplicationIdAndDocumentTypeOrderByUploadedAtAsc(
            UUID applicationId,
            AdmissionEnrollmentDocumentType documentType
    );

    long countByApplicationIdAndDocumentType(
            UUID applicationId,
            AdmissionEnrollmentDocumentType documentType
    );
}
