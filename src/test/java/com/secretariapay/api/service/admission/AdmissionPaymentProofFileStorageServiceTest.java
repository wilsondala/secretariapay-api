package com.secretariapay.api.service.admission;

import com.secretariapay.api.dto.admission.AdmissionDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmissionPaymentProofFileStorageServiceTest {

    @Mock
    private AdmissionPublicPaymentService publicPaymentService;

    @TempDir
    Path tempDir;

    @Test
    void shouldStorePdfAndSubmitProofMetadata() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "comprovativo matrícula.pdf",
                "application/pdf",
                "%PDF-1.4 comprovativo de teste".getBytes(StandardCharsets.UTF_8)
        );
        AdmissionDto.PublicPaymentResponse expected = mock(AdmissionDto.PublicPaymentResponse.class);
        when(publicPaymentService.submitPaymentProof(
                eq("IMT-ADM-TESTE"),
                org.mockito.ArgumentMatchers.any(AdmissionDto.PublicPaymentProofRequest.class)
        )).thenReturn(expected);

        AdmissionPaymentProofFileStorageService service = new AdmissionPaymentProofFileStorageService(
                publicPaymentService,
                tempDir.toString(),
                5 * 1024 * 1024L
        );

        AdmissionDto.PublicPaymentResponse result = service.storeAndSubmit(
                "IMT-ADM-TESTE",
                "TESTE-BI-001",
                file
        );

        assertEquals(expected, result);
        ArgumentCaptor<AdmissionDto.PublicPaymentProofRequest> captor =
                ArgumentCaptor.forClass(AdmissionDto.PublicPaymentProofRequest.class);
        verify(publicPaymentService).submitPaymentProof(eq("IMT-ADM-TESTE"), captor.capture());

        AdmissionDto.PublicPaymentProofRequest request = captor.getValue();
        assertEquals("TESTE-BI-001", request.documentNumber());
        assertEquals("application/pdf", request.mimeType());
        assertEquals("comprovativo_matr_cula.pdf", request.fileName());
        assertTrue(request.fileUrl().matches(
                "/api/v1/admissions/payment-proof-files/[0-9a-f-]{36}\\.pdf"
        ));
        assertEquals(1L, Files.list(tempDir).count());
    }

    @Test
    void shouldRejectFileWithInvalidSignature() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "comprovativo.pdf",
                "application/pdf",
                "não é um pdf".getBytes(StandardCharsets.UTF_8)
        );
        AdmissionPaymentProofFileStorageService service = new AdmissionPaymentProofFileStorageService(
                publicPaymentService,
                tempDir.toString(),
                5 * 1024 * 1024L
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.storeAndSubmit("IMT-ADM-TESTE", "TESTE-BI-001", file)
        );

        assertTrue(exception.getMessage().contains("não corresponde"));
    }

    @Test
    void shouldRejectFileAboveConfiguredLimit() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "comprovativo.png",
                "image/png",
                new byte[32]
        );
        AdmissionPaymentProofFileStorageService service = new AdmissionPaymentProofFileStorageService(
                publicPaymentService,
                tempDir.toString(),
                8L
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.storeAndSubmit("IMT-ADM-TESTE", "TESTE-BI-001", file)
        );

        assertTrue(exception.getMessage().contains("5 MB"));
    }
}
