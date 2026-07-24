package com.secretariapay.api.service.admission;

import com.secretariapay.api.dto.admission.AdmissionDto;
import com.secretariapay.api.repository.admission.AdmissionEnrollmentDocumentReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmissionDigitalDocumentCompletionServiceTest {

    @Mock
    private AdmissionEnrollmentDocumentChecklistService checklistService;
    @Mock
    private AdmissionEnrollmentDocumentReviewRepository reviewRepository;

    @Test
    void shouldEvaluateRobotSubmissionUsingDigitalChecklistRules() {
        UUID applicationId = UUID.randomUUID();
        AdmissionDto.EnrollmentDocumentChecklistResponse expected =
                mock(AdmissionDto.EnrollmentDocumentChecklistResponse.class);
        when(reviewRepository.findByApplicationId(applicationId)).thenReturn(Optional.empty());
        when(checklistService.review(
                org.mockito.ArgumentMatchers.eq(applicationId),
                org.mockito.ArgumentMatchers.any(AdmissionDto.EnrollmentDocumentChecklistRequest.class)
        )).thenReturn(expected);

        AdmissionDto.EnrollmentDocumentChecklistResponse result = service()
                .evaluateRobotSubmission(applicationId, false, "Robô WhatsApp IMETRO");

        assertSame(expected, result);
        ArgumentCaptor<AdmissionDto.EnrollmentDocumentChecklistRequest> captor =
                ArgumentCaptor.forClass(AdmissionDto.EnrollmentDocumentChecklistRequest.class);
        verify(checklistService).review(
                org.mockito.ArgumentMatchers.eq(applicationId),
                captor.capture()
        );
        AdmissionDto.EnrollmentDocumentChecklistRequest request = captor.getValue();
        assertTrue(request.twoPassportPhotos());
        assertTrue(request.authenticatedCertificateCopy());
        assertTrue(request.identityDocumentCopy());
        assertTrue(request.secondaryEducationCompleted());
        assertFalse(request.studiedAbroad());
        assertFalse(request.originalsVerified());
    }

    private AdmissionDigitalDocumentCompletionService service() {
        return new AdmissionDigitalDocumentCompletionService(
                checklistService,
                reviewRepository
        );
    }
}
