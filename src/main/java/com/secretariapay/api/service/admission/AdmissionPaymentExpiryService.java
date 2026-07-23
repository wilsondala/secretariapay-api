package com.secretariapay.api.service.admission;

import com.secretariapay.api.entity.admission.AdmissionApplication;
import com.secretariapay.api.entity.admission.AdmissionInvoice;
import com.secretariapay.api.entity.enums.admission.AdmissionApplicationStatus;
import com.secretariapay.api.entity.enums.admission.AdmissionInvoiceStatus;
import com.secretariapay.api.repository.admission.AdmissionApplicationRepository;
import com.secretariapay.api.repository.admission.AdmissionInvoiceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AdmissionPaymentExpiryService {

    private static final ZoneId LUANDA_ZONE = ZoneId.of("Africa/Luanda");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AdmissionInvoiceRepository invoiceRepository;
    private final AdmissionApplicationRepository applicationRepository;
    private final boolean enabled;

    public AdmissionPaymentExpiryService(
            AdmissionInvoiceRepository invoiceRepository,
            AdmissionApplicationRepository applicationRepository,
            @Value("${secretariapay.admissions.payment-expiry-enabled:false}") boolean enabled
    ) {
        this.invoiceRepository = invoiceRepository;
        this.applicationRepository = applicationRepository;
        this.enabled = enabled;
    }

    @Scheduled(
            cron = "${secretariapay.admissions.payment-expiry-cron:0 15 * * * *}",
            zone = "Africa/Luanda"
    )
    public void runScheduledExpiry() {
        if (enabled) {
            expireOverduePayments();
        }
    }

    @Transactional
    public int expireOverduePayments() {
        if (!enabled) return 0;

        LocalDate today = LocalDate.now(LUANDA_ZONE);
        List<AdmissionInvoice> overdueInvoices = invoiceRepository
                .findAllByStatusAndDueDateBefore(AdmissionInvoiceStatus.PENDING, today);

        int expired = 0;
        for (AdmissionInvoice invoice : overdueInvoices) {
            AdmissionApplication application = invoice.getApplication();
            if (application == null || isFinalized(application.getStatus())) continue;

            invoice.setStatus(AdmissionInvoiceStatus.EXPIRED);
            application.setStatus(AdmissionApplicationStatus.EXPIRED);
            application.setNotes(appendNote(
                    application.getNotes(),
                    "Desistência automática por falta de pagamento. A guia "
                            + invoice.getInvoiceCode()
                            + " venceu em "
                            + invoice.getDueDate().format(DATE_FORMAT)
                            + "."
            ));

            invoiceRepository.save(invoice);
            applicationRepository.save(application);
            expired++;
        }
        return expired;
    }

    private boolean isFinalized(AdmissionApplicationStatus status) {
        return status == AdmissionApplicationStatus.PAID
                || status == AdmissionApplicationStatus.CONFIRMED
                || status == AdmissionApplicationStatus.REJECTED
                || status == AdmissionApplicationStatus.CANCELLED
                || status == AdmissionApplicationStatus.EXPIRED;
    }

    private String appendNote(String current, String note) {
        String timestamp = LocalDateTime.now(LUANDA_ZONE).toString();
        String entry = "[" + timestamp + "] " + note;
        if (current == null || current.isBlank()) return entry;
        return current.trim() + System.lineSeparator() + entry;
    }
}
