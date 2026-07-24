package com.secretariapay.api.service.enrollment;

import com.secretariapay.api.dto.enrollment.EnrollmentDto;
import com.secretariapay.api.entity.academic.AcademicClass;
import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.academic.Institution;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.admission.AdmissionApplication;
import com.secretariapay.api.entity.admission.AdmissionCampaign;
import com.secretariapay.api.entity.admission.AdmissionInvoice;
import com.secretariapay.api.entity.enrollment.AcademicEnrollmentInvoice;
import com.secretariapay.api.entity.enrollment.AcademicEnrollmentRequest;
import com.secretariapay.api.entity.enums.admission.AdmissionApplicationStatus;
import com.secretariapay.api.entity.enums.admission.AdmissionInvoiceStatus;
import com.secretariapay.api.entity.enums.admission.AdmissionShift;
import com.secretariapay.api.entity.enums.enrollment.EnrollmentRequestStatus;
import com.secretariapay.api.entity.enums.enrollment.EnrollmentRequestType;
import com.secretariapay.api.repository.academic.AcademicClassRepository;
import com.secretariapay.api.repository.academic.CourseRepository;
import com.secretariapay.api.repository.academic.StudentRepository;
import com.secretariapay.api.repository.admission.AdmissionApplicationRepository;
import com.secretariapay.api.repository.admission.AdmissionCampaignRepository;
import com.secretariapay.api.repository.admission.AdmissionCourseOfferingRepository;
import com.secretariapay.api.repository.admission.AdmissionInvoiceRepository;
import com.secretariapay.api.repository.enrollment.AcademicEnrollmentInvoiceRepository;
import com.secretariapay.api.repository.enrollment.AcademicEnrollmentPaymentProofRepository;
import com.secretariapay.api.repository.enrollment.AcademicEnrollmentRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock private AcademicEnrollmentRequestRepository requestRepository;
    @Mock private AcademicEnrollmentInvoiceRepository invoiceRepository;
    @Mock private AcademicEnrollmentPaymentProofRepository proofRepository;
    @Mock private AdmissionApplicationRepository admissionApplicationRepository;
    @Mock private AdmissionInvoiceRepository admissionInvoiceRepository;
    @Mock private AdmissionCampaignRepository campaignRepository;
    @Mock private AdmissionCourseOfferingRepository offeringRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private AcademicClassRepository academicClassRepository;

    private EnrollmentService service;

    @BeforeEach
    void setUp() {
        service = new EnrollmentService(
                requestRepository,
                invoiceRepository,
                proofRepository,
                admissionApplicationRepository,
                admissionInvoiceRepository,
                campaignRepository,
                offeringRepository,
                studentRepository,
                courseRepository,
                academicClassRepository
        );
        when(requestRepository.save(any(AcademicEnrollmentRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceRepository.save(any(AcademicEnrollmentInvoice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldUseOfficialEnrollmentFeeFromCampaign() {
        UUID applicationId = UUID.randomUUID();
        Institution institution = persistedInstitution("IMETRO");
        Course course = new Course().setInstitution(institution).setName("Engenharia Civil").setActive(true);
        AdmissionCampaign campaign = new AdmissionCampaign()
                .setInstitution(institution)
                .setAcademicYear("2026/2027")
                .setEnrollmentFee(new BigDecimal("23500.00"))
                .setCurrency("AOA");
        AdmissionApplication application = new AdmissionApplication()
                .setInstitution(institution)
                .setCampaign(campaign)
                .setDesiredCourse(course)
                .setDesiredShift("NOITE")
                .setAcademicYear("2026/2027")
                .setFullName("Candidato Teste")
                .setDocumentNumber("BI-TESTE-001")
                .setStatus(AdmissionApplicationStatus.CONFIRMED);
        AdmissionInvoice registrationInvoice = new AdmissionInvoice()
                .setStatus(AdmissionInvoiceStatus.PAID);

        when(admissionApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(requestRepository.existsByAdmissionApplicationId(applicationId)).thenReturn(false);
        when(admissionInvoiceRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(registrationInvoice));
        when(offeringRepository.existsByCampaignIdAndCourseIdAndShiftAndActiveTrue(
                nullable(UUID.class),
                nullable(UUID.class),
                any(AdmissionShift.class)))
                .thenReturn(true);

        service.createEnrollmentFromAdmission(
                applicationId,
                new EnrollmentDto.EnrollmentFromAdmissionRequest(
                        LocalDate.now().plusDays(5),
                        1,
                        "MAT-REF-001",
                        "MANUAL_LOCAL"
                )
        );

        ArgumentCaptor<AcademicEnrollmentInvoice> captor = ArgumentCaptor.forClass(AcademicEnrollmentInvoice.class);
        verify(invoiceRepository).save(captor.capture());
        assertEquals(new BigDecimal("23500.00"), captor.getValue().getAmount());
        assertEquals(EnrollmentRequestType.ENROLLMENT,
                captor.getValue().getEnrollmentRequest().getRequestType());
    }

    @Test
    void shouldUseOfficialReenrollmentFeeFromCampaign() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        Institution institution = persistedInstitution("IMETRO");
        Course currentCourse = new Course().setInstitution(institution).setName("Economia").setActive(true);
        Course targetCourse = new Course().setInstitution(institution).setName("Economia").setActive(true);
        AcademicClass currentClass = new AcademicClass().setCourse(currentCourse);
        Student student = new Student()
                .setAcademicClass(currentClass)
                .setStudentNumber("IMT-2025-0001")
                .setFullName("Estudante Teste");
        AdmissionCampaign campaign = new AdmissionCampaign()
                .setInstitution(institution)
                .setAcademicYear("2026/2027")
                .setReenrollmentFee(new BigDecimal("23500.00"))
                .setCurrency("AOA");

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(targetCourse));
        when(campaignRepository.findFirstByInstitutionIdAndActiveTrueOrderByRegistrationStartDesc(any(UUID.class)))
                .thenReturn(Optional.of(campaign));
        when(requestRepository.existsByStudentIdAndAcademicYearAndRequestTypeAndStatusNot(
                nullable(UUID.class),
                any(String.class),
                any(EnrollmentRequestType.class),
                any(EnrollmentRequestStatus.class)))
                .thenReturn(false);
        when(offeringRepository.existsByCampaignIdAndCourseIdAndShiftAndActiveTrue(
                nullable(UUID.class),
                nullable(UUID.class),
                any(AdmissionShift.class)))
                .thenReturn(true);

        service.createReenrollment(new EnrollmentDto.ReenrollmentRequest(
                studentId,
                courseId,
                "NOITE",
                "2026/2027",
                2,
                LocalDate.now().plusDays(5),
                "REMAT-REF-001",
                "MANUAL_LOCAL"
        ));

        ArgumentCaptor<AcademicEnrollmentInvoice> captor = ArgumentCaptor.forClass(AcademicEnrollmentInvoice.class);
        verify(invoiceRepository).save(captor.capture());
        assertEquals(new BigDecimal("23500.00"), captor.getValue().getAmount());
        assertEquals(EnrollmentRequestType.REENROLLMENT,
                captor.getValue().getEnrollmentRequest().getRequestType());
    }

    private Institution persistedInstitution(String name) {
        Institution institution = mock(Institution.class);
        when(institution.getId()).thenReturn(UUID.randomUUID());
        when(institution.getName()).thenReturn(name);
        return institution;
    }
}
