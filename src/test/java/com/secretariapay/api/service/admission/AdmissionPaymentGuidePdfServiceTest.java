package com.secretariapay.api.service.admission;

import com.secretariapay.api.dto.admission.AdmissionDto;
import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.academic.Institution;
import com.secretariapay.api.entity.admission.AdmissionApplication;
import com.secretariapay.api.entity.admission.AdmissionInvoice;
import com.secretariapay.api.entity.enums.admission.AdmissionInvoiceStatus;
import com.secretariapay.api.repository.admission.AdmissionApplicationRepository;
import com.secretariapay.api.repository.admission.AdmissionInvoiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmissionPaymentGuidePdfServiceTest {

    @Mock
    private AdmissionApplicationRepository applicationRepository;

    @Mock
    private AdmissionInvoiceRepository invoiceRepository;

    @Mock
    private AdmissionApplication application;

    @Mock
    private Course course;

    @Mock
    private Institution institution;

    @Test
    void shouldGeneratePdfForAuthorizedPendingInvoice() {
        UUID applicationId = UUID.randomUUID();
        when(application.getId()).thenReturn(applicationId);
        when(application.getApplicationCode()).thenReturn("IMT-ADM-TESTE-GUIA");
        when(application.getDocumentNumber()).thenReturn("TESTE-BI-GUIA-001");
        when(application.getDocumentType()).thenReturn("BI");
        when(application.getFullName()).thenReturn("CANDIDATO TESTE GUIA");
        when(application.getDesiredShift()).thenReturn("MANHA");
        when(application.getAcademicYear()).thenReturn("2026/2027");
        when(application.getDesiredCourse()).thenReturn(course);
        when(application.getInstitution()).thenReturn(institution);
        when(course.getName()).thenReturn("Arquitectura");
        when(institution.getLegalName()).thenReturn("Instituto Superior Politécnico Metropolitano de Angola");

        AdmissionInvoice invoice = new AdmissionInvoice()
                .setApplication(application)
                .setInvoiceCode("IMT-INSCR-TESTE-GUIA")
                .setAmount(new BigDecimal("6500.00"))
                .setCurrency("AOA")
                .setDueDate(LocalDate.now().plusDays(3))
                .setPaymentReference("IMT-INSCR-TESTE-GUIA")
                .setStatus(AdmissionInvoiceStatus.PENDING);

        when(applicationRepository.findByApplicationCodeIgnoreCase("IMT-ADM-TESTE-GUIA"))
                .thenReturn(Optional.of(application));
        when(invoiceRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(invoice));

        AdmissionPaymentGuidePdfService service = service(true);
        byte[] pdf = service.generate(
                "IMT-ADM-TESTE-GUIA",
                new AdmissionDto.PublicApplicationAccessRequest("TESTE-BI-GUIA-001")
        );

        assertTrue(pdf.length > 1000);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void shouldRejectGuideWhenInvoiceIsExpired() {
        UUID applicationId = UUID.randomUUID();
        when(application.getId()).thenReturn(applicationId);
        when(application.getDocumentNumber()).thenReturn("TESTE-BI-GUIA-002");
        when(applicationRepository.findByApplicationCodeIgnoreCase("IMT-ADM-TESTE-EXPIRADA"))
                .thenReturn(Optional.of(application));

        AdmissionInvoice invoice = new AdmissionInvoice()
                .setApplication(application)
                .setInvoiceCode("IMT-INSCR-TESTE-EXPIRADA")
                .setAmount(new BigDecimal("6500.00"))
                .setDueDate(LocalDate.now().minusDays(1))
                .setStatus(AdmissionInvoiceStatus.EXPIRED);
        when(invoiceRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(invoice));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service(true).generate(
                        "IMT-ADM-TESTE-EXPIRADA",
                        new AdmissionDto.PublicApplicationAccessRequest("TESTE-BI-GUIA-002")
                )
        );

        assertTrue(exception.getMessage().contains("prazo"));
    }

    private AdmissionPaymentGuidePdfService service(boolean enabled) {
        return new AdmissionPaymentGuidePdfService(
                applicationRepository,
                invoiceRepository,
                enabled,
                "Banco Angolano de Investimento",
                "OMNEN INTELENGENDA",
                "AO06 0040 0000 6014 4677 1017 1",
                "06014467710001",
                "+244 991 640 259",
                "secretaria.financeira@imetroangola.com"
        );
    }
}
