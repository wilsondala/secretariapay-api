package com.secretariapay.api.service.admission;

import com.secretariapay.api.dto.admission.AdmissionDto;
import com.secretariapay.api.entity.admission.AdmissionApplication;
import com.secretariapay.api.entity.admission.AdmissionInvoice;
import com.secretariapay.api.entity.admission.AdmissionPaymentProof;
import com.secretariapay.api.entity.enums.admission.AdmissionApplicationStatus;
import com.secretariapay.api.entity.enums.admission.AdmissionInvoiceStatus;
import com.secretariapay.api.repository.admission.AdmissionApplicationRepository;
import com.secretariapay.api.repository.admission.AdmissionInvoiceRepository;
import com.secretariapay.api.repository.admission.AdmissionPaymentProofRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class AdmissionPublicPaymentService {

    private static final ZoneId LUANDA_ZONE = ZoneId.of("Africa/Luanda");

    private final AdmissionApplicationRepository applicationRepository;
    private final AdmissionInvoiceRepository invoiceRepository;
    private final AdmissionPaymentProofRepository proofRepository;
    private final AdmissionService admissionService;
    private final boolean pilotEnabled;
    private final int dueDays;
    private final String environmentLabel;
    private final String provider;
    private final String bankName;
    private final String accountHolder;
    private final String iban;
    private final String accountNumber;
    private final String multicaixaReference;
    private final String mobileMoneyInfo;
    private final String supportWhatsapp;
    private final String supportEmail;

    public AdmissionPublicPaymentService(
            AdmissionApplicationRepository applicationRepository,
            AdmissionInvoiceRepository invoiceRepository,
            AdmissionPaymentProofRepository proofRepository,
            AdmissionService admissionService,
            @Value("${secretariapay.admissions.public-payment-pilot-enabled:false}") boolean pilotEnabled,
            @Value("${secretariapay.admissions.public-payment-due-days:3}") int dueDays,
            @Value("${secretariapay.admissions.public-payment-environment-label:HOMOLOGAÇÃO LOCAL}") String environmentLabel,
            @Value("${secretariapay.admissions.public-payment-provider:BAI_TRANSFERENCIA_BANCARIA_PILOTO}") String provider,
            @Value("${secretariapay.payment.bank-name:Banco Angolano de Investimento}") String bankName,
            @Value("${secretariapay.payment.account-holder:OMNEN INTELENGENDA}") String accountHolder,
            @Value("${secretariapay.payment.iban:AO06 0040 0000 6014 4677 1017 1}") String iban,
            @Value("${secretariapay.payment.account-number:06014467710001}") String accountNumber,
            @Value("${secretariapay.payment.multicaixa-reference:Multicaixa Express / transferência bancária para a conta AKZ indicada}") String multicaixaReference,
            @Value("${secretariapay.payment.mobile-money-info:Unitel Money/Afrimoney quando autorizado pela instituição}") String mobileMoneyInfo,
            @Value("${secretariapay.institution.whatsapp:+244 991 640 259}") String supportWhatsapp,
            @Value("${secretariapay.institution.financial-email:secretaria.financeira@imetroangola.com}") String supportEmail
    ) {
        this.applicationRepository = applicationRepository;
        this.invoiceRepository = invoiceRepository;
        this.proofRepository = proofRepository;
        this.admissionService = admissionService;
        this.pilotEnabled = pilotEnabled;
        this.dueDays = Math.max(1, dueDays);
        this.environmentLabel = clean(environmentLabel, "HOMOLOGAÇÃO LOCAL");
        this.provider = clean(provider, "BAI_TRANSFERENCIA_BANCARIA_PILOTO");
        this.bankName = clean(bankName, "Banco Angolano de Investimento");
        this.accountHolder = clean(accountHolder, "OMNEN INTELENGENDA");
        this.iban = clean(iban, "AO06 0040 0000 6014 4677 1017 1");
        this.accountNumber = clean(accountNumber, "06014467710001");
        this.multicaixaReference = clean(multicaixaReference, "Multicaixa Express / transferência bancária para a conta AKZ indicada");
        this.mobileMoneyInfo = clean(mobileMoneyInfo, "Unitel Money/Afrimoney quando autorizado pela instituição");
        this.supportWhatsapp = clean(supportWhatsapp, "+244 991 640 259");
        this.supportEmail = clean(supportEmail, "secretaria.financeira@imetroangola.com");
    }

    @Transactional
    public AdmissionDto.PublicPaymentResponse getStatus(
            String applicationCode,
            AdmissionDto.PublicApplicationAccessRequest request
    ) {
        AdmissionApplication application = findAuthorizedApplication(applicationCode, request.documentNumber());
        AdmissionInvoice invoice = invoiceRepository.findByApplicationId(application.getId()).orElse(null);
        expireIfOverdue(application, invoice);
        return toResponse(application);
    }

    @Transactional
    public AdmissionDto.PublicPaymentResponse issueOrGetInvoice(
            String applicationCode,
            AdmissionDto.PublicApplicationAccessRequest request
    ) {
        ensurePilotEnabled();
        AdmissionApplication application = findAuthorizedApplication(applicationCode, request.documentNumber());
        ensureSubmitted(application);

        AdmissionInvoice invoice = invoiceRepository.findByApplicationId(application.getId()).orElse(null);
        expireIfOverdue(application, invoice);
        ensureNotWithdrawn(application, invoice);

        if (invoice == null) {
            if (application.getCampaign() == null || application.getCampaign().getRegistrationFee() == null) {
                throw new IllegalArgumentException("A campanha não possui taxa oficial de inscrição configurada.");
            }

            LocalDate dueDate = LocalDate.now(LUANDA_ZONE).plusDays(dueDays);
            admissionService.issueInvoice(
                    application.getId(),
                    new AdmissionDto.InvoiceRequest(
                            application.getCampaign().getRegistrationFee(),
                            dueDate,
                            null,
                            provider
                    )
            );
            invoice = invoiceRepository.findByApplicationId(application.getId())
                    .orElseThrow(() -> new IllegalStateException("A cobrança foi emitida, mas não pôde ser consultada."));
        }

        if (invoice.getPaymentReference() == null || invoice.getPaymentReference().isBlank()) {
            invoice.setPaymentReference(invoice.getInvoiceCode());
        }
        if (invoice.getProvider() == null || invoice.getProvider().isBlank()) {
            invoice.setProvider(provider);
        }
        invoiceRepository.save(invoice);

        return toResponse(applicationRepository.findByApplicationCodeIgnoreCase(application.getApplicationCode())
                .orElse(application));
    }

    @Transactional
    public AdmissionDto.PublicPaymentResponse submitPaymentProof(
            String applicationCode,
            AdmissionDto.PublicPaymentProofRequest request
    ) {
        ensurePilotEnabled();
        AdmissionApplication application = findAuthorizedApplication(applicationCode, request.documentNumber());
        AdmissionInvoice invoice = invoiceRepository.findByApplicationId(application.getId())
                .orElseThrow(() -> new IllegalArgumentException("A cobrança da inscrição ainda não foi emitida."));

        expireIfOverdue(application, invoice);
        ensureNotWithdrawn(application, invoice);
        if (invoice.getStatus() != AdmissionInvoiceStatus.PENDING) {
            throw new IllegalArgumentException("O comprovativo não pode ser enviado no estado atual da cobrança.");
        }

        admissionService.submitPaymentProof(
                invoice.getId(),
                new AdmissionDto.PaymentProofRequest(
                        request.fileUrl(),
                        request.fileName(),
                        request.mimeType()
                )
        );

        return toResponse(applicationRepository.findByApplicationCodeIgnoreCase(application.getApplicationCode())
                .orElse(application));
    }

    private AdmissionApplication findAuthorizedApplication(String applicationCode, String documentNumber) {
        String normalizedCode = clean(applicationCode, null);
        String normalizedDocument = clean(documentNumber, null);
        if (normalizedCode == null || normalizedDocument == null) {
            throw new IllegalArgumentException("Informe o código da candidatura e o número do documento.");
        }

        AdmissionApplication application = applicationRepository.findByApplicationCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new IllegalArgumentException("Candidatura não encontrada ou documento inválido."));
        if (application.getDocumentNumber() == null
                || !application.getDocumentNumber().trim().equalsIgnoreCase(normalizedDocument)) {
            throw new IllegalArgumentException("Candidatura não encontrada ou documento inválido.");
        }
        return application;
    }

    private void ensureSubmitted(AdmissionApplication application) {
        if (!Boolean.TRUE.equals(application.getTermsAccepted()) || application.getSubmittedAt() == null) {
            throw new IllegalArgumentException("A cobrança só pode ser preparada para uma candidatura submetida.");
        }
    }

    private void ensurePilotEnabled() {
        if (!pilotEnabled) {
            throw new IllegalArgumentException("O pagamento público provisório está desativado neste ambiente.");
        }
    }

    private void ensureNotWithdrawn(AdmissionApplication application, AdmissionInvoice invoice) {
        if (application.getStatus() == AdmissionApplicationStatus.EXPIRED
                || (invoice != null && invoice.getStatus() == AdmissionInvoiceStatus.EXPIRED)) {
            throw new IllegalArgumentException("O prazo de pagamento terminou. A candidatura foi marcada como desistência por falta de pagamento.");
        }
    }

    private void expireIfOverdue(AdmissionApplication application, AdmissionInvoice invoice) {
        if (invoice == null
                || invoice.getStatus() != AdmissionInvoiceStatus.PENDING
                || invoice.getDueDate() == null
                || !invoice.getDueDate().isBefore(LocalDate.now(LUANDA_ZONE))) {
            return;
        }

        invoice.setStatus(AdmissionInvoiceStatus.EXPIRED);
        application.setStatus(AdmissionApplicationStatus.EXPIRED);
        application.setNotes(appendNote(
                application.getNotes(),
                "Desistência automática por falta de pagamento. A guia "
                        + invoice.getInvoiceCode()
                        + " venceu em "
                        + invoice.getDueDate()
                        + "."
        ));
        invoiceRepository.save(invoice);
        applicationRepository.save(application);
    }

    private String appendNote(String current, String note) {
        String entry = "[" + LocalDateTime.now(LUANDA_ZONE) + "] " + note;
        if (current == null || current.isBlank()) return entry;
        return current.trim() + System.lineSeparator() + entry;
    }

    private AdmissionDto.PublicPaymentResponse toResponse(AdmissionApplication application) {
        AdmissionInvoice invoice = invoiceRepository.findByApplicationId(application.getId()).orElse(null);
        AdmissionPaymentProof proof = invoice == null
                ? null
                : proofRepository.findFirstByInvoiceIdOrderByCreatedAtDesc(invoice.getId()).orElse(null);

        return new AdmissionDto.PublicPaymentResponse(
                application.getApplicationCode(),
                application.getFullName(),
                application.getDesiredCourse() == null ? null : application.getDesiredCourse().getName(),
                application.getDesiredShift(),
                application.getAcademicYear(),
                application.getStatus(),
                toInvoiceResponse(invoice),
                toProofResponse(proof),
                paymentInstructions()
        );
    }

    private AdmissionDto.PublicPaymentInstructionsResponse paymentInstructions() {
        if (!pilotEnabled) {
            return new AdmissionDto.PublicPaymentInstructionsResponse(
                    false,
                    true,
                    environmentLabel,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    supportWhatsapp,
                    supportEmail,
                    "Fluxo provisório desativado. Nenhuma instrução bancária foi disponibilizada."
            );
        }

        return new AdmissionDto.PublicPaymentInstructionsResponse(
                true,
                true,
                environmentLabel,
                bankName,
                accountHolder,
                iban,
                accountNumber,
                multicaixaReference,
                mobileMoneyInfo,
                supportWhatsapp,
                supportEmail,
                "Dados provisórios de homologação. Não utilizar em produção sem autorização institucional explícita."
        );
    }

    private AdmissionDto.InvoiceResponse toInvoiceResponse(AdmissionInvoice invoice) {
        if (invoice == null) return null;
        return new AdmissionDto.InvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceCode(),
                invoice.getAmount(),
                invoice.getCurrency(),
                invoice.getDueDate(),
                invoice.getStatus(),
                invoice.getPaymentMethod(),
                invoice.getPaymentReference(),
                invoice.getProvider(),
                invoice.getExternalTransactionId(),
                invoice.getPaidAt(),
                invoice.getCreatedAt(),
                invoice.getUpdatedAt()
        );
    }

    private AdmissionDto.PaymentProofResponse toProofResponse(AdmissionPaymentProof proof) {
        if (proof == null) return null;
        return new AdmissionDto.PaymentProofResponse(
                proof.getId(),
                proof.getInvoice() == null ? null : proof.getInvoice().getId(),
                proof.getFileUrl(),
                proof.getFileName(),
                proof.getMimeType(),
                proof.getStatus(),
                proof.getReviewedBy(),
                proof.getReviewNote(),
                proof.getSubmittedAt(),
                proof.getReviewedAt(),
                proof.getCreatedAt(),
                proof.getUpdatedAt()
        );
    }

    private String clean(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.trim();
    }
}
