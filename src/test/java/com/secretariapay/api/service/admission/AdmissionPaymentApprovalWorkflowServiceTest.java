package com.secretariapay.api.service.admission;

import com.secretariapay.api.dto.admission.AdmissionDto;
import com.secretariapay.api.entity.admission.AdmissionApplication;
import com.secretariapay.api.entity.enums.admission.AdmissionApplicationStatus;
import com.secretariapay.api.repository.admission.AdmissionApplicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmissionPaymentApprovalWorkflowServiceTest {

    @Mock
    private AdmissionService admissionService;

    @Mock
    private AdmissionApplicationRepository applicationRepository;

    @Mock
    private AdmissionOperationalNotificationService notificationService;

    @Test
    void shouldRequestEnrollmentDocumentsAfterDcrApprovesRegistrationPayment() {
        UUID proofId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        AdmissionDto.ReviewPaymentProofRequest request =
                mock(AdmissionDto.ReviewPaymentProofRequest.class);
        AdmissionDto.ApplicationResponse response =
                mock(AdmissionDto.ApplicationResponse.class);
        AdmissionApplication application = new AdmissionApplication()
                .setApplicationCode("IMT-ADM-20260723-MATRICULA")
                .setStatus(AdmissionApplicationStatus.DOCUMENTATION_PENDING);

        when(admissionService.approvePaymentProof(proofId, request)).thenReturn(response);
        when(response.id()).thenReturn(applicationId);
        when(response.status()).thenReturn(AdmissionApplicationStatus.DOCUMENTATION_PENDING);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        AdmissionDto.ApplicationResponse result = service().approvePaymentProof(proofId, request);

        assertSame(response, result);
        verify(notificationService).enqueueEnrollmentDocumentsRequested(application);
    }

    @Test
    void shouldNotRequestDocumentsWhenApplicationIsAlreadyConfirmed() {
        UUID proofId = UUID.randomUUID();
        AdmissionDto.ReviewPaymentProofRequest request =
                mock(AdmissionDto.ReviewPaymentProofRequest.class);
        AdmissionDto.ApplicationResponse response =
                mock(AdmissionDto.ApplicationResponse.class);

        when(admissionService.approvePaymentProof(proofId, request)).thenReturn(response);
        when(response.status()).thenReturn(AdmissionApplicationStatus.CONFIRMED);

        AdmissionDto.ApplicationResponse result = service().approvePaymentProof(proofId, request);

        assertSame(response, result);
        verify(applicationRepository, never()).findById(org.mockito.ArgumentMatchers.any());
        verify(notificationService, never())
                .enqueueEnrollmentDocumentsRequested(org.mockito.ArgumentMatchers.any());
    }

    private AdmissionPaymentApprovalWorkflowService service() {
        return new AdmissionPaymentApprovalWorkflowService(
                admissionService,
                applicationRepository,
                notificationService
        );
    }
}
