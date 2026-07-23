package com.secretariapay.api.service.admission;

import com.secretariapay.api.entity.admission.AdmissionApplication;
import com.secretariapay.api.entity.admission.AdmissionInvoice;
import com.secretariapay.api.entity.enums.admission.AdmissionApplicationStatus;
import com.secretariapay.api.entity.enums.admission.AdmissionInvoiceStatus;
import com.secretariapay.api.repository.admission.AdmissionApplicationRepository;
import com.secretariapay.api.repository.admission.AdmissionInvoiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmissionPaymentExpiryServiceTest {

    @Mock
    private AdmissionInvoiceRepository invoiceRepository;

    @Mock
    private AdmissionApplicationRepository applicationRepository;

    @Test
    void shouldDoNothingWhenAutomaticExpiryIsDisabled() {
        AdmissionPaymentExpiryService service = new AdmissionPaymentExpiryService(
                invoiceRepository,
                applicationRepository,
                false
        );

        assertEquals(0, service.expireOverduePayments());
        verify(invoiceRepository, never()).findAllByStatusAndDueDateBefore(
                AdmissionInvoiceStatus.PENDING,
                LocalDate.now()
        );
    }

    @Test
    void shouldMarkApplicationAsWithdrawnWhenPendingInvoiceIsOverdue() {
        AdmissionApplication application = new AdmissionApplication()
                .setApplicationCode("IMT-ADM-TESTE-PRAZO")
                .setStatus(AdmissionApplicationStatus.AWAITING_PAYMENT)
                .setNotes("Candidatura submetida.");

        AdmissionInvoice invoice = new AdmissionInvoice()
                .setApplication(application)
                .setInvoiceCode("IMT-INSCR-TESTE-PRAZO")
                .setAmount(new BigDecimal("6500.00"))
                .setDueDate(LocalDate.now().minusDays(1))
                .setStatus(AdmissionInvoiceStatus.PENDING);

        when(invoiceRepository.findAllByStatusAndDueDateBefore(
                AdmissionInvoiceStatus.PENDING,
                LocalDate.now()
        )).thenReturn(List.of(invoice));

        AdmissionPaymentExpiryService service = new AdmissionPaymentExpiryService(
                invoiceRepository,
                applicationRepository,
                true
        );

        assertEquals(1, service.expireOverduePayments());
        assertEquals(AdmissionInvoiceStatus.EXPIRED, invoice.getStatus());
        assertEquals(AdmissionApplicationStatus.EXPIRED, application.getStatus());
        assertTrue(application.getNotes().contains("Desistência automática por falta de pagamento"));
        verify(invoiceRepository).save(invoice);
        verify(applicationRepository).save(application);
    }

    @Test
    void shouldNotExpireProofAlreadyUnderReview() {
        when(invoiceRepository.findAllByStatusAndDueDateBefore(
                AdmissionInvoiceStatus.PENDING,
                LocalDate.now()
        )).thenReturn(List.of());

        AdmissionPaymentExpiryService service = new AdmissionPaymentExpiryService(
                invoiceRepository,
                applicationRepository,
                true
        );

        assertEquals(0, service.expireOverduePayments());
        verify(applicationRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
