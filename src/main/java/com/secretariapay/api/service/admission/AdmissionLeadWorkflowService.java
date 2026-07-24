package com.secretariapay.api.service.admission;

import com.secretariapay.api.dto.admission.AdmissionDto;
import com.secretariapay.api.entity.admission.AdmissionApplication;
import com.secretariapay.api.entity.admission.AdmissionLead;
import com.secretariapay.api.entity.enums.admission.AdmissionLeadStatus;
import com.secretariapay.api.entity.enums.admission.AdmissionSourceChannel;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.admission.AdmissionApplicationRepository;
import com.secretariapay.api.repository.admission.AdmissionLeadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class AdmissionLeadWorkflowService {

    private final AdmissionService admissionService;
    private final AdmissionLeadRepository leadRepository;
    private final AdmissionApplicationRepository applicationRepository;

    public AdmissionLeadWorkflowService(
            AdmissionService admissionService,
            AdmissionLeadRepository leadRepository,
            AdmissionApplicationRepository applicationRepository
    ) {
        this.admissionService = admissionService;
        this.leadRepository = leadRepository;
        this.applicationRepository = applicationRepository;
    }

    @Transactional
    public AdmissionDto.LeadResponse updateLeadStatus(
            UUID leadId,
            AdmissionLeadStatus status,
            String notes
    ) {
        AdmissionLead lead = findLead(leadId);
        String updatedNotes = appendHistory(lead.getNotes(), notes);
        return admissionService.updateLeadStatus(leadId, status, updatedNotes);
    }

    @Transactional
    public AdmissionDto.ApplicationResponse createApplication(AdmissionDto.ApplicationRequest request) {
        if (request.leadId() == null) {
            return admissionService.createApplication(request);
        }

        AdmissionLead lead = findLead(request.leadId());
        AdmissionSourceChannel sourceChannel = resolveSourceChannel(lead.getLeadSource());

        AdmissionDto.ApplicationResponse created = admissionService.createApplication(
                request,
                AdmissionSourceChannel.INTERNAL
        );

        if (sourceChannel != AdmissionSourceChannel.INTERNAL) {
            AdmissionApplication application = applicationRepository.findById(created.id())
                    .orElseThrow(() -> new NotFoundException("Candidatura não encontrada após a conversão do lead."));
            application.setSourceChannel(sourceChannel);
            applicationRepository.save(application);
        }

        return admissionService.getApplication(created.id());
    }

    static AdmissionSourceChannel resolveSourceChannel(String leadSource) {
        if (leadSource == null || leadSource.isBlank()) {
            return AdmissionSourceChannel.INTERNAL;
        }

        return switch (leadSource.trim().toUpperCase(Locale.ROOT)) {
            case "WHATSAPP" -> AdmissionSourceChannel.WHATSAPP;
            case "FORM", "WEBSITE", "SITE" -> AdmissionSourceChannel.FORM;
            case "IMPORT", "IMPORTACAO", "IMPORTAÇÃO" -> AdmissionSourceChannel.IMPORT;
            default -> AdmissionSourceChannel.INTERNAL;
        };
    }

    static String appendHistory(String existingNotes, String newNote) {
        String existing = normalize(existingNotes);
        String incoming = normalize(newNote);

        if (incoming == null) {
            return existing;
        }
        if (existing == null) {
            return incoming;
        }
        if (existing.equalsIgnoreCase(incoming) || existing.endsWith("\n" + incoming)) {
            return existing;
        }
        return existing + System.lineSeparator() + incoming;
    }

    private AdmissionLead findLead(UUID leadId) {
        return leadRepository.findById(leadId)
                .orElseThrow(() -> new NotFoundException("Lead não encontrado."));
    }

    private static String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
