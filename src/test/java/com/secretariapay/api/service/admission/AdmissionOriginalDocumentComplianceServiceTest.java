package com.secretariapay.api.service.admission;

import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.admission.AdmissionApplication;
import com.secretariapay.api.entity.admission.AdmissionEnrollmentDocumentReview;
import com.secretariapay.api.entity.enrollment.AcademicEnrollmentRequest;
import com.secretariapay.api.entity.enums.enrollment.EnrollmentRequestStatus;
import com.secretariapay.api.repository.academic.StudentRepository;
import com.secretariapay.api.repository.admission.AdmissionEnrollmentDocumentReviewRepository;
import com.secretariapay.api.repository.enrollment.AcademicEnrollmentRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
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
class AdmissionOriginalDocumentComplianceServiceTest {

    @Mock
    private AdmissionEnrollmentDocumentReviewRepository reviewRepository;
    @Mock
    private AcademicEnrollmentRequestRepository enrollmentRequestRepository;
    @Mock
    private StudentRepository studentRepository;

    @Test
    void shouldRemainDisabledDuringInitialPilot() {
        int blocked = service(false).blockOverdueStudents();

        assertEquals(0, blocked);
        verify(reviewRepository, never())
                .findByOriginalsVerifiedFalseAndOriginalsDueDateBeforeAndOriginalsBlockActiveFalse(any());
    }

    @Test
    void shouldBlockCompletedStudentAfterOriginalsDeadlineWhenEnabled() {
        UUID applicationId = UUID.randomUUID();
        AdmissionApplication application = new AdmissionApplication()
                .setApplicationCode("IMT-ADM-ORIGINAIS-ATRASADOS");
        ReflectionTestUtils.setField(application, "id", applicationId);
        AdmissionEnrollmentDocumentReview review = new AdmissionEnrollmentDocumentReview()
                .setApplication(application)
                .setOriginalsVerified(false)
                .setOriginalsDueDate(LocalDate.now().minusDays(1))
                .setOriginalsBlockActive(false);
        Student student = new Student().setStudentNumber("202601234");
        AcademicEnrollmentRequest enrollment = new AcademicEnrollmentRequest()
                .setStatus(EnrollmentRequestStatus.COMPLETED)
                .setStudent(student);

        when(reviewRepository.findByOriginalsVerifiedFalseAndOriginalsDueDateBeforeAndOriginalsBlockActiveFalse(any()))
                .thenReturn(List.of(review));
        when(enrollmentRequestRepository.findByAdmissionApplicationId(applicationId))
                .thenReturn(Optional.of(enrollment));

        int blocked = service(true).blockOverdueStudents();

        assertEquals(1, blocked);
        assertTrue(student.getFinanciallyBlocked());
        assertTrue(student.getBlockedReason().contains("Documentos originais"));
        assertTrue(review.getOriginalsBlockActive());
        verify(studentRepository).save(student);
        verify(reviewRepository).save(review);
    }

    @Test
    void shouldRemoveOnlyDocumentationBlockAfterOriginalsVerification() {
        UUID applicationId = UUID.randomUUID();
        AdmissionApplication application = new AdmissionApplication();
        ReflectionTestUtils.setField(application, "id", applicationId);
        AdmissionEnrollmentDocumentReview review = new AdmissionEnrollmentDocumentReview()
                .setApplication(application)
                .setOriginalsVerified(true)
                .setOriginalsBlockActive(true);
        Student student = new Student()
                .setFinanciallyBlocked(true)
                .setBlockedReason("Documentos originais da matrícula não apresentados até 08/09/2026. Regularize a conferência presencial na Secretaria Académica.");
        AcademicEnrollmentRequest enrollment = new AcademicEnrollmentRequest().setStudent(student);

        when(enrollmentRequestRepository.findByAdmissionApplicationId(applicationId))
                .thenReturn(Optional.of(enrollment));

        service(true).clearBlockIfOriginalsVerified(review);

        assertFalse(student.getFinanciallyBlocked());
        assertEquals(null, student.getBlockedReason());
        assertFalse(review.getOriginalsBlockActive());
        verify(studentRepository).save(student);
        verify(reviewRepository).save(review);
    }

    private AdmissionOriginalDocumentComplianceService service(boolean enabled) {
        return new AdmissionOriginalDocumentComplianceService(
                reviewRepository,
                enrollmentRequestRepository,
                studentRepository,
                enabled
        );
    }
}
