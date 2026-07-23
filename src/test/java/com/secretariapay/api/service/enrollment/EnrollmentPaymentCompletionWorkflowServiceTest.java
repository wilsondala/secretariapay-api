package com.secretariapay.api.service.enrollment;

import com.secretariapay.api.dto.enrollment.EnrollmentDto;
import com.secretariapay.api.entity.admission.AdmissionApplication;
import com.secretariapay.api.entity.admission.AdmissionEnrollmentDocumentReview;
import com.secretariapay.api.entity.enums.enrollment.EnrollmentRequestStatus;
import com.secretariapay.api.repository.admission.AdmissionApplicationRepository;
import com.secretariapay.api.repository.admission.AdmissionEnrollmentDocumentReviewRepository;
import com.secretariapay.api.service.admission.AdmissionOperationalNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentPaymentCompletionWorkflowServiceTest {

    @Mock
    private EnrollmentService enrollmentService;
    @Mock
    private AdmissionApplicationRepository applicationRepository;
    @Mock
    private AdmissionEnrollmentDocumentReviewRepository reviewRepository;
    @Mock
    private AdmissionOperationalNotificationService notificationService;

    @Test
    void shouldNotifyCandidateAfterEnrollmentPaymentCreatesStudent() {
        UUID proofId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        EnrollmentDto.ReviewPaymentRequest request = mock(EnrollmentDto.ReviewPaymentRequest.class);
        EnrollmentDto.EnrollmentResponse response = mock(EnrollmentDto.EnrollmentResponse.class);
        AdmissionApplication application = new AdmissionApplication()
                .setApplicationCode("IMT-ADM-20260723-MATRICULA")
                .setWhatsapp("+244 923 200 777");
        AdmissionEnrollmentDocumentReview review = new AdmissionEnrollmentDocumentReview()
                .setOriginalsDueDate(LocalDate.of(2026, 9, 8));

        when(enrollmentService.approvePaymentProof(proofId, request)).thenReturn(response);
        when(response.status()).thenReturn(EnrollmentRequestStatus.COMPLETED);
        when(response.admissionApplicationId()).thenReturn(applicationId);
        when(response.studentNumber()).thenReturn("202601234");
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(reviewRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(review));

        EnrollmentDto.EnrollmentResponse result = service().approvePaymentProof(proofId, request);

        assertSame(response, result);
        verify(notificationService).enqueueEnrollmentCompletedOriginalsPending(
                application,
                "202601234",
                LocalDate.of(2026, 9, 8)
        );
    }

    @Test
    void shouldNotNotifyBeforeEnrollmentIsCompleted() {
        UUID invoiceId = UUID.randomUUID();
        EnrollmentDto.ReviewPaymentRequest request = mock(EnrollmentDto.ReviewPaymentRequest.class);
        EnrollmentDto.EnrollmentResponse response = mock(EnrollmentDto.EnrollmentResponse.class);

        when(enrollmentService.confirmPayment(invoiceId, request)).thenReturn(response);
        when(response.status()).thenReturn(EnrollmentRequestStatus.AWAITING_PAYMENT);

        EnrollmentDto.EnrollmentResponse result = service().confirmPayment(invoiceId, request);

        assertSame(response, result);
        verify(notificationService, never()).enqueueEnrollmentCompletedOriginalsPending(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    private EnrollmentPaymentCompletionWorkflowService service() {
        return new EnrollmentPaymentCompletionWorkflowService(
                enrollmentService,
                applicationRepository,
                reviewRepository,
                notificationService
        );
    }
}
