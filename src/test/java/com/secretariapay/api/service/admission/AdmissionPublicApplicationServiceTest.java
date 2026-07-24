package com.secretariapay.api.service.admission;

import com.secretariapay.api.dto.admission.AdmissionDto;
import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.academic.Institution;
import com.secretariapay.api.entity.admission.AdmissionApplication;
import com.secretariapay.api.entity.admission.AdmissionCampaign;
import com.secretariapay.api.entity.enums.admission.AdmissionApplicationStatus;
import com.secretariapay.api.entity.enums.admission.AdmissionShift;
import com.secretariapay.api.entity.enums.admission.AdmissionSourceChannel;
import com.secretariapay.api.repository.academic.CourseRepository;
import com.secretariapay.api.repository.academic.InstitutionRepository;
import com.secretariapay.api.repository.admission.AdmissionApplicationRepository;
import com.secretariapay.api.repository.admission.AdmissionCampaignRepository;
import com.secretariapay.api.repository.admission.AdmissionCourseOfferingRepository;
import com.secretariapay.api.repository.admission.AdmissionInvoiceRepository;
import com.secretariapay.api.repository.admission.AdmissionLeadRepository;
import com.secretariapay.api.repository.admission.AdmissionPaymentProofRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdmissionPublicApplicationServiceTest {

    private static final ZoneId LUANDA_ZONE = ZoneId.of("Africa/Luanda");

    @Mock
    private AdmissionLeadRepository leadRepository;

    @Mock
    private AdmissionApplicationRepository applicationRepository;

    @Mock
    private AdmissionInvoiceRepository invoiceRepository;

    @Mock
    private AdmissionPaymentProofRepository proofRepository;

    @Mock
    private InstitutionRepository institutionRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private AdmissionCampaignRepository campaignRepository;

    @Mock
    private AdmissionCourseOfferingRepository offeringRepository;

    @InjectMocks
    private AdmissionService admissionService;

    @Test
    void shouldRejectPublicApplicationBeforeOfficialRegistrationPeriod() {
        TestContext context = prepareContext(
                LocalDate.now(LUANDA_ZONE).plusDays(1),
                LocalDate.now(LUANDA_ZONE).plusDays(30)
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> admissionService.createApplication(context.request(), AdmissionSourceChannel.FORM)
        );

        assertTrue(exception.getMessage().contains("As inscrições estarão disponíveis de"));
        verify(applicationRepository, never()).save(any(AdmissionApplication.class));
    }

    @Test
    void shouldAcceptPublicApplicationDuringOfficialRegistrationPeriod() {
        TestContext context = prepareContext(
                LocalDate.now(LUANDA_ZONE).minusDays(1),
                LocalDate.now(LUANDA_ZONE).plusDays(1)
        );

        when(applicationRepository.save(any(AdmissionApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceRepository.findByApplicationId(nullable(UUID.class))).thenReturn(Optional.empty());

        AdmissionDto.ApplicationResponse response = admissionService.createApplication(
                context.request(),
                AdmissionSourceChannel.FORM
        );

        assertEquals(AdmissionSourceChannel.FORM, response.sourceChannel());
        assertEquals(AdmissionApplicationStatus.SUBMITTED, response.status());
        assertEquals("2026/2027", response.academicYear());
        assertEquals("Arquitectura", response.desiredCourseName());
        assertEquals("MANHA", response.desiredShift());
        assertEquals("TESTE-PORTAL-PUBLICO-001", response.documentNumber());
        assertNotNull(response.applicationCode());
        assertNotNull(response.submittedAt());
        verify(applicationRepository).save(any(AdmissionApplication.class));
    }

    private TestContext prepareContext(LocalDate registrationStart, LocalDate registrationEnd) {
        UUID institutionId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();

        Institution institution = org.mockito.Mockito.mock(Institution.class);
        Course course = org.mockito.Mockito.mock(Course.class);
        AdmissionCampaign campaign = org.mockito.Mockito.mock(AdmissionCampaign.class);

        when(institutionRepository.findById(institutionId)).thenReturn(Optional.of(institution));
        when(institution.getId()).thenReturn(institutionId);
        when(institution.getName()).thenReturn("IMETRO");

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(course.getInstitution()).thenReturn(institution);
        when(course.getId()).thenReturn(courseId);
        when(course.getName()).thenReturn("Arquitectura");
        when(course.getActive()).thenReturn(true);

        when(campaignRepository.findFirstByInstitutionIdAndActiveTrueOrderByRegistrationStartDesc(institutionId))
                .thenReturn(Optional.of(campaign));
        when(campaign.getId()).thenReturn(campaignId);
        when(campaign.getCampaignCode()).thenReturn("IMETRO-ADM-2026-2027");
        when(campaign.getAcademicYear()).thenReturn("2026/2027");
        when(campaign.getRegistrationStart()).thenReturn(registrationStart);
        when(campaign.getRegistrationEnd()).thenReturn(registrationEnd);
        when(campaign.getPublicFormEnabled()).thenReturn(true);
        when(campaign.getActive()).thenReturn(true);

        when(offeringRepository.existsByCampaignIdAndCourseIdAndShiftAndActiveTrue(
                campaignId,
                courseId,
                AdmissionShift.MANHA
        )).thenReturn(true);

        return new TestContext(applicationRequest(institutionId, courseId));
    }

    private AdmissionDto.ApplicationRequest applicationRequest(UUID institutionId, UUID courseId) {
        return new AdmissionDto.ApplicationRequest(
                institutionId,
                null,
                courseId,
                "MANHA",
                "2026/2027",
                "CANDIDATO TESTE PORTAL PÚBLICO",
                "BI",
                "TESTE-PORTAL-PUBLICO-001",
                LocalDate.of(1991, 4, 14),
                "+244 923 000 105",
                "+244 923 000 105",
                "candidato.portal.2026@example.com",
                "Escola de Teste",
                "Luanda",
                "Belas",
                false,
                true,
                null
        );
    }

    private record TestContext(AdmissionDto.ApplicationRequest request) {}
}
