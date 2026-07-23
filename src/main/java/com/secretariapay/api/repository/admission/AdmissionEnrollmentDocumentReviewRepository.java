package com.secretariapay.api.repository.admission;

import com.secretariapay.api.entity.admission.AdmissionEnrollmentDocumentReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdmissionEnrollmentDocumentReviewRepository
        extends JpaRepository<AdmissionEnrollmentDocumentReview, UUID> {

    Optional<AdmissionEnrollmentDocumentReview> findByApplicationId(UUID applicationId);
}
