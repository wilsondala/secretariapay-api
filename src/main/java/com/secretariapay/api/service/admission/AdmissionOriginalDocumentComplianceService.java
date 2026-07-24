package com.secretariapay.api.service.admission;

import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.admission.AdmissionEnrollmentDocumentReview;
import com.secretariapay.api.entity.enrollment.AcademicEnrollmentRequest;
import com.secretariapay.api.entity.enums.enrollment.EnrollmentRequestStatus;
import com.secretariapay.api.repository.academic.StudentRepository;
import com.secretariapay.api.repository.admission.AdmissionEnrollmentDocumentReviewRepository;
import com.secretariapay.api.repository.enrollment.AcademicEnrollmentRequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AdmissionOriginalDocumentComplianceService {

    static final String BLOCK_REASON_PREFIX = "Documentos originais da matrícula não apresentados até ";
    private static final ZoneId LUANDA_ZONE = ZoneId.of("Africa/Luanda");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AdmissionEnrollmentDocumentReviewRepository reviewRepository;
    private final AcademicEnrollmentRequestRepository enrollmentRequestRepository;
    private final StudentRepository studentRepository;
    private final boolean blockingEnabled;

    public AdmissionOriginalDocumentComplianceService(
            AdmissionEnrollmentDocumentReviewRepository reviewRepository,
            AcademicEnrollmentRequestRepository enrollmentRequestRepository,
            StudentRepository studentRepository,
            @Value("${secretariapay.enrollment.original-documents-blocking-enabled:false}") boolean blockingEnabled
    ) {
        this.reviewRepository = reviewRepository;
        this.enrollmentRequestRepository = enrollmentRequestRepository;
        this.studentRepository = studentRepository;
        this.blockingEnabled = blockingEnabled;
    }

    @Scheduled(
            cron = "${secretariapay.enrollment.original-documents-blocking-cron:0 30 2 * * *}",
            zone = "Africa/Luanda"
    )
    @Transactional
    public void runScheduledBlocking() {
        if (blockingEnabled) blockOverdueStudents();
    }

    @Transactional
    public int blockOverdueStudents() {
        if (!blockingEnabled) return 0;

        LocalDate today = LocalDate.now(LUANDA_ZONE);
        List<AdmissionEnrollmentDocumentReview> overdue = reviewRepository
                .findByOriginalsVerifiedFalseAndOriginalsDueDateBeforeAndOriginalsBlockActiveFalse(today);

        int blocked = 0;
        for (AdmissionEnrollmentDocumentReview review : overdue) {
            AcademicEnrollmentRequest enrollment = enrollmentRequestRepository
                    .findByAdmissionApplicationId(review.getApplication().getId())
                    .orElse(null);
            if (enrollment == null
                    || enrollment.getStatus() != EnrollmentRequestStatus.COMPLETED
                    || enrollment.getStudent() == null) {
                continue;
            }

            Student student = enrollment.getStudent();
            String reason = BLOCK_REASON_PREFIX
                    + review.getOriginalsDueDate().format(DATE_FORMAT)
                    + ". Regularize a conferência presencial na Secretaria Académica.";
            student.setFinanciallyBlocked(true).setBlockedReason(reason);
            studentRepository.save(student);

            review.setOriginalsBlockActive(true)
                    .setOriginalsBlockedAt(LocalDateTime.now(LUANDA_ZONE));
            reviewRepository.save(review);
            blocked++;
        }
        return blocked;
    }

    @Transactional
    public void clearBlockIfOriginalsVerified(AdmissionEnrollmentDocumentReview review) {
        if (review == null
                || !Boolean.TRUE.equals(review.getOriginalsVerified())
                || !Boolean.TRUE.equals(review.getOriginalsBlockActive())
                || review.getApplication() == null) {
            return;
        }

        AcademicEnrollmentRequest enrollment = enrollmentRequestRepository
                .findByAdmissionApplicationId(review.getApplication().getId())
                .orElse(null);
        Student student = enrollment == null ? null : enrollment.getStudent();
        if (student != null
                && student.getBlockedReason() != null
                && student.getBlockedReason().startsWith(BLOCK_REASON_PREFIX)) {
            student.setFinanciallyBlocked(false).setBlockedReason(null);
            studentRepository.save(student);
        }

        review.setOriginalsBlockActive(false).setOriginalsBlockedAt(null);
        reviewRepository.save(review);
    }
}
