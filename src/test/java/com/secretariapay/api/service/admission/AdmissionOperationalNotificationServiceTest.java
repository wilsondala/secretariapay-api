package com.secretariapay.api.service.admission;

import com.secretariapay.api.dto.whatsapp.WhatsAppCloudSendResult;
import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.admission.AdmissionApplication;
import com.secretariapay.api.entity.admission.AdmissionOperationalNotification;
import com.secretariapay.api.entity.enums.admission.AdmissionNotificationStatus;
import com.secretariapay.api.repository.admission.AdmissionOperationalNotificationRepository;
import com.secretariapay.api.service.whatsapp.WhatsAppCloudApiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmissionOperationalNotificationServiceTest {

    @Mock
    private AdmissionOperationalNotificationRepository repository;

    @Mock
    private WhatsAppCloudApiClient whatsAppClient;

    @Test
    void shouldCreateOneMaskedOperationalAlertForApplication() {
        Course course = org.mockito.Mockito.mock(Course.class);
        when(course.getName()).thenReturn("Arquitectura");

        AdmissionApplication application = new AdmissionApplication()
                .setApplicationCode("IMT-ADM-20260723-ALERTA")
                .setFullName("Candidato Teste")
                .setDocumentNumber("123456789LA001")
                .setDesiredCourse(course)
                .setDesiredShift("MANHA")
                .setAcademicYear("2026/2027")
                .setWhatsapp("+244 923 000 200");

        when(repository.existsByIdempotencyKey(any(String.class))).thenReturn(false);
        when(repository.save(any(AdmissionOperationalNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service(false).enqueueApplicationSubmitted(application);

        ArgumentCaptor<AdmissionOperationalNotification> captor =
                ArgumentCaptor.forClass(AdmissionOperationalNotification.class);
        verify(repository).save(captor.capture());

        AdmissionOperationalNotification notification = captor.getValue();
        assertEquals(AdmissionNotificationStatus.PENDING, notification.getStatus());
        assertEquals("+244 991 640 259", notification.getRecipient());
        assertTrue(notification.getMessageBody().contains("Nova candidatura recebida"));
        assertTrue(notification.getMessageBody().contains("IMT-ADM-20260723-ALERTA"));
        assertTrue(notification.getMessageBody().contains("Arquitectura"));
        assertTrue(notification.getMessageBody().contains("**********A001"));
        assertTrue(notification.getMessageBody().contains("http://localhost:5173/admissions"));
    }

    @Test
    void shouldRequestOfficialEnrollmentDocumentsFromCandidateWhatsapp() {
        Course course = org.mockito.Mockito.mock(Course.class);
        when(course.getName()).thenReturn("Arquitectura");

        AdmissionApplication application = new AdmissionApplication()
                .setApplicationCode("IMT-ADM-20260723-DOCUMENTOS")
                .setFullName("Candidato Teste")
                .setDesiredCourse(course)
                .setDesiredShift("MANHA")
                .setAcademicYear("2026/2027")
                .setWhatsapp("+244 923 200 777");

        when(repository.existsByIdempotencyKey(any(String.class))).thenReturn(false);
        when(repository.save(any(AdmissionOperationalNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service(false).enqueueEnrollmentDocumentsRequested(application);

        ArgumentCaptor<AdmissionOperationalNotification> captor =
                ArgumentCaptor.forClass(AdmissionOperationalNotification.class);
        verify(repository).save(captor.capture());

        AdmissionOperationalNotification notification = captor.getValue();
        assertEquals("ENROLLMENT_DOCUMENTS_REQUESTED", notification.getEventType());
        assertEquals(AdmissionNotificationStatus.PENDING, notification.getStatus());
        assertEquals("+244 923 200 777", notification.getRecipient());
        assertTrue(notification.getMessageBody().contains("Inscrição confirmada"));
        assertTrue(notification.getMessageBody().contains("2 fotografias do tipo passe"));
        assertTrue(notification.getMessageBody().contains("certificado de habilitações"));
        assertTrue(notification.getMessageBody().contains("Bilhete de Identidade"));
        assertTrue(notification.getMessageBody().contains("Ministério da Educação"));
        assertTrue(notification.getMessageBody().contains("Idade mínima de 18 anos"));
        assertTrue(notification.getMessageBody().contains("23.500,00 Kz"));
    }

    @Test
    void shouldAuditMissingCandidateWhatsappWithoutDispatching() {
        AdmissionApplication application = new AdmissionApplication()
                .setApplicationCode("IMT-ADM-20260723-SEM-CONTACTO")
                .setFullName("Candidato sem contacto");

        when(repository.existsByIdempotencyKey(any(String.class))).thenReturn(false);
        when(repository.save(any(AdmissionOperationalNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service(false).enqueueEnrollmentDocumentsRequested(application);

        ArgumentCaptor<AdmissionOperationalNotification> captor =
                ArgumentCaptor.forClass(AdmissionOperationalNotification.class);
        verify(repository).save(captor.capture());

        AdmissionOperationalNotification notification = captor.getValue();
        assertEquals(AdmissionNotificationStatus.EXHAUSTED, notification.getStatus());
        assertEquals("SEM_CONTACTO", notification.getRecipient());
        assertNotNull(notification.getLastError());
        assertTrue(notification.getLastError().contains("não possui WhatsApp nem telefone"));
        verify(whatsAppClient, never()).sendText(any(String.class), any(String.class));
    }

    @Test
    void shouldNotDuplicateAlertWithSameIdempotencyKey() {
        AdmissionApplication application = new AdmissionApplication()
                .setApplicationCode("IMT-ADM-20260723-DUPLICADA");

        when(repository.existsByIdempotencyKey(any(String.class))).thenReturn(true);

        service(false).enqueueApplicationSubmitted(application);

        verify(repository, never()).save(any(AdmissionOperationalNotification.class));
    }

    @Test
    void shouldSendPendingAlertAndStoreProviderMessageId() {
        AdmissionOperationalNotification notification = new AdmissionOperationalNotification()
                .setRecipient("+244 991 640 259")
                .setMessageBody("Nova candidatura recebida")
                .setStatus(AdmissionNotificationStatus.PENDING)
                .setAttempts(0)
                .setNextAttemptAt(LocalDateTime.now().minusMinutes(1));

        when(repository.findTop20ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                any(),
                any(LocalDateTime.class)
        )).thenReturn(List.of(notification));
        when(whatsAppClient.sendText(
                eq("+244 991 640 259"),
                eq("Nova candidatura recebida")
        )).thenReturn(WhatsAppCloudSendResult.sent("wamid.TESTE", 200));
        when(repository.save(notification)).thenReturn(notification);

        service(true).dispatchPending();

        assertEquals(AdmissionNotificationStatus.SENT, notification.getStatus());
        assertEquals(1, notification.getAttempts());
        assertEquals("wamid.TESTE", notification.getProviderMessageId());
        verify(repository).save(notification);
    }

    private AdmissionOperationalNotificationService service(boolean enabled) {
        return new AdmissionOperationalNotificationService(
                repository,
                whatsAppClient,
                enabled,
                "+244 991 640 259",
                "http://localhost:5173/admissions",
                5
        );
    }
}
