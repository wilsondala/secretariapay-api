package com.secretariapay.api.service.admission;

import com.secretariapay.api.entity.admission.AdmissionApplication;
import com.secretariapay.api.entity.admission.AdmissionEnrollmentDocumentFile;
import com.secretariapay.api.entity.admission.AdmissionInvoice;
import com.secretariapay.api.entity.enums.admission.AdmissionApplicationStatus;
import com.secretariapay.api.entity.enums.admission.AdmissionEnrollmentDocumentType;
import com.secretariapay.api.entity.enums.admission.AdmissionInvoiceStatus;
import com.secretariapay.api.repository.admission.AdmissionApplicationRepository;
import com.secretariapay.api.repository.admission.AdmissionEnrollmentDocumentFileRepository;
import com.secretariapay.api.repository.admission.AdmissionInvoiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmissionEnrollmentDocumentFileStorageServiceTest {

    @Mock
    private AdmissionApplicationRepository applicationRepository;

    @Mock
    private AdmissionInvoiceRepository invoiceRepository;

    @Mock
    private AdmissionEnrollmentDocumentFileRepository fileRepository;

    @TempDir
    Path tempDir;

    @Test
    void shouldStorePassportPhotoForPaidApplication() throws Exception {
        UUID applicationId = UUID.randomUUID();
        AdmissionApplication application = eligibleApplication(applicationId);
        AdmissionInvoice invoice = new AdmissionInvoice().setStatus(AdmissionInvoiceStatus.PAID);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "foto.png",
                "image/png",
                pngBytes()
        );

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(invoiceRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(invoice));
        when(fileRepository.countByApplicationIdAndDocumentType(
                applicationId,
                AdmissionEnrollmentDocumentType.PASSPORT_PHOTO
        )).thenReturn(0L);
        when(fileRepository.save(any(AdmissionEnrollmentDocumentFile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdmissionEnrollmentDocumentFileStorageService.DocumentFileResponse response = service().store(
                applicationId,
                AdmissionEnrollmentDocumentType.PASSPORT_PHOTO,
                file,
                "secretaria@imetroangola.com"
        );

        assertEquals("image/png", response.mimeType());
        assertEquals("foto.png", response.originalFileName());
        assertEquals(AdmissionEnrollmentDocumentType.PASSPORT_PHOTO, response.documentType());

        ArgumentCaptor<AdmissionEnrollmentDocumentFile> captor =
                ArgumentCaptor.forClass(AdmissionEnrollmentDocumentFile.class);
        verify(fileRepository).save(captor.capture());
        Path stored = tempDir.resolve(applicationId.toString()).resolve(captor.getValue().getStoredName());
        assertTrue(Files.isRegularFile(stored));
    }

    @Test
    void shouldRejectPdfAsPassportPhoto() {
        UUID applicationId = UUID.randomUUID();
        AdmissionApplication application = eligibleApplication(applicationId);
        AdmissionInvoice invoice = new AdmissionInvoice().setStatus(AdmissionInvoiceStatus.PAID);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "foto.pdf",
                "application/pdf",
                "%PDF-1.7".getBytes()
        );

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(invoiceRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(invoice));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service().store(
                        applicationId,
                        AdmissionEnrollmentDocumentType.PASSPORT_PHOTO,
                        file,
                        "Secretaria"
                )
        );

        assertTrue(exception.getMessage().contains("JPG ou PNG"));
    }

    @Test
    void shouldRequireAllPhysicalFilesBeforeChecklistApproval() {
        UUID applicationId = UUID.randomUUID();
        when(fileRepository.countByApplicationIdAndDocumentType(
                applicationId,
                AdmissionEnrollmentDocumentType.PASSPORT_PHOTO
        )).thenReturn(2L);
        when(fileRepository.countByApplicationIdAndDocumentType(
                applicationId,
                AdmissionEnrollmentDocumentType.AUTHENTICATED_CERTIFICATE
        )).thenReturn(1L);
        when(fileRepository.countByApplicationIdAndDocumentType(
                applicationId,
                AdmissionEnrollmentDocumentType.IDENTITY_DOCUMENT
        )).thenReturn(1L);
        when(fileRepository.countByApplicationIdAndDocumentType(
                applicationId,
                AdmissionEnrollmentDocumentType.EDUCATION_EQUIVALENCE
        )).thenReturn(0L);

        assertTrue(service().hasRequiredFiles(applicationId, false));
        assertFalse(service().hasRequiredFiles(applicationId, true));
    }

    private AdmissionEnrollmentDocumentFileStorageService service() {
        return new AdmissionEnrollmentDocumentFileStorageService(
                applicationRepository,
                invoiceRepository,
                fileRepository,
                tempDir.toString(),
                5 * 1024 * 1024L
        );
    }

    private AdmissionApplication eligibleApplication(UUID applicationId) {
        AdmissionApplication application = new AdmissionApplication()
                .setApplicationCode("IMT-ADM-20260723-FILES")
                .setStatus(AdmissionApplicationStatus.DOCUMENTATION_PENDING);
        ReflectionTestUtils.setField(application, "id", applicationId);
        return application;
    }

    private byte[] pngBytes() {
        return new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x00
        };
    }
}
