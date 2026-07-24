package com.secretariapay.api.service.admission;

import com.secretariapay.api.dto.admission.AdmissionDto;
import com.secretariapay.api.entity.admission.AdmissionEnrollmentDocumentReview;
import com.secretariapay.api.repository.admission.AdmissionEnrollmentDocumentReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdmissionDigitalDocumentCompletionService {

    private final AdmissionEnrollmentDocumentChecklistService checklistService;
    private final AdmissionEnrollmentDocumentReviewRepository reviewRepository;

    public AdmissionDigitalDocumentCompletionService(
            AdmissionEnrollmentDocumentChecklistService checklistService,
            AdmissionEnrollmentDocumentReviewRepository reviewRepository
    ) {
        this.checklistService = checklistService;
        this.reviewRepository = reviewRepository;
    }

    /**
     * Integration contract for the WhatsApp robot. Call after each candidate upload.
     * The checklist service remains the source of truth and only creates the charge
     * when all required physical files are present and the candidate is eligible.
     */
    @Transactional
    public AdmissionDto.EnrollmentDocumentChecklistResponse evaluateRobotSubmission(
            UUID applicationId,
            boolean studiedAbroad,
            String submittedBy
    ) {
        AdmissionEnrollmentDocumentReview existing = reviewRepository
                .findByApplicationId(applicationId)
                .orElse(null);

        String actor = clean(submittedBy, "Robô SecretáriaPay");
        return checklistService.review(
                applicationId,
                new AdmissionDto.EnrollmentDocumentChecklistRequest(
                        true,
                        true,
                        true,
                        studiedAbroad,
                        studiedAbroad,
                        true,
                        existing != null && Boolean.TRUE.equals(existing.getOriginalsPresented()),
                        existing != null && Boolean.TRUE.equals(existing.getOriginalsVerified()),
                        actor,
                        "Documentos digitais recebidos pelo robô SecretáriaPay e encaminhados para a matrícula.",
                        existing == null ? null : existing.getOriginalsVerificationNotes()
                )
        );
    }

    private String clean(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.trim();
    }
}
