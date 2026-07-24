package com.secretariapay.api.service.admission;

import com.secretariapay.api.dto.admission.AdmissionDto;
import com.secretariapay.api.entity.admission.AdmissionApplication;
import com.secretariapay.api.entity.admission.AdmissionInvoice;
import com.secretariapay.api.entity.enums.admission.AdmissionApplicationStatus;
import com.secretariapay.api.entity.enums.admission.AdmissionInvoiceStatus;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.admission.AdmissionApplicationRepository;
import com.secretariapay.api.repository.admission.AdmissionInvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
public class AdmissionDocumentationService {

    private static final Set<AdmissionApplicationStatus> TERMINAL_STATUSES = EnumSet.of(
            AdmissionApplicationStatus.REJECTED,
            AdmissionApplicationStatus.CANCELLED,
            AdmissionApplicationStatus.EXPIRED
    );

    private final AdmissionApplicationRepository applicationRepository;
    private final AdmissionInvoiceRepository invoiceRepository;
    private final AdmissionService admissionService;

    public AdmissionDocumentationService(
            AdmissionApplicationRepository applicationRepository,
            AdmissionInvoiceRepository invoiceRepository,
            AdmissionService admissionService
    ) {
        this.applicationRepository = applicationRepository;
        this.invoiceRepository = invoiceRepository;
        this.admissionService = admissionService;
    }

    @Transactional
    public AdmissionDto.ApplicationResponse reviewDocuments(
            UUID applicationId,
            AdmissionDto.ApplicationDocumentsRequest request
    ) {
        AdmissionApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Candidatura não encontrada."));

        if (TERMINAL_STATUSES.contains(application.getStatus())) {
            throw new IllegalArgumentException(
                    "Não é possível alterar a documentação de uma candidatura encerrada."
            );
        }

        boolean documentsComplete = Boolean.TRUE.equals(request.documentsComplete());
        AdmissionInvoice invoice = invoiceRepository.findByApplicationId(applicationId).orElse(null);
        boolean paymentConfirmed = invoice != null && invoice.getStatus() == AdmissionInvoiceStatus.PAID;

        application.setDocumentsComplete(documentsComplete);
        application.setNotes(appendReviewNote(
                application.getNotes(),
                request.reviewedBy(),
                request.notes(),
                documentsComplete
        ));

        if (documentsComplete && paymentConfirmed) {
            application.setStatus(AdmissionApplicationStatus.CONFIRMED)
                    .setConfirmedAt(LocalDateTime.now());
        } else if (!documentsComplete && paymentConfirmed) {
            application.setStatus(AdmissionApplicationStatus.DOCUMENTATION_PENDING)
                    .setConfirmedAt(null);
        }

        applicationRepository.save(application);
        return admissionService.getApplication(applicationId);
    }

    private String appendReviewNote(
            String existingNotes,
            String reviewedBy,
            String reviewNotes,
            boolean documentsComplete
    ) {
        StringBuilder entry = new StringBuilder()
                .append("[Documentação - ")
                .append(documentsComplete ? "COMPLETA" : "PENDENTE")
                .append("] Validada por ")
                .append(reviewedBy.trim());

        if (reviewNotes != null && !reviewNotes.isBlank()) {
            entry.append(": ").append(reviewNotes.trim());
        }

        if (existingNotes == null || existingNotes.isBlank()) {
            return entry.toString();
        }
        return existingNotes.trim() + System.lineSeparator() + entry;
    }
}
