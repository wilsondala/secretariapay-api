package com.secretariapay.api.service.admission;

import com.secretariapay.api.dto.admission.AdmissionDto;
import com.secretariapay.api.entity.admission.AdmissionApplication;
import com.secretariapay.api.entity.enums.admission.AdmissionApplicationStatus;
import com.secretariapay.api.entity.enums.admission.AdmissionSourceChannel;
import com.secretariapay.api.repository.admission.AdmissionApplicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmissionPublicApplicationWorkflowServiceTest {

    @Mock
    private AdmissionService admissionService;

    @Mock
    private AdmissionApplicationRepository applicationRepository;

    @Mock
    private AdmissionOperationalNotificationService notificationService;

    @Test
    void shouldQueueOperationalAlertAfterSubmittedPublicApplication() {
        AdmissionDto.ApplicationRequest request = mock(AdmissionDto.ApplicationRequest.class);
        AdmissionDto.ApplicationResponse response = mock(AdmissionDto.ApplicationResponse.class);
        AdmissionApplication application = new AdmissionApplication()
                .setApplicationCode("IMT-ADM-20260723-ALERTA")
                .setStatus(AdmissionApplicationStatus.SUBMITTED);
        UUID applicationId = UUID.randomUUID();

        when(admissionService.createApplication(request, AdmissionSourceChannel.FORM))
                .thenReturn(response);
        when(response.id()).thenReturn(applicationId);
        when(response.status()).thenReturn(AdmissionApplicationStatus.SUBMITTED);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        AdmissionPublicApplicationWorkflowService service =
                new AdmissionPublicApplicationWorkflowService(
                        admissionService,
                        applicationRepository,
                        notificationService
                );

        AdmissionDto.ApplicationResponse result = service.createApplication(request);

        assertSame(response, result);
        verify(notificationService).enqueueApplicationSubmitted(application);
    }
}
