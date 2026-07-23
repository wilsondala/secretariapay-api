package com.secretariapay.api.service.admission;

import com.secretariapay.api.dto.admission.AdmissionDto;
import com.secretariapay.api.dto.enrollment.EnrollmentDto;
import com.secretariapay.api.entity.admission.AdmissionApplication;
import com.secretariapay.api.entity.admission.AdmissionEnrollmentDocumentReview;
import com.secretariapay.api.entity.admission.AdmissionInvoice;
import com.secretariapay.api.entity.enrollment.AcademicEnrollmentInvoice;
import com.secretariapay.api.entity.enrollment.AcademicEnrollmentRequest;
import com.secretariapay.api.entity.enums.admission.AdmissionApplicationStatus;
import com.secretariapay.api.entity.enums.admission.AdmissionInvoiceStatus;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.admission.AdmissionApplicationRepository;
import com.secretariapay.api.repository.admission.AdmissionEnrollmentDocumentReviewRepository;
import com.secretariapay.api.repository.admission.AdmissionInvoiceRepository;
import com.secretariapay.api.repository.enrollment.AcademicEnrollmentInvoiceRepository;
import com.secretariapay.api.repository.enrollment.AcademicEnrollmentRequestRepository;
import com.secretariapay.api.service.enrollment.EnrollmentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
public class AdmissionEnrollmentDocumentChecklistService {

    private static final ZoneId LUANDA_ZONE = ZoneId.of("Africa/Luanda");
    private static final Set<AdmissionApplicationStatus> TERMINAL_STATUSES = EnumSet.of(
            AdmissionApplicationStatus.REJECTED,
            AdmissionApplicationStatus.CANCELLED,
            AdmissionApplicationStatus.EXPIRED
    );

    private final AdmissionApplicationRepository applicationRepository;
    private final AdmissionInvoiceRepository admissionInvoiceRepository;
    private final AdmissionEnrollmentDocumentReviewRepository reviewRepository;
    private final AcademicEnrollmentRequestRepository enrollmentRequestRepository;
    private final AcademicEnrollmentInvoiceRepository enrollmentInvoiceRepository;
    private final EnrollmentService enrollmentService;
    private final AdmissionEnrollmentDocumentFileStorageService documentFileStorageService;
    private final int enrollmentPaymentDueDays;
    private final String enrollmentPaymentProvider;

    public AdmissionEnrollmentDocumentChecklistService(
            AdmissionApplicationRepository applicationRepository,
            AdmissionInvoiceRepository admissionInvoiceRepository,
            AdmissionEnrollmentDocumentReviewRepository reviewRepository,
            AcademicEnrollmentRequestRepository enrollmentRequestRepository,
            AcademicEnrollmentInvoiceRepository enrollmentInvoiceRepository,
            EnrollmentService enrollmentService,
            AdmissionEnrollmentDocumentFileStorageService documentFileStorageService,
            @Value("${secretariapay.enrollment.payment-due-days:3}") int enrollmentPaymentDueDays,
            @Value("${secretariapay.enrollment.payment-provider:BAI_TRANSFERENCIA_BANCARIA_PILOTO}") String enrollmentPaymentProvider
    ) {
        this.applicationRepository = applicationRepository;
        this.admissionInvoiceRepository = admissionInvoiceRepository;
        this.reviewRepository = reviewRepository;
        this.enrollmentRequestRepository = enrollmentRequestRepository;
        this.enrollmentInvoiceRepository = enrollmentInvoiceRepository;
        this.enrollmentService = enrollmentService;
        this.documentFileStorageService = documentFileStorageService;
        this.enrollmentPaymentDueDays = Math.max(1, enrollmentPaymentDueDays);
        this.enrollmentPaymentProvider = clean(
                enrollmentPaymentProvider,
                "BAI_TRANSFERENCIA_BANCARIA_PILOTO"
        );
    }

    @Transactional(readOnly = true)
    public AdmissionDto.EnrollmentDocumentChecklistResponse get(UUID applicationId) {
        AdmissionEnrollmentDocumentReview review = reviewRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new NotFoundException(
                        "O checklist documental da matrícula ainda não foi preenchido."
                ));
        return toResponse(review);
    }

    @Transactional
    public AdmissionDto.EnrollmentDocumentChecklistResponse review(
            UUID applicationId,
            AdmissionDto.EnrollmentDocumentChecklistRequest request
    ) {
        AdmissionApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Candidatura não encontrada."));

        if (TERMINAL_STATUSES.contains(application.getStatus())) {
            throw new IllegalArgumentException(
                    "Não é possível validar documentos de uma candidatura encerrada."
            );
        }

        AdmissionInvoice registrationInvoice = admissionInvoiceRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "A cobrança da inscrição não foi encontrada."
                ));
        if (registrationInvoice.getStatus() != AdmissionInvoiceStatus.PAID) {
            throw new IllegalArgumentException(
                    "O checklist de matrícula somente pode ser validado após a DCR confirmar o pagamento da inscrição."
            );
        }

        boolean ageEligible = isAgeEligible(application.getBirthDate());
        boolean studiedAbroad = Boolean.TRUE.equals(request.studiedAbroad());
        boolean equivalenceSatisfied = !studiedAbroad
                || Boolean.TRUE.equals(request.educationEquivalenceCopy());
        boolean requiredFilesPresent = documentFileStorageService.hasRequiredFiles(
                applicationId,
                studiedAbroad
        );
        boolean originalsPresented = Boolean.TRUE.equals(request.originalsPresented());
        boolean originalsVerified = Boolean.TRUE.equals(request.originalsVerified());

        if (originalsVerified && !originalsPresented) {
            throw new IllegalArgumentException(
                    "A autenticidade dos documentos somente pode ser confirmada depois da apresentação presencial dos originais."
            );
        }
        if ((originalsPresented || originalsVerified) && !requiredFilesPresent) {
            throw new IllegalArgumentException(
                    "Anexe primeiro todas as cópias digitais obrigatórias antes de registar a conferência presencial dos originais."
            );
        }

        boolean documentsComplete = Boolean.TRUE.equals(request.twoPassportPhotos())
                && Boolean.TRUE.equals(request.authenticatedCertificateCopy())
                && Boolean.TRUE.equals(request.identityDocumentCopy())
                && Boolean.TRUE.equals(request.secondaryEducationCompleted())
                && ageEligible
                && equivalenceSatisfied
                && requiredFilesPresent
                && originalsPresented
                && originalsVerified;

        AcademicEnrollmentRequest existingEnrollment = enrollmentRequestRepository
                .findByAdmissionApplicationId(applicationId)
                .orElse(null);
        if (existingEnrollment != null && !documentsComplete) {
            throw new IllegalArgumentException(
                    "A matrícula já foi iniciada. A documentação não pode ser marcada como pendente sem cancelamento formal do processo."
            );
        }

        LocalDateTime now = LocalDateTime.now(LUANDA_ZONE);
        String reviewer = request.reviewedBy().trim();
        AdmissionEnrollmentDocumentReview review = reviewRepository.findByApplicationId(applicationId)
                .orElseGet(AdmissionEnrollmentDocumentReview::new)
                .setApplication(application)
                .setTwoPassportPhotos(request.twoPassportPhotos())
                .setAuthenticatedCertificateCopy(request.authenticatedCertificateCopy())
                .setIdentityDocumentCopy(request.identityDocumentCopy())
                .setStudiedAbroad(request.studiedAbroad())
                .setEducationEquivalenceCopy(request.educationEquivalenceCopy())
                .setSecondaryEducationCompleted(request.secondaryEducationCompleted())
                .setAgeEligible(ageEligible)
                .setOriginalsPresented(originalsPresented)
                .setOriginalsVerified(originalsVerified)
                .setOriginalsVerifiedBy(originalsVerified ? reviewer : null)
                .setOriginalsVerifiedAt(originalsVerified ? now : null)
                .setOriginalsVerificationNotes(clean(request.originalsVerificationNotes(), null))
                .setDocumentsComplete(documentsComplete)
                .setReviewedBy(reviewer)
                .setNotes(clean(request.notes(), null))
                .setReviewedAt(now);
        reviewRepository.save(review);

        application.setDocumentsComplete(documentsComplete);
        if (documentsComplete) {
            application.setStatus(AdmissionApplicationStatus.CONFIRMED)
                    .setConfirmedAt(firstNonNull(
                            application.getConfirmedAt(),
                            now
                    ));
        } else {
            application.setStatus(AdmissionApplicationStatus.DOCUMENTATION_PENDING)
                    .setConfirmedAt(null);
        }
        applicationRepository.save(application);

        if (documentsComplete && existingEnrollment == null) {
            enrollmentService.createEnrollmentFromAdmission(
                    applicationId,
                    new EnrollmentDto.EnrollmentFromAdmissionRequest(
                            LocalDate.now(LUANDA_ZONE).plusDays(enrollmentPaymentDueDays),
                            1,
                            null,
                            enrollmentPaymentProvider
                    )
            );
        }

        return toResponse(reviewRepository.findByApplicationId(applicationId).orElse(review));
    }

    private AdmissionDto.EnrollmentDocumentChecklistResponse toResponse(
            AdmissionEnrollmentDocumentReview review
    ) {
        AdmissionApplication application = review.getApplication();
        AcademicEnrollmentRequest enrollment = enrollmentRequestRepository
                .findByAdmissionApplicationId(application.getId())
                .orElse(null);
        AcademicEnrollmentInvoice invoice = enrollment == null
                ? null
                : enrollmentInvoiceRepository.findByEnrollmentRequestId(enrollment.getId()).orElse(null);

        return new AdmissionDto.EnrollmentDocumentChecklistResponse(
                review.getId(),
                application.getId(),
                application.getApplicationCode(),
                review.getTwoPassportPhotos(),
                review.getAuthenticatedCertificateCopy(),
                review.getIdentityDocumentCopy(),
                review.getStudiedAbroad(),
                review.getEducationEquivalenceCopy(),
                review.getSecondaryEducationCompleted(),
                review.getAgeEligible(),
                review.getOriginalsPresented(),
                review.getOriginalsVerified(),
                review.getOriginalsVerifiedBy(),
                review.getOriginalsVerifiedAt(),
                review.getOriginalsVerificationNotes(),
                review.getDocumentsComplete(),
                review.getReviewedBy(),
                review.getNotes(),
                review.getReviewedAt(),
                enrollment == null ? null : enrollment.getId(),
                enrollment == null ? null : enrollment.getRequestCode(),
                enrollment == null ? null : enrollment.getStatus(),
                invoice == null ? null : invoice.getAmount(),
                invoice == null ? null : invoice.getCurrency(),
                invoice == null ? null : invoice.getDueDate(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }

    private boolean isAgeEligible(LocalDate birthDate) {
        if (birthDate == null || birthDate.isAfter(LocalDate.now(LUANDA_ZONE))) return false;
        return Period.between(birthDate, LocalDate.now(LUANDA_ZONE)).getYears() >= 18;
    }

    private LocalDateTime firstNonNull(LocalDateTime value, LocalDateTime fallback) {
        return value == null ? fallback : value;
    }

    private String clean(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.trim();
    }
}
