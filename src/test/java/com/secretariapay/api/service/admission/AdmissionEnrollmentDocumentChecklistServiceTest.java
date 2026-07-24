package com.secretariapay.api.service.admission;

import com.secretariapay.api.dto.admission.AdmissionDto;
import com.secretariapay.api.dto.enrollment.EnrollmentDto;
import com.secretariapay.api.entity.admission.AdmissionApplication;
import com.secretariapay.api.entity.admission.AdmissionEnrollmentDocumentReview;
import com.secretariapay.api.entity.admission.AdmissionInvoice;
import com.secretariapay.api.entity.enrollment.AcademicEnrollmentRequest;
import com.secretariapay.api.entity.enums.admission.AdmissionApplicationStatus;
import com.secretariapay.api.entity.enums.admission.AdmissionInvoiceStatus;
import com.secretariapay.api.repository.admission.AdmissionApplicationRepository;
import com.secretariapay.api.repository.admission.AdmissionEnrollmentDocumentReviewRepository;
import com.secretariapay.api.repository.admission.AdmissionInvoiceRepository;
import com.secretariapay.api.repository.enrollment.AcademicEnrollmentInvoiceRepository;
import com.secretariapay.api.repository.enrollment.AcademicEnrollmentRequestRepository;
import com.secretariapay.api.service.enrollment.EnrollmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmissionEnrollmentDocumentChecklistServiceTest {

    private static final ZoneId LUANDA_ZONE = ZoneId.of("Africa/Luanda");

    @Mock
    private AdmissionApplicationRepository applicationRepository;
    @Mock
    private AdmissionInvoiceRepository admissionInvoiceRepository;
    @Mock
    private AdmissionEnrollmentDocumentReviewRepository reviewRepository;
    @Mock
    private AcademicEnrollmentRequestRepository enrollmentRequestRepository;
    @Mock
    private AcademicEnrollmentInvoiceRepository enrollmentInvoiceRepository;
    @Mock
    private EnrollmentService enrollmentService;
    @Mock
    private AdmissionEnrollmentDocumentFileStorageService documentFileStorageService;
    @Mock
    private AdmissionOriginalDocumentComplianceService originalDocumentComplianceService;
    @Mock
    private AdmissionEnrollmentChargeNotificationService enrollmentChargeNotificationService;

    @Test
    void shouldCreateEnrollmentChargeWithCompleteDigitalDocumentsEvenWithoutOriginals() {
        UUID applicationId = UUID.randomUUID();
        AdmissionApplication application = paidApplication(applicationId, LocalDate.of(2000, 1, 10));
        AdmissionInvoice registrationInvoice = new AdmissionInvoice().setStatus(AdmissionInvoiceStatus.PAID);
        EnrollmentDto.EnrollmentResponse enrollmentResponse = org.mockito.Mockito.mock(EnrollmentDto.EnrollmentResponse.class);

        standardReviewStubs(applicationId, application, registrationInvoice, Optional.empty());
        when(documentFileStorageService.hasRequiredFiles(applicationId, false)).thenReturn(true);
        when(enrollmentService.createEnrollmentFromAdmission(
                org.mockito.ArgumentMatchers.eq(applicationId),
                any(EnrollmentDto.EnrollmentFromAdmissionRequest.class)
        )).thenReturn(enrollmentResponse);

        AdmissionDto.EnrollmentDocumentChecklistResponse response = service().review(
                applicationId,
                completeDomesticRequest(false, false)
        );

        assertTrue(response.documentsComplete());
        assertTrue(response.ageEligible());
        assertFalse(response.originalsVerified());
        assertEquals(AdmissionApplicationStatus.CONFIRMED, application.getStatus());

        ArgumentCaptor<EnrollmentDto.EnrollmentFromAdmissionRequest> captor =
                ArgumentCaptor.forClass(EnrollmentDto.EnrollmentFromAdmissionRequest.class);
        verify(enrollmentService).createEnrollmentFromAdmission(
                org.mockito.ArgumentMatchers.eq(applicationId),
                captor.capture()
        );
        assertEquals(1, captor.getValue().targetYearLevel());
        assertEquals("BAI_TRANSFERENCIA_BANCARIA_PILOTO", captor.getValue().provider());
        assertEquals(LocalDate.now(LUANDA_ZONE).plusDays(3), captor.getValue().dueDate());
        verify(enrollmentChargeNotificationService).enqueue(application, enrollmentResponse);
    }

    @Test
    void shouldKeepDocumentsPendingWhenRequiredFilesWereNotUploaded() {
        UUID applicationId = UUID.randomUUID();
        AdmissionApplication application = paidApplication(applicationId, LocalDate.of(2000, 1, 10));
        AdmissionInvoice registrationInvoice = new AdmissionInvoice().setStatus(AdmissionInvoiceStatus.PAID);

        standardReviewStubs(applicationId, application, registrationInvoice, Optional.empty());
        when(documentFileStorageService.hasRequiredFiles(applicationId, false)).thenReturn(false);

        AdmissionDto.EnrollmentDocumentChecklistResponse response = service().review(
                applicationId,
                completeDomesticRequest(false, false)
        );

        assertFalse(response.documentsComplete());
        assertEquals(AdmissionApplicationStatus.DOCUMENTATION_PENDING, application.getStatus());
        verify(enrollmentService, never()).createEnrollmentFromAdmission(any(), any());
        verify(enrollmentChargeNotificationService, never()).enqueue(any(), any());
    }

    @Test
    void shouldKeepDocumentsPendingWhenCandidateStudiedAbroadWithoutEquivalence() {
        UUID applicationId = UUID.randomUUID();
        AdmissionApplication application = paidApplication(applicationId, LocalDate.of(1998, 4, 20));
        AdmissionInvoice registrationInvoice = new AdmissionInvoice().setStatus(AdmissionInvoiceStatus.PAID);

        standardReviewStubs(applicationId, application, registrationInvoice, Optional.empty());
        when(documentFileStorageService.hasRequiredFiles(applicationId, true)).thenReturn(false);

        AdmissionDto.EnrollmentDocumentChecklistResponse response = service().review(
                applicationId,
                new AdmissionDto.EnrollmentDocumentChecklistRequest(
                        true,
                        true,
                        true,
                        true,
                        false,
                        true,
                        false,
                        false,
                        "Secretaria Académica",
                        "Equivalência pendente.",
                        null
                )
        );

        assertFalse(response.documentsComplete());
        assertEquals(AdmissionApplicationStatus.DOCUMENTATION_PENDING, application.getStatus());
        verify(enrollmentService, never()).createEnrollmentFromAdmission(any(), any());
    }

    @Test
    void shouldKeepDocumentsPendingWhenCandidateIsUnderEighteen() {
        UUID applicationId = UUID.randomUUID();
        AdmissionApplication application = paidApplication(
                applicationId,
                LocalDate.now(LUANDA_ZONE).minusYears(17)
        );
        AdmissionInvoice registrationInvoice = new AdmissionInvoice().setStatus(AdmissionInvoiceStatus.PAID);

        standardReviewStubs(applicationId, application, registrationInvoice, Optional.empty());
        when(documentFileStorageService.hasRequiredFiles(applicationId, false)).thenReturn(true);

        AdmissionDto.EnrollmentDocumentChecklistResponse response = service().review(
                applicationId,
                completeDomesticRequest(false, false)
        );

        assertFalse(response.ageEligible());
        assertFalse(response.documentsComplete());
        verify(enrollmentService, never()).createEnrollmentFromAdmission(any(), any());
    }

    @Test
    void shouldNotDuplicateEnrollmentWhenDigitalChecklistIsReviewedAgain() {
        UUID applicationId = UUID.randomUUID();
        AdmissionApplication application = paidApplication(applicationId, LocalDate.of(1999, 8, 15));
        AdmissionInvoice registrationInvoice = new AdmissionInvoice().setStatus(AdmissionInvoiceStatus.PAID);
        AcademicEnrollmentRequest existingEnrollment = new AcademicEnrollmentRequest()
                .setRequestCode("IMT-MAT-20260723-EXISTENTE");

        standardReviewStubs(
                applicationId,
                application,
                registrationInvoice,
                Optional.of(existingEnrollment)
        );
        when(documentFileStorageService.hasRequiredFiles(applicationId, false)).thenReturn(true);

        AdmissionDto.EnrollmentDocumentChecklistResponse response = service().review(
                applicationId,
                completeDomesticRequest(false, false)
        );

        assertTrue(response.documentsComplete());
        verify(enrollmentService, never()).createEnrollmentFromAdmission(any(), any());
        verify(enrollmentChargeNotificationService, never()).enqueue(any(), any());
    }

    @Test
    void shouldClearDocumentationBlockAfterOriginalsAreVerifiedLater() {
        UUID applicationId = UUID.randomUUID();
        AdmissionApplication application = paidApplication(applicationId, LocalDate.of(1999, 8, 15));
        AdmissionInvoice registrationInvoice = new AdmissionInvoice().setStatus(AdmissionInvoiceStatus.PAID);
        AcademicEnrollmentRequest existingEnrollment = new AcademicEnrollmentRequest()
                .setRequestCode("IMT-MAT-20260723-CONCLUIDA");
        AdmissionEnrollmentDocumentReview existingReview = new AdmissionEnrollmentDocumentReview()
                .setApplication(application)
                .setOriginalsDueDate(LocalDate.now(LUANDA_ZONE).plusDays(10))
                .setReviewedBy("Secretaria Académica");

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(admissionInvoiceRepository.findByApplicationId(applicationId))
                .thenReturn(Optional.of(registrationInvoice));
        when(enrollmentRequestRepository.findByAdmissionApplicationId(applicationId))
                .thenReturn(Optional.of(existingEnrollment));
        when(reviewRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(existingReview));
        when(reviewRepository.save(any(AdmissionEnrollmentDocumentReview.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationRepository.save(application)).thenReturn(application);
        when(documentFileStorageService.hasRequiredFiles(applicationId, false)).thenReturn(true);

        AdmissionDto.EnrollmentDocumentChecklistResponse response = service().review(
                applicationId,
                completeDomesticRequest(true, true)
        );

        assertTrue(response.documentsComplete());
        assertTrue(response.originalsVerified());
        verify(originalDocumentComplianceService).clearBlockIfOriginalsVerified(existingReview);
        verify(enrollmentService, never()).createEnrollmentFromAdmission(any(), any());
    }

    private void standardReviewStubs(
            UUID applicationId,
            AdmissionApplication application,
            AdmissionInvoice registrationInvoice,
            Optional<AcademicEnrollmentRequest> enrollment
    ) {
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(admissionInvoiceRepository.findByApplicationId(applicationId))
                .thenReturn(Optional.of(registrationInvoice));
        when(enrollmentRequestRepository.findByAdmissionApplicationId(applicationId))
                .thenReturn(enrollment);
        when(reviewRepository.findByApplicationId(applicationId)).thenReturn(Optional.empty());
        when(reviewRepository.save(any(AdmissionEnrollmentDocumentReview.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(applicationRepository.save(application)).thenReturn(application);
    }

    private AdmissionEnrollmentDocumentChecklistService service() {
        return new AdmissionEnrollmentDocumentChecklistService(
                applicationRepository,
                admissionInvoiceRepository,
                reviewRepository,
                enrollmentRequestRepository,
                enrollmentInvoiceRepository,
                enrollmentService,
                documentFileStorageService,
                originalDocumentComplianceService,
                enrollmentChargeNotificationService,
                3,
                30,
                "BAI_TRANSFERENCIA_BANCARIA_PILOTO"
        );
    }

    private AdmissionApplication paidApplication(UUID applicationId, LocalDate birthDate) {
        AdmissionApplication application = new AdmissionApplication()
                .setApplicationCode("IMT-ADM-20260723-CHECKLIST")
                .setBirthDate(birthDate)
                .setStatus(AdmissionApplicationStatus.DOCUMENTATION_PENDING);
        ReflectionTestUtils.setField(application, "id", applicationId);
        return application;
    }

    private AdmissionDto.EnrollmentDocumentChecklistRequest completeDomesticRequest(
            boolean originalsPresented,
            boolean originalsVerified
    ) {
        return new AdmissionDto.EnrollmentDocumentChecklistRequest(
                true,
                true,
                true,
                false,
                false,
                true,
                originalsPresented,
                originalsVerified,
                "Secretaria Académica",
                "Documentação digital conferida.",
                originalsVerified ? "Originais apresentados sem divergências." : null
        );
    }
}
