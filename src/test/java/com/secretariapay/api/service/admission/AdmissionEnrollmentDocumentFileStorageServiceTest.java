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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
                pdfBytes()
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

    @Test
    void shouldDeletePreviousPhysicalFileOnlyAfterCommitWhenReplacing() throws Exception {
        UUID applicationId = UUID.randomUUID();
        AdmissionApplication application = eligibleApplication(applicationId);
        AdmissionInvoice invoice = new AdmissionInvoice().setStatus(AdmissionInvoiceStatus.PAID);
        AdmissionEnrollmentDocumentFile previous = previousCertificate(application, "previous.pdf");
        Path previousPath = createPhysicalFile(applicationId, previous.getStoredName(), pdfBytes());
        MockMultipartFile replacement = new MockMultipartFile(
                "file",
                "certificado-novo.pdf",
                "application/pdf",
                pdfBytes()
        );

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(invoiceRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(invoice));
        when(fileRepository.findByApplicationIdAndDocumentTypeOrderByUploadedAtAsc(
                applicationId,
                AdmissionEnrollmentDocumentType.AUTHENTICATED_CERTIFICATE
        )).thenReturn(List.of(previous));
        when(fileRepository.save(any(AdmissionEnrollmentDocumentFile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionSynchronizationManager.initSynchronization();
        try {
            service().store(
                    applicationId,
                    AdmissionEnrollmentDocumentType.AUTHENTICATED_CERTIFICATE,
                    replacement,
                    "Secretaria"
            );
            Path replacementPath = storedPath(applicationId);

            assertTrue(Files.isRegularFile(previousPath));
            assertTrue(Files.isRegularFile(replacementPath));
            verify(fileRepository).delete(previous);

            completeSynchronization(TransactionSynchronization.STATUS_COMMITTED);

            assertFalse(Files.exists(previousPath));
            assertTrue(Files.isRegularFile(replacementPath));
        } finally {
            clearSynchronization();
        }
    }

    @Test
    void shouldPreservePreviousFileAndRemoveNewFileWhenTransactionRollsBack() throws Exception {
        UUID applicationId = UUID.randomUUID();
        AdmissionApplication application = eligibleApplication(applicationId);
        AdmissionInvoice invoice = new AdmissionInvoice().setStatus(AdmissionInvoiceStatus.PAID);
        AdmissionEnrollmentDocumentFile previous = previousCertificate(application, "previous.pdf");
        Path previousPath = createPhysicalFile(applicationId, previous.getStoredName(), pdfBytes());
        MockMultipartFile replacement = new MockMultipartFile(
                "file",
                "certificado-novo.pdf",
                "application/pdf",
                pdfBytes()
        );

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(invoiceRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(invoice));
        when(fileRepository.findByApplicationIdAndDocumentTypeOrderByUploadedAtAsc(
                applicationId,
                AdmissionEnrollmentDocumentType.AUTHENTICATED_CERTIFICATE
        )).thenReturn(List.of(previous));
        when(fileRepository.save(any(AdmissionEnrollmentDocumentFile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionSynchronizationManager.initSynchronization();
        try {
            service().store(
                    applicationId,
                    AdmissionEnrollmentDocumentType.AUTHENTICATED_CERTIFICATE,
                    replacement,
                    "Secretaria"
            );
            Path replacementPath = storedPath(applicationId);

            assertTrue(Files.isRegularFile(previousPath));
            assertTrue(Files.isRegularFile(replacementPath));

            completeSynchronization(TransactionSynchronization.STATUS_ROLLED_BACK);

            assertTrue(Files.isRegularFile(previousPath));
            assertFalse(Files.exists(replacementPath));
        } finally {
            clearSynchronization();
        }
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

    private AdmissionEnrollmentDocumentFile previousCertificate(
            AdmissionApplication application,
            String storedName
    ) {
        return new AdmissionEnrollmentDocumentFile()
                .setApplication(application)
                .setDocumentType(AdmissionEnrollmentDocumentType.AUTHENTICATED_CERTIFICATE)
                .setStoredName(storedName)
                .setOriginalFileName("certificado-anterior.pdf")
                .setMimeType("application/pdf")
                .setFileSize(pdfBytes().length)
                .setUploadedBy("Secretaria");
    }

    private Path createPhysicalFile(UUID applicationId, String storedName, byte[] content) throws Exception {
        Path directory = tempDir.resolve(applicationId.toString());
        Files.createDirectories(directory);
        return Files.write(directory.resolve(storedName), content);
    }

    private Path storedPath(UUID applicationId) {
        Path directory = tempDir.resolve(applicationId.toString());
        try (var paths = Files.list(directory)) {
            return paths
                    .filter(path -> !path.getFileName().toString().equals("previous.pdf"))
                    .findFirst()
                    .orElseThrow();
        } catch (Exception exception) {
            throw new IllegalStateException("Ficheiro novo não encontrado no teste.", exception);
        }
    }

    private void completeSynchronization(int status) {
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        if (status == TransactionSynchronization.STATUS_COMMITTED) {
            synchronizations.forEach(TransactionSynchronization::afterCommit);
        }
        synchronizations.forEach(synchronization -> synchronization.afterCompletion(status));
    }

    private void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private byte[] pngBytes() {
        return new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x00
        };
    }

    private byte[] pdfBytes() {
        return "%PDF-1.7\n".getBytes();
    }
}
