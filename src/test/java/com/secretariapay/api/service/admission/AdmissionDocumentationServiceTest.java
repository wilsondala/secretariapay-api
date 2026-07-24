package com.secretariapay.api.service.admission;

import com.secretariapay.api.dto.admission.AdmissionDto;
import com.secretariapay.api.entity.admission.AdmissionApplication;
import com.secretariapay.api.entity.admission.AdmissionInvoice;
import com.secretariapay.api.entity.enums.admission.AdmissionApplicationStatus;
import com.secretariapay.api.entity.enums.admission.AdmissionInvoiceStatus;
import com.secretariapay.api.repository.admission.AdmissionApplicationRepository;
import com.secretariapay.api.repository.admission.AdmissionInvoiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmissionDocumentationServiceTest {

    @Mock
    private AdmissionApplicationRepository applicationRepository;

    @Mock
    private AdmissionInvoiceRepository invoiceRepository;

    @Mock
    private AdmissionService admissionService;

    @Test
    void shouldConfirmApplicationWhenDocumentsAndPaymentAreComplete() {
        UUID applicationId = UUID.randomUUID();
        AdmissionApplication application = new AdmissionApplication()
                .setDocumentsComplete(false)
                .setStatus(AdmissionApplicationStatus.DOCUMENTATION_PENDING);
        AdmissionInvoice invoice = new AdmissionInvoice()
                .setStatus(AdmissionInvoiceStatus.PAID);

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(invoiceRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(invoice));
        when(applicationRepository.save(any(AdmissionApplication.class))).thenReturn(application);

        AdmissionDocumentationService service = new AdmissionDocumentationService(
                applicationRepository,
                invoiceRepository,
                admissionService
        );

        service.reviewDocuments(
                applicationId,
                new AdmissionDto.ApplicationDocumentsRequest(
                        true,
                        "Secretaria - Teste",
                        "Documentação conferida"
                )
        );

        assertTrue(application.getDocumentsComplete());
        assertEquals(AdmissionApplicationStatus.CONFIRMED, application.getStatus());
        assertNotNull(application.getConfirmedAt());
        assertTrue(application.getNotes().contains("Documentação - COMPLETA"));
        verify(applicationRepository).save(application);
        verify(admissionService).getApplication(applicationId);
    }

    @Test
    void shouldKeepPaidApplicationPendingWhenDocumentsAreIncomplete() {
        UUID applicationId = UUID.randomUUID();
        AdmissionApplication application = new AdmissionApplication()
                .setDocumentsComplete(true)
                .setStatus(AdmissionApplicationStatus.CONFIRMED)
                .setConfirmedAt(java.time.LocalDateTime.now());
        AdmissionInvoice invoice = new AdmissionInvoice()
                .setStatus(AdmissionInvoiceStatus.PAID);

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(invoiceRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(invoice));
        when(applicationRepository.save(any(AdmissionApplication.class))).thenReturn(application);

        AdmissionDocumentationService service = new AdmissionDocumentationService(
                applicationRepository,
                invoiceRepository,
                admissionService
        );

        service.reviewDocuments(
                applicationId,
                new AdmissionDto.ApplicationDocumentsRequest(
                        false,
                        "Secretaria - Teste",
                        "Certificado autenticado pendente"
                )
        );

        assertFalse(application.getDocumentsComplete());
        assertEquals(AdmissionApplicationStatus.DOCUMENTATION_PENDING, application.getStatus());
        assertNull(application.getConfirmedAt());
        assertTrue(application.getNotes().contains("Documentação - PENDENTE"));
        verify(applicationRepository).save(application);
    }
}
