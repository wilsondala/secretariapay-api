package com.secretariapay.api.service.admission;

import com.secretariapay.api.dto.admission.AdmissionDto;
import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.academic.Institution;
import com.secretariapay.api.entity.admission.AdmissionApplication;
import com.secretariapay.api.entity.admission.AdmissionInvoice;
import com.secretariapay.api.entity.admission.AdmissionLead;
import com.secretariapay.api.entity.admission.AdmissionPaymentProof;
import com.secretariapay.api.entity.enums.admission.AdmissionApplicationStatus;
import com.secretariapay.api.entity.enums.admission.AdmissionInvoiceStatus;
import com.secretariapay.api.entity.enums.admission.AdmissionLeadStatus;
import com.secretariapay.api.entity.enums.admission.AdmissionPaymentProofStatus;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.academic.CourseRepository;
import com.secretariapay.api.repository.academic.InstitutionRepository;
import com.secretariapay.api.repository.admission.AdmissionApplicationRepository;
import com.secretariapay.api.repository.admission.AdmissionInvoiceRepository;
import com.secretariapay.api.repository.admission.AdmissionLeadRepository;
import com.secretariapay.api.repository.admission.AdmissionPaymentProofRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AdmissionService {

    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final List<AdmissionApplicationStatus> TERMINAL_APPLICATION_STATUSES = List.of(
            AdmissionApplicationStatus.REJECTED,
            AdmissionApplicationStatus.CANCELLED,
            AdmissionApplicationStatus.EXPIRED
    );

    private final AdmissionLeadRepository leadRepository;
    private final AdmissionApplicationRepository applicationRepository;
    private final AdmissionInvoiceRepository invoiceRepository;
    private final AdmissionPaymentProofRepository proofRepository;
    private final InstitutionRepository institutionRepository;
    private final CourseRepository courseRepository;

    public AdmissionService(
            AdmissionLeadRepository leadRepository,
            AdmissionApplicationRepository applicationRepository,
            AdmissionInvoiceRepository invoiceRepository,
            AdmissionPaymentProofRepository proofRepository,
            InstitutionRepository institutionRepository,
            CourseRepository courseRepository
    ) {
        this.leadRepository = leadRepository;
        this.applicationRepository = applicationRepository;
        this.invoiceRepository = invoiceRepository;
        this.proofRepository = proofRepository;
        this.institutionRepository = institutionRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional
    public AdmissionDto.LeadResponse createLead(AdmissionDto.LeadRequest request) {
        Institution institution = findInstitution(request.institutionId());
        Course course = request.desiredCourseId() == null ? null : findCourse(request.desiredCourseId(), institution.getId());

        AdmissionLead existing = findDuplicateLead(request);
        if (existing != null) {
            applyLeadData(existing, request, course);
            existing.setLastContactAt(LocalDateTime.now());
            return toLeadResponse(leadRepository.save(existing));
        }

        AdmissionLead lead = new AdmissionLead()
                .setInstitution(institution)
                .setDesiredCourse(course)
                .setStatus(AdmissionLeadStatus.NEW);
        applyLeadData(lead, request, course);
        return toLeadResponse(leadRepository.save(lead));
    }

    @Transactional(readOnly = true)
    public List<AdmissionDto.LeadResponse> listLeads(UUID institutionId, AdmissionLeadStatus status) {
        List<AdmissionLead> leads = status == null
                ? leadRepository.findByInstitutionIdOrderByCreatedAtDesc(institutionId)
                : leadRepository.findByInstitutionIdAndStatusOrderByCreatedAtDesc(institutionId, status);
        return leads.stream().map(this::toLeadResponse).toList();
    }

    @Transactional
    public AdmissionDto.LeadResponse updateLeadStatus(UUID leadId, AdmissionLeadStatus status, String notes) {
        AdmissionLead lead = findLead(leadId);
        lead.setStatus(status).setLastContactAt(LocalDateTime.now());
        if (notes != null && !notes.isBlank()) lead.setNotes(notes.trim());
        return toLeadResponse(leadRepository.save(lead));
    }

    @Transactional
    public AdmissionDto.ApplicationResponse createApplication(AdmissionDto.ApplicationRequest request) {
        Institution institution = findInstitution(request.institutionId());
        Course course = findCourse(request.desiredCourseId(), institution.getId());

        if (applicationRepository.existsByInstitutionIdAndDocumentNumberIgnoreCaseAndStatusNotIn(
                institution.getId(), request.documentNumber().trim(), TERMINAL_APPLICATION_STATUSES)) {
            throw new IllegalArgumentException("Já existe uma candidatura ativa para este documento de identificação.");
        }

        AdmissionLead lead = request.leadId() == null ? null : findLead(request.leadId());
        if (lead != null && !lead.getInstitution().getId().equals(institution.getId())) {
            throw new IllegalArgumentException("O lead não pertence à instituição informada.");
        }

        AdmissionApplication application = new AdmissionApplication()
                .setApplicationCode(generateApplicationCode())
                .setInstitution(institution)
                .setLead(lead)
                .setDesiredCourse(course)
                .setDesiredShift(normalizeShift(request.desiredShift()))
                .setAcademicYear(request.academicYear().trim())
                .setFullName(request.fullName().trim())
                .setDocumentType(trimToNull(request.documentType()))
                .setDocumentNumber(request.documentNumber().trim())
                .setBirthDate(request.birthDate())
                .setPhone(trimToNull(request.phone()))
                .setWhatsapp(trimToNull(request.whatsapp()))
                .setEmail(trimToNull(request.email()))
                .setPreviousSchool(trimToNull(request.previousSchool()))
                .setProvince(trimToNull(request.province()))
                .setMunicipality(trimToNull(request.municipality()))
                .setDocumentsComplete(Boolean.TRUE.equals(request.documentsComplete()))
                .setTermsAccepted(Boolean.TRUE.equals(request.termsAccepted()))
                .setNotes(trimToNull(request.notes()))
                .setStatus(AdmissionApplicationStatus.DRAFT);

        if (Boolean.TRUE.equals(request.termsAccepted())) {
            application.setStatus(AdmissionApplicationStatus.SUBMITTED)
                    .setSubmittedAt(LocalDateTime.now());
        }

        AdmissionApplication saved = applicationRepository.save(application);
        if (lead != null) {
            lead.setStatus(AdmissionLeadStatus.CONVERTED_TO_APPLICANT)
                    .setDesiredCourse(course)
                    .setDesiredShift(application.getDesiredShift())
                    .setConvertedAt(LocalDateTime.now());
            leadRepository.save(lead);
        }
        return toApplicationResponse(saved);
    }

    @Transactional
    public AdmissionDto.ApplicationResponse submitApplication(UUID applicationId) {
        AdmissionApplication application = findApplication(applicationId);
        ensureStatus(application, AdmissionApplicationStatus.DRAFT, AdmissionApplicationStatus.DOCUMENTATION_PENDING);
        if (!Boolean.TRUE.equals(application.getTermsAccepted())) {
            throw new IllegalArgumentException("A candidatura só pode ser submetida após a aceitação dos termos.");
        }
        application.setStatus(AdmissionApplicationStatus.SUBMITTED)
                .setSubmittedAt(LocalDateTime.now());
        return toApplicationResponse(applicationRepository.save(application));
    }

    @Transactional(readOnly = true)
    public AdmissionDto.ApplicationResponse findApplication(UUID applicationId) {
        return toApplicationResponse(findApplicationEntity(applicationId));
    }

    @Transactional(readOnly = true)
    public AdmissionDto.ApplicationResponse findApplicationByCode(String applicationCode) {
        AdmissionApplication application = applicationRepository.findByApplicationCodeIgnoreCase(applicationCode)
                .orElseThrow(() -> new NotFoundException("Candidatura não encontrada."));
        return toApplicationResponse(application);
    }

    @Transactional(readOnly = true)
    public List<AdmissionDto.ApplicationResponse> listApplications(
            UUID institutionId,
            AdmissionApplicationStatus status,
            UUID courseId,
            String shift
    ) {
        List<AdmissionApplication> applications;
        if (courseId != null && shift != null && !shift.isBlank()) {
            applications = applicationRepository.findByInstitutionIdAndDesiredCourseIdAndDesiredShiftIgnoreCaseOrderByCreatedAtDesc(
                    institutionId, courseId, normalizeShift(shift));
        } else if (status != null) {
            applications = applicationRepository.findByInstitutionIdAndStatusOrderByCreatedAtDesc(institutionId, status);
        } else {
            applications = applicationRepository.findByInstitutionIdOrderByCreatedAtDesc(institutionId);
        }
        return applications.stream()
                .filter(item -> status == null || item.getStatus() == status)
                .filter(item -> courseId == null || item.getDesiredCourse().getId().equals(courseId))
                .filter(item -> shift == null || shift.isBlank() || item.getDesiredShift().equalsIgnoreCase(normalizeShift(shift)))
                .map(this::toApplicationResponse)
                .toList();
    }

    @Transactional
    public AdmissionDto.ApplicationResponse updateApplicationStatus(
            UUID applicationId,
            AdmissionDto.ApplicationStatusRequest request
    ) {
        AdmissionApplication application = findApplicationEntity(applicationId);
        application.setStatus(request.status());
        if (request.notes() != null && !request.notes().isBlank()) application.setNotes(request.notes().trim());
        if (request.status() == AdmissionApplicationStatus.CONFIRMED && application.getConfirmedAt() == null) {
            application.setConfirmedAt(LocalDateTime.now());
        }
        return toApplicationResponse(applicationRepository.save(application));
    }

    @Transactional
    public AdmissionDto.ApplicationResponse issueInvoice(UUID applicationId, AdmissionDto.InvoiceRequest request) {
        AdmissionApplication application = findApplicationEntity(applicationId);
        if (!Boolean.TRUE.equals(application.getTermsAccepted())) {
            throw new IllegalArgumentException("Não é possível emitir cobrança sem a aceitação dos termos da candidatura.");
        }
        if (request.dueDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("A data de vencimento não pode estar no passado.");
        }

        AdmissionInvoice invoice = invoiceRepository.findByApplicationId(applicationId).orElseGet(AdmissionInvoice::new);
        if (invoice.getId() == null) {
            invoice.setApplication(application).setInvoiceCode(generateInvoiceCode());
        } else if (invoice.getStatus() == AdmissionInvoiceStatus.PAID) {
            throw new IllegalArgumentException("A cobrança desta candidatura já foi paga.");
        }
        invoice.setAmount(request.amount())
                .setDueDate(request.dueDate())
                .setPaymentReference(trimToNull(request.paymentReference()))
                .setProvider(trimToNull(request.provider()))
                .setStatus(AdmissionInvoiceStatus.PENDING);
        invoiceRepository.save(invoice);
        application.setStatus(AdmissionApplicationStatus.AWAITING_PAYMENT);
        return toApplicationResponse(applicationRepository.save(application));
    }

    @Transactional
    public AdmissionDto.PaymentProofResponse submitPaymentProof(UUID invoiceId, AdmissionDto.PaymentProofRequest request) {
        AdmissionInvoice invoice = findInvoice(invoiceId);
        if (invoice.getStatus() == AdmissionInvoiceStatus.PAID) {
            throw new IllegalArgumentException("A cobrança já está paga.");
        }
        AdmissionPaymentProof proof = proofRepository.save(new AdmissionPaymentProof()
                .setInvoice(invoice)
                .setFileUrl(request.fileUrl().trim())
                .setFileName(trimToNull(request.fileName()))
                .setMimeType(trimToNull(request.mimeType()))
                .setStatus(AdmissionPaymentProofStatus.PENDING_REVIEW));
        invoice.setStatus(AdmissionInvoiceStatus.UNDER_REVIEW);
        invoiceRepository.save(invoice);
        AdmissionApplication application = invoice.getApplication();
        application.setStatus(AdmissionApplicationStatus.PAYMENT_UNDER_REVIEW);
        applicationRepository.save(application);
        return toProofResponse(proof);
    }

    @Transactional
    public AdmissionDto.ApplicationResponse approvePaymentProof(UUID proofId, AdmissionDto.ReviewPaymentProofRequest request) {
        AdmissionPaymentProof proof = findProof(proofId);
        if (proof.getStatus() != AdmissionPaymentProofStatus.PENDING_REVIEW) {
            throw new IllegalArgumentException("O comprovativo já foi analisado.");
        }
        proof.setStatus(AdmissionPaymentProofStatus.APPROVED)
                .setReviewedBy(request.reviewedBy().trim())
                .setReviewNote(trimToNull(request.reviewNote()))
                .setReviewedAt(LocalDateTime.now());
        proofRepository.save(proof);
        return confirmInvoicePayment(proof.getInvoice(), request);
    }

    @Transactional
    public AdmissionDto.ApplicationResponse rejectPaymentProof(UUID proofId, AdmissionDto.ReviewPaymentProofRequest request) {
        AdmissionPaymentProof proof = findProof(proofId);
        if (proof.getStatus() != AdmissionPaymentProofStatus.PENDING_REVIEW) {
            throw new IllegalArgumentException("O comprovativo já foi analisado.");
        }
        proof.setStatus(AdmissionPaymentProofStatus.REJECTED)
                .setReviewedBy(request.reviewedBy().trim())
                .setReviewNote(trimToNull(request.reviewNote()))
                .setReviewedAt(LocalDateTime.now());
        proofRepository.save(proof);

        AdmissionInvoice invoice = proof.getInvoice();
        invoice.setStatus(AdmissionInvoiceStatus.PENDING);
        invoiceRepository.save(invoice);
        AdmissionApplication application = invoice.getApplication();
        application.setStatus(AdmissionApplicationStatus.AWAITING_PAYMENT);
        return toApplicationResponse(applicationRepository.save(application));
    }

    @Transactional
    public AdmissionDto.ApplicationResponse confirmInvoicePayment(
            UUID invoiceId,
            AdmissionDto.ReviewPaymentProofRequest request
    ) {
        return confirmInvoicePayment(findInvoice(invoiceId), request);
    }

    @Transactional(readOnly = true)
    public AdmissionDto.DashboardResponse dashboard(UUID institutionId, UUID courseId, String shift) {
        List<AdmissionApplication> applications = applicationRepository.findByInstitutionIdOrderByCreatedAtDesc(institutionId).stream()
                .filter(item -> courseId == null || item.getDesiredCourse().getId().equals(courseId))
                .filter(item -> shift == null || shift.isBlank() || item.getDesiredShift().equalsIgnoreCase(normalizeShift(shift)))
                .toList();
        List<AdmissionLead> leads = leadRepository.findByInstitutionIdOrderByCreatedAtDesc(institutionId);

        BigDecimal totalInvoiced = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        for (AdmissionApplication application : applications) {
            AdmissionInvoice invoice = invoiceRepository.findByApplicationId(application.getId()).orElse(null);
            if (invoice != null && invoice.getAmount() != null) {
                totalInvoiced = totalInvoiced.add(invoice.getAmount());
                if (invoice.getStatus() == AdmissionInvoiceStatus.PAID) totalPaid = totalPaid.add(invoice.getAmount());
            }
        }

        List<AdmissionDto.ReportRow> rows = applications.stream().map(this::toReportRow).toList();
        return new AdmissionDto.DashboardResponse(
                leads.size(),
                leads.stream().filter(item -> item.getStatus() == AdmissionLeadStatus.CONTACTED).count(),
                applications.stream().filter(item -> item.getSubmittedAt() != null).count(),
                applications.stream().filter(item -> item.getStatus() == AdmissionApplicationStatus.AWAITING_PAYMENT).count(),
                applications.stream().filter(item -> item.getStatus() == AdmissionApplicationStatus.PAYMENT_UNDER_REVIEW).count(),
                applications.stream().filter(item -> item.getStatus() == AdmissionApplicationStatus.CONFIRMED).count(),
                totalInvoiced,
                totalPaid,
                rows
        );
    }

    private AdmissionDto.ApplicationResponse confirmInvoicePayment(
            AdmissionInvoice invoice,
            AdmissionDto.ReviewPaymentProofRequest request
    ) {
        if (invoice.getStatus() == AdmissionInvoiceStatus.PAID) {
            return toApplicationResponse(invoice.getApplication());
        }
        invoice.setStatus(AdmissionInvoiceStatus.PAID)
                .setPaymentMethod(trimToNull(request.paymentMethod()))
                .setPaymentReference(firstNonBlank(request.paymentReference(), invoice.getPaymentReference()))
                .setProvider(firstNonBlank(request.provider(), invoice.getProvider()))
                .setExternalTransactionId(trimToNull(request.externalTransactionId()))
                .setPaidAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        AdmissionApplication application = invoice.getApplication();
        if (Boolean.TRUE.equals(application.getDocumentsComplete())) {
            application.setStatus(AdmissionApplicationStatus.CONFIRMED)
                    .setConfirmedAt(LocalDateTime.now());
        } else {
            application.setStatus(AdmissionApplicationStatus.DOCUMENTATION_PENDING);
        }
        return toApplicationResponse(applicationRepository.save(application));
    }

    private AdmissionLead findDuplicateLead(AdmissionDto.LeadRequest request) {
        if (request.whatsapp() != null && !request.whatsapp().isBlank()) {
            AdmissionLead found = leadRepository.findFirstByInstitutionIdAndWhatsappIgnoreCase(request.institutionId(), request.whatsapp().trim()).orElse(null);
            if (found != null) return found;
        }
        if (request.documentNumber() != null && !request.documentNumber().isBlank()) {
            AdmissionLead found = leadRepository.findFirstByInstitutionIdAndDocumentNumberIgnoreCase(request.institutionId(), request.documentNumber().trim()).orElse(null);
            if (found != null) return found;
        }
        if (request.email() != null && !request.email().isBlank()) {
            return leadRepository.findFirstByInstitutionIdAndEmailIgnoreCase(request.institutionId(), request.email().trim()).orElse(null);
        }
        return null;
    }

    private void applyLeadData(AdmissionLead lead, AdmissionDto.LeadRequest request, Course course) {
        lead.setDesiredCourse(course)
                .setFullName(request.fullName().trim())
                .setPhone(trimToNull(request.phone()))
                .setWhatsapp(trimToNull(request.whatsapp()))
                .setEmail(trimToNull(request.email()))
                .setDocumentNumber(trimToNull(request.documentNumber()))
                .setDesiredShift(trimToNull(request.desiredShift()) == null ? null : normalizeShift(request.desiredShift()))
                .setProvince(trimToNull(request.province()))
                .setMunicipality(trimToNull(request.municipality()))
                .setLeadSource(trimToNull(request.leadSource()))
                .setConsentGiven(Boolean.TRUE.equals(request.consentGiven()))
                .setNotes(trimToNull(request.notes()));
    }

    private Institution findInstitution(UUID id) {
        return institutionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Instituição não encontrada."));
    }

    private Course findCourse(UUID id, UUID institutionId) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Curso não encontrado."));
        if (!course.getInstitution().getId().equals(institutionId)) {
            throw new IllegalArgumentException("O curso não pertence à instituição informada.");
        }
        if (!Boolean.TRUE.equals(course.getActive())) {
            throw new IllegalArgumentException("O curso informado está inativo.");
        }
        return course;
    }

    private AdmissionLead findLead(UUID id) {
        return leadRepository.findById(id).orElseThrow(() -> new NotFoundException("Lead não encontrado."));
    }

    private AdmissionApplication findApplicationEntity(UUID id) {
        return applicationRepository.findById(id).orElseThrow(() -> new NotFoundException("Candidatura não encontrada."));
    }

    private AdmissionApplication findApplication(UUID id) {
        return findApplicationEntity(id);
    }

    private AdmissionInvoice findInvoice(UUID id) {
        return invoiceRepository.findById(id).orElseThrow(() -> new NotFoundException("Cobrança de inscrição não encontrada."));
    }

    private AdmissionPaymentProof findProof(UUID id) {
        return proofRepository.findById(id).orElseThrow(() -> new NotFoundException("Comprovativo de inscrição não encontrado."));
    }

    private void ensureStatus(AdmissionApplication application, AdmissionApplicationStatus... allowed) {
        for (AdmissionApplicationStatus status : allowed) {
            if (application.getStatus() == status) return;
        }
        throw new IllegalArgumentException("A ação não é permitida no estado atual da candidatura.");
    }

    private String generateApplicationCode() {
        String code;
        do {
            code = "IMT-ADM-" + LocalDate.now().format(CODE_DATE) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        } while (applicationRepository.existsByApplicationCode(code));
        return code;
    }

    private String generateInvoiceCode() {
        String code;
        do {
            code = "IMT-INSCR-" + System.currentTimeMillis();
        } while (invoiceRepository.existsByInvoiceCode(code));
        return code;
    }

    private String normalizeShift(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String firstNonBlank(String preferred, String fallback) {
        String value = trimToNull(preferred);
        return value == null ? fallback : value;
    }

    private AdmissionDto.LeadResponse toLeadResponse(AdmissionLead lead) {
        return new AdmissionDto.LeadResponse(
                lead.getId(),
                lead.getInstitution().getId(),
                lead.getInstitution().getName(),
                lead.getDesiredCourse() == null ? null : lead.getDesiredCourse().getId(),
                lead.getDesiredCourse() == null ? null : lead.getDesiredCourse().getName(),
                lead.getFullName(), lead.getPhone(), lead.getWhatsapp(), lead.getEmail(), lead.getDocumentNumber(),
                lead.getDesiredShift(), lead.getProvince(), lead.getMunicipality(), lead.getLeadSource(),
                lead.getConsentGiven(), lead.getStatus(), lead.getLastContactAt(), lead.getConvertedAt(), lead.getNotes(),
                lead.getCreatedAt(), lead.getUpdatedAt()
        );
    }

    private AdmissionDto.ApplicationResponse toApplicationResponse(AdmissionApplication application) {
        AdmissionInvoice invoice = invoiceRepository.findByApplicationId(application.getId()).orElse(null);
        AdmissionPaymentProof proof = invoice == null ? null : proofRepository.findFirstByInvoiceIdOrderByCreatedAtDesc(invoice.getId()).orElse(null);
        return new AdmissionDto.ApplicationResponse(
                application.getId(), application.getApplicationCode(), application.getInstitution().getId(), application.getInstitution().getName(),
                application.getLead() == null ? null : application.getLead().getId(),
                application.getDesiredCourse().getId(), application.getDesiredCourse().getName(), application.getDesiredShift(), application.getAcademicYear(),
                application.getFullName(), application.getDocumentType(), application.getDocumentNumber(), application.getBirthDate(),
                application.getPhone(), application.getWhatsapp(), application.getEmail(), application.getPreviousSchool(), application.getProvince(),
                application.getMunicipality(), application.getDocumentsComplete(), application.getTermsAccepted(), application.getStatus(), application.getNotes(),
                application.getSubmittedAt(), application.getConfirmedAt(), toInvoiceResponse(invoice), toProofResponse(proof),
                application.getCreatedAt(), application.getUpdatedAt()
        );
    }

    private AdmissionDto.InvoiceResponse toInvoiceResponse(AdmissionInvoice invoice) {
        if (invoice == null) return null;
        return new AdmissionDto.InvoiceResponse(
                invoice.getId(), invoice.getInvoiceCode(), invoice.getAmount(), invoice.getCurrency(), invoice.getDueDate(), invoice.getStatus(),
                invoice.getPaymentMethod(), invoice.getPaymentReference(), invoice.getProvider(), invoice.getExternalTransactionId(), invoice.getPaidAt(),
                invoice.getCreatedAt(), invoice.getUpdatedAt()
        );
    }

    private AdmissionDto.PaymentProofResponse toProofResponse(AdmissionPaymentProof proof) {
        if (proof == null) return null;
        return new AdmissionDto.PaymentProofResponse(
                proof.getId(), proof.getInvoice().getId(), proof.getFileUrl(), proof.getFileName(), proof.getMimeType(), proof.getStatus(),
                proof.getReviewedBy(), proof.getReviewNote(), proof.getSubmittedAt(), proof.getReviewedAt(), proof.getCreatedAt(), proof.getUpdatedAt()
        );
    }

    private AdmissionDto.ReportRow toReportRow(AdmissionApplication application) {
        AdmissionInvoice invoice = invoiceRepository.findByApplicationId(application.getId()).orElse(null);
        return new AdmissionDto.ReportRow(
                application.getApplicationCode(), application.getFullName(), application.getDocumentNumber(),
                firstNonBlank(application.getWhatsapp(), application.getPhone()), application.getDesiredCourse().getName(), application.getDesiredShift(),
                application.getStatus(), invoice == null ? null : invoice.getStatus(), application.getSubmittedAt(), application.getConfirmedAt()
        );
    }
}
