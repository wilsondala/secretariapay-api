package com.secretariapay.api.repository.admission;

import com.secretariapay.api.entity.admission.AdmissionEnrollmentDocumentReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdmissionEnrollmentDocumentReviewRepository
        extends JpaRepository<AdmissionEnrollmentDocumentReview, UUID> {

    Optional<AdmissionEnrollmentDocumentReview> findByApplicationId(UUID applicationId);

    List<AdmissionEnrollmentDocumentReview>
    findByOriginalsVerifiedFalseAndOriginalsDueDateBeforeAndOriginalsBlockActiveFalse(
            LocalDate originalsDueDate
    );
}
