package com.secretariapay.api.service.admission;

import com.secretariapay.api.dto.admission.AdmissionDto;
import com.secretariapay.api.entity.admission.AdmissionApplication;
import com.secretariapay.api.entity.admission.AdmissionLead;
import com.secretariapay.api.entity.enums.admission.AdmissionLeadStatus;
import com.secretariapay.api.entity.enums.admission.AdmissionSourceChannel;
import com.secretariapay.api.repository.admission.AdmissionApplicationRepository;
import com.secretariapay.api.repository.admission.AdmissionLeadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmissionLeadWorkflowServiceTest {

    @Mock
    private AdmissionService admissionService;

    @Mock
    private AdmissionLeadRepository leadRepository;

    @Mock
    private AdmissionApplicationRepository applicationRepository;

    @InjectMocks
    private AdmissionLeadWorkflowService workflowService;

    @Test
    void shouldPreserveWhatsappSourceWhenInternalTeamConvertsLead() {
        UUID leadId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        AdmissionDto.ApplicationRequest request = applicationRequest(leadId);
        AdmissionLead lead = new AdmissionLead().setLeadSource("WHATSAPP");
        AdmissionApplication application = new AdmissionApplication();
        AdmissionDto.ApplicationResponse created = mock(AdmissionDto.ApplicationResponse.class);
        AdmissionDto.ApplicationResponse refreshed = mock(AdmissionDto.ApplicationResponse.class);

        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(admissionService.createApplication(request, AdmissionSourceChannel.INTERNAL)).thenReturn(created);
        when(created.id()).thenReturn(applicationId);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(admissionService.getApplication(applicationId)).thenReturn(refreshed);

        AdmissionDto.ApplicationResponse result = workflowService.createApplication(request);

        assertSame(refreshed, result);
        assertEquals(AdmissionSourceChannel.WHATSAPP, application.getSourceChannel());
        verify(applicationRepository).save(application);
    }

    @Test
    void shouldAppendContactNoteWithoutLosingOriginalLeadObservation() {
        UUID leadId = UUID.randomUUID();
        AdmissionLead lead = new AdmissionLead().setNotes("Teste local do fluxo oficial de captação e conversão.");
        AdmissionDto.LeadResponse expected = mock(AdmissionDto.LeadResponse.class);
        String contactNote = "Contacto registado pela equipa de captação.";
        String expectedHistory = lead.getNotes() + System.lineSeparator() + contactNote;

        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(admissionService.updateLeadStatus(
                leadId,
                AdmissionLeadStatus.CONTACTED,
                expectedHistory
        )).thenReturn(expected);

        AdmissionDto.LeadResponse result = workflowService.updateLeadStatus(
                leadId,
                AdmissionLeadStatus.CONTACTED,
                contactNote
        );

        assertSame(expected, result);
        verify(admissionService).updateLeadStatus(
                leadId,
                AdmissionLeadStatus.CONTACTED,
                expectedHistory
        );
    }

    @Test
    void shouldResolveKnownLeadSources() {
        assertEquals(
                AdmissionSourceChannel.WHATSAPP,
                AdmissionLeadWorkflowService.resolveSourceChannel("whatsapp")
        );
        assertEquals(
                AdmissionSourceChannel.FORM,
                AdmissionLeadWorkflowService.resolveSourceChannel("website")
        );
        assertEquals(
                AdmissionSourceChannel.INTERNAL,
                AdmissionLeadWorkflowService.resolveSourceChannel("campaign")
        );
    }

    private AdmissionDto.ApplicationRequest applicationRequest(UUID leadId) {
        return new AdmissionDto.ApplicationRequest(
                UUID.randomUUID(),
                leadId,
                UUID.randomUUID(),
                "MANHA",
                "2026/2027",
                "Candidato de teste",
                "BI",
                "TESTE-LEAD-IMETRO-002",
                null,
                "+244 923 000 102",
                "+244 923 000 102",
                "wilson.captacao.2026@example.com",
                "Escola de Teste",
                "Luanda",
                "Kilamba Kiaxi",
                false,
                true,
                "Teste local controlado."
        );
    }
}
