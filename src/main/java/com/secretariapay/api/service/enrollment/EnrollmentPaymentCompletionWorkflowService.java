package com.secretariapay.api.service.enrollment;

import com.secretariapay.api.dto.enrollment.EnrollmentDto;
import com.secretariapay.api.entity.admission.AdmissionApplication;
import com.secretariapay.api.entity.admission.AdmissionEnrollmentDocumentReview;
import com.secretariapay.api.entity.enums.enrollment.EnrollmentRequestStatus;
import com.secretariapay.api.repository.admission.AdmissionApplicationRepository;
import com.secretariapay.api.repository.admission.AdmissionEnrollmentDocumentReviewRepository;
import com.secretariapay.api.service.admission.AdmissionOperationalNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class EnrollmentPaymentCompletionWorkflowService {

    private final EnrollmentService enrollmentService;
    private final AdmissionApplicationRepository applicationRepository;
    private final AdmissionEnrollmentDocumentReviewRepository reviewRepository;
    private final AdmissionOperationalNotificationService notificationService;

    public EnrollmentPaymentCompletionWorkflowService(
            EnrollmentService enrollmentService,
            AdmissionApplicationRepository applicationRepository,
            AdmissionEnrollmentDocumentReviewRepository reviewRepository,
            AdmissionOperationalNotificationService notificationService
    ) {
        this.enrollmentService = enrollmentService;
        this.applicationRepository = applicationRepository;
        this.reviewRepository = reviewRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public EnrollmentDto.EnrollmentResponse approvePaymentProof(
            UUID proofId,
            EnrollmentDto.ReviewPaymentRequest request
    ) {
        EnrollmentDto.EnrollmentResponse response = enrollmentService.approvePaymentProof(proofId, request);
        notifyCompletedEnrollment(response);
        return response;
    }

    @Transactional
    public EnrollmentDto.EnrollmentResponse confirmPayment(
            UUID invoiceId,
            EnrollmentDto.ReviewPaymentRequest request
    ) {
        EnrollmentDto.EnrollmentResponse response = enrollmentService.confirmPayment(invoiceId, request);
        notifyCompletedEnrollment(response);
        return response;
    }

    private void notifyCompletedEnrollment(EnrollmentDto.EnrollmentResponse response) {
        if (response == null
                || response.status() != EnrollmentRequestStatus.COMPLETED
                || response.admissionApplicationId() == null
                || response.studentNumber() == null) {
            return;
        }

        UUID applicationId = response.admissionApplicationId();
        AdmissionApplication application = applicationRepository
                .findById(applicationId)
                .orElse(null);
        if (application == null) return;

        LocalDate originalsDueDate = reviewRepository
                .findByApplicationId(applicationId)
                .map(AdmissionEnrollmentDocumentReview::getOriginalsDueDate)
                .orElse(application.getCampaign() == null
                        ? null
                        : application.getCampaign().getRegistrationEnd());

        notificationService.enqueueEnrollmentCompletedOriginalsPending(
                application,
                response.studentNumber(),
                originalsDueDate
        );
    }
}
