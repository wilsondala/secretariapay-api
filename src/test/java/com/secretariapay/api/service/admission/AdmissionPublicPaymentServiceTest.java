package com.secretariapay.api.service.admission;

import com.secretariapay.api.dto.admission.AdmissionDto;
import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.admission.AdmissionApplication;
import com.secretariapay.api.entity.admission.AdmissionCampaign;
import com.secretariapay.api.entity.admission.AdmissionInvoice;
import com.secretariapay.api.entity.enums.admission.AdmissionApplicationStatus;
import com.secretariapay.api.entity.enums.admission.AdmissionInvoiceStatus;
import com.secretariapay.api.repository.admission.AdmissionApplicationRepository;
import com.secretariapay.api.repository.admission.AdmissionInvoiceRepository;
import com.secretariapay.api.repository.admission.AdmissionPaymentProofRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdmissionPublicPaymentServiceTest {

    private static final ZoneId LUANDA_ZONE = ZoneId.of("Africa/Luanda");

    @Mock
    private AdmissionApplicationRepository applicationRepository;

    @Mock
    private AdmissionInvoiceRepository invoiceRepository;

    @Mock
    private AdmissionPaymentProofRepository proofRepository;

    @Mock
    private AdmissionService admissionService;

    private AdmissionApplication application;
    private AdmissionCampaign campaign;
    private Course course;
    private UUID applicationId;

    @BeforeEach
    void setUp() {
        applicationId = UUID.randomUUID();
        application = org.mockito.Mockito.mock(AdmissionApplication.class);
        campaign = org.mockito.Mockito.mock(AdmissionCampaign.class);
        course = org.mockito.Mockito.mock(Course.class);

        when(application.getId()).thenReturn(applicationId);
        when(application.getApplicationCode()).thenReturn("IMT-ADM-20260723-PILOTO");
        when(application.getDocumentNumber()).thenReturn("TESTE-PORTAL-PUBLICO-001");
        when(application.getFullName()).thenReturn("CANDIDATO TESTE PORTAL PÚBLICO");
        when(application.getDesiredCourse()).thenReturn(course);
        when(application.getDesiredShift()).thenReturn("MANHA");
        when(application.getAcademicYear()).thenReturn("2026/2027");
        when(application.getStatus()).thenReturn(AdmissionApplicationStatus.SUBMITTED);
        when(application.getTermsAccepted()).thenReturn(true);
        when(application.getSubmittedAt()).thenReturn(LocalDateTime.now());
        when(application.getCampaign()).thenReturn(campaign);
        when(course.getName()).thenReturn("Arquitectura");
        when(campaign.getRegistrationFee()).thenReturn(new BigDecimal("6500.00"));
    }

    @Test
    void shouldKeepPilotDisabledByDefaultAndHideBankingData() {
        when(applicationRepository.findByApplicationCodeIgnoreCase("IMT-ADM-20260723-PILOTO"))
                .thenReturn(Optional.of(application));
        when(invoiceRepository.findByApplicationId(applicationId)).thenReturn(Optional.empty());

        AdmissionDto.PublicPaymentResponse status = service(false).getStatus(
                "IMT-ADM-20260723-PILOTO",
                new AdmissionDto.PublicApplicationAccessRequest("TESTE-PORTAL-PUBLICO-001")
        );

        assertFalse(status.paymentInstructions().enabled());
        assertTrue(status.paymentInstructions().provisional());
        assertNull(status.paymentInstructions().iban());
        assertNull(status.invoice());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service(false).issueOrGetInvoice(
                        "IMT-ADM-20260723-PILOTO",
                        new AdmissionDto.PublicApplicationAccessRequest("TESTE-PORTAL-PUBLICO-001")
                )
        );
        assertTrue(exception.getMessage().contains("desativado"));
        verify(admissionService, never()).issueInvoice(any(), any());
    }

    @Test
    void shouldIssueOfficialRegistrationFeeWithProvisionalBankingInstructions() {
        when(applicationRepository.findByApplicationCodeIgnoreCase("IMT-ADM-20260723-PILOTO"))
                .thenReturn(Optional.of(application));

        AdmissionInvoice invoice = new AdmissionInvoice()
                .setApplication(application)
                .setInvoiceCode("IMT-INSCR-PILOTO-001")
                .setAmount(new BigDecimal("6500.00"))
                .setCurrency("AOA")
                .setDueDate(LocalDate.now(LUANDA_ZONE).plusDays(3))
                .setStatus(AdmissionInvoiceStatus.PENDING);

        when(invoiceRepository.findByApplicationId(applicationId))
                .thenReturn(Optional.empty(), Optional.of(invoice), Optional.of(invoice));
        when(invoiceRepository.save(invoice)).thenReturn(invoice);
        when(proofRepository.findFirstByInvoiceIdOrderByCreatedAtDesc(nullable(UUID.class)))
                .thenReturn(Optional.empty());
        when(admissionService.issueInvoice(any(), any())).thenReturn(org.mockito.Mockito.mock(AdmissionDto.ApplicationResponse.class));

        AdmissionDto.PublicPaymentResponse response = service(true).issueOrGetInvoice(
                "IMT-ADM-20260723-PILOTO",
                new AdmissionDto.PublicApplicationAccessRequest("TESTE-PORTAL-PUBLICO-001")
        );

        ArgumentCaptor<AdmissionDto.InvoiceRequest> requestCaptor = ArgumentCaptor.forClass(AdmissionDto.InvoiceRequest.class);
        verify(admissionService).issueInvoice(org.mockito.ArgumentMatchers.eq(applicationId), requestCaptor.capture());

        assertEquals(new BigDecimal("6500.00"), requestCaptor.getValue().amount());
        assertEquals(LocalDate.now(LUANDA_ZONE).plusDays(3), requestCaptor.getValue().dueDate());
        assertEquals("IMT-INSCR-PILOTO-001", response.invoice().paymentReference());
        assertEquals("BAI_TRANSFERENCIA_BANCARIA_PILOTO", response.invoice().provider());
        assertTrue(response.paymentInstructions().enabled());
        assertTrue(response.paymentInstructions().provisional());
        assertEquals("Banco Angolano de Investimento", response.paymentInstructions().bankName());
        assertEquals("OMNEN INTELENGENDA", response.paymentInstructions().accountHolder());
        assertEquals("AO06 0040 0000 6014 4677 1017 1", response.paymentInstructions().iban());
        assertEquals("06014467710001", response.paymentInstructions().accountNumber());
    }

    @Test
    void shouldRejectAccessWhenDocumentDoesNotMatchApplication() {
        when(applicationRepository.findByApplicationCodeIgnoreCase("IMT-ADM-20260723-PILOTO"))
                .thenReturn(Optional.of(application));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service(true).getStatus(
                        "IMT-ADM-20260723-PILOTO",
                        new AdmissionDto.PublicApplicationAccessRequest("DOCUMENTO-INCORRETO")
                )
        );

        assertEquals("Candidatura não encontrada ou documento inválido.", exception.getMessage());
        verify(invoiceRepository, never()).findByApplicationId(any());
    }

    private AdmissionPublicPaymentService service(boolean enabled) {
        return new AdmissionPublicPaymentService(
                applicationRepository,
                invoiceRepository,
                proofRepository,
                admissionService,
                enabled,
                3,
                "HOMOLOGAÇÃO LOCAL",
                "BAI_TRANSFERENCIA_BANCARIA_PILOTO",
                "Banco Angolano de Investimento",
                "OMNEN INTELENGENDA",
                "AO06 0040 0000 6014 4677 1017 1",
                "06014467710001",
                "Multicaixa Express / transferência bancária para a conta AKZ indicada",
                "Unitel Money/Afrimoney quando autorizado pela instituição",
                "+244 923 168 085",
                "secretaria.financeira@imetroangola.com"
        );
    }
}
