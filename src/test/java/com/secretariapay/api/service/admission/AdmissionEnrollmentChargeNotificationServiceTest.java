package com.secretariapay.api.service.admission;

import com.secretariapay.api.dto.enrollment.EnrollmentDto;
import com.secretariapay.api.entity.admission.AdmissionApplication;
import com.secretariapay.api.entity.admission.AdmissionOperationalNotification;
import com.secretariapay.api.entity.enums.admission.AdmissionNotificationStatus;
import com.secretariapay.api.repository.admission.AdmissionOperationalNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmissionEnrollmentChargeNotificationServiceTest {

    @Mock
    private AdmissionOperationalNotificationRepository repository;

    @Test
    void shouldQueueGuideMessageWhenEnrollmentChargeIsCreated() {
        AdmissionApplication application = new AdmissionApplication()
                .setApplicationCode("IMT-ADM-20260723-GUIA")
                .setFullName("Candidato Teste")
                .setWhatsapp("+244 923 200 777");
        EnrollmentDto.EnrollmentResponse enrollment = mock(EnrollmentDto.EnrollmentResponse.class);
        EnrollmentDto.InvoiceResponse invoice = mock(EnrollmentDto.InvoiceResponse.class);

        when(enrollment.invoice()).thenReturn(invoice);
        when(enrollment.requestCode()).thenReturn("IMT-MAT-20260723-TESTE");
        when(invoice.amount()).thenReturn(new BigDecimal("23500.00"));
        when(invoice.currency()).thenReturn("AOA");
        when(invoice.dueDate()).thenReturn(LocalDate.of(2026, 7, 26));
        when(repository.existsByIdempotencyKey(any(String.class))).thenReturn(false);
        when(repository.save(any(AdmissionOperationalNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        new AdmissionEnrollmentChargeNotificationService(repository).enqueue(application, enrollment);

        ArgumentCaptor<AdmissionOperationalNotification> captor =
                ArgumentCaptor.forClass(AdmissionOperationalNotification.class);
        verify(repository).save(captor.capture());
        AdmissionOperationalNotification notification = captor.getValue();
        assertEquals("ENROLLMENT_CHARGE_CREATED", notification.getEventType());
        assertEquals(AdmissionNotificationStatus.PENDING, notification.getStatus());
        assertEquals("+244 923 200 777", notification.getRecipient());
        assertTrue(notification.getMessageBody().contains("guia de matrícula disponível"));
        assertTrue(notification.getMessageBody().contains("23.500,00 Kz"));
        assertTrue(notification.getMessageBody().contains("26/07/2026"));
        assertTrue(notification.getMessageBody().contains("originais"));
    }
}
