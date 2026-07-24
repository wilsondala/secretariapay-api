package com.secretariapay.api.service.enrollment;

import com.secretariapay.api.dto.enrollment.EnrollmentDto;
import com.secretariapay.api.entity.academic.AcademicClass;
import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.admission.AdmissionApplication;
import com.secretariapay.api.entity.admission.AdmissionCampaign;
import com.secretariapay.api.entity.admission.AdmissionInvoice;
import com.secretariapay.api.entity.enrollment.AcademicEnrollmentInvoice;
import com.secretariapay.api.entity.enrollment.AcademicEnrollmentPaymentProof;
import com.secretariapay.api.entity.enrollment.AcademicEnrollmentRequest;
import com.secretariapay.api.entity.enums.academic.AcademicShift;
import com.secretariapay.api.entity.enums.academic.StudentStatus;
import com.secretariapay.api.entity.enums.admission.AdmissionApplicationStatus;
import com.secretariapay.api.entity.enums.admission.AdmissionInvoiceStatus;
import com.secretariapay.api.entity.enums.admission.AdmissionShift;
import com.secretariapay.api.entity.enums.enrollment.EnrollmentInvoiceStatus;
import com.secretariapay.api.entity.enums.enrollment.EnrollmentPaymentProofStatus;
import com.secretariapay.api.entity.enums.enrollment.EnrollmentRequestStatus;
import com.secretariapay.api.entity.enums.enrollment.EnrollmentRequestType;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.academic.AcademicClassRepository;
import com.secretariapay.api.repository.academic.CourseRepository;
import com.secretariapay.api.repository.academic.StudentRepository;
import com.secretariapay.api.repository.admission.AdmissionApplicationRepository;
import com.secretariapay.api.repository.admission.AdmissionCampaignRepository;
import com.secretariapay.api.repository.admission.AdmissionCourseOfferingRepository;
import com.secretariapay.api.repository.admission.AdmissionInvoiceRepository;
import com.secretariapay.api.repository.enrollment.AcademicEnrollmentInvoiceRepository;
import com.secretariapay.api.repository.enrollment.AcademicEnrollmentPaymentProofRepository;
import com.secretariapay.api.repository.enrollment.AcademicEnrollmentRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class EnrollmentService {

    private static final ZoneId LUANDA_ZONE = ZoneId.of("Africa/Luanda");
    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AcademicEnrollmentRequestRepository requestRepository;
    private final AcademicEnrollmentInvoiceRepository invoiceRepository;
    private final AcademicEnrollmentPaymentProofRepository proofRepository;
    private final AdmissionApplicationRepository admissionApplicationRepository;
    private final AdmissionInvoiceRepository admissionInvoiceRepository;
    private final AdmissionCampaignRepository campaignRepository;
    private final AdmissionCourseOfferingRepository offeringRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final AcademicClassRepository academicClassRepository;

    public EnrollmentService(
            AcademicEnrollmentRequestRepository requestRepository,
            AcademicEnrollmentInvoiceRepository invoiceRepository,
            AcademicEnrollmentPaymentProofRepository proofRepository,
            AdmissionApplicationRepository admissionApplicationRepository,
            AdmissionInvoiceRepository admissionInvoiceRepository,
            AdmissionCampaignRepository campaignRepository,
            AdmissionCourseOfferingRepository offeringRepository,
            StudentRepository studentRepository,
            CourseRepository courseRepository,
            AcademicClassRepository academicClassRepository
    ) {
        this.requestRepository = requestRepository;
        this.invoiceRepository = invoiceRepository;
        this.proofRepository = proofRepository;
        this.admissionApplicationRepository = admissionApplicationRepository;
        this.admissionInvoiceRepository = admissionInvoiceRepository;
        this.campaignRepository = campaignRepository;
        this.offeringRepository = offeringRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.academicClassRepository = academicClassRepository;
    }

    @Transactional
    public EnrollmentDto.EnrollmentResponse createEnrollmentFromAdmission(
            UUID applicationId,
            EnrollmentDto.EnrollmentFromAdmissionRequest request
    ) {
        AdmissionApplication application = admissionApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Candidatura não encontrada."));

        if (application.getStatus() != AdmissionApplicationStatus.CONFIRMED) {
            throw new IllegalArgumentException("A matrícula exige uma inscrição confirmada.");
        }
        if (application.getCampaign() == null) {
            throw new IllegalArgumentException("A candidatura não está vinculada a uma campanha académica.");
        }
        if (requestRepository.existsByAdmissionApplicationId(applicationId)) {
            throw new IllegalArgumentException("Já existe uma matrícula para esta candidatura.");
        }

        AdmissionInvoice registrationInvoice = admissionInvoiceRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("A cobrança da inscrição não foi encontrada."));
        if (registrationInvoice.getStatus() != AdmissionInvoiceStatus.PAID) {
            throw new IllegalArgumentException("A matrícula exige a inscrição paga.");
        }

        validateDueDate(request.dueDate());
        int yearLevel = validateYearLevel(request.targetYearLevel() == null ? 1 : request.targetYearLevel());
        AdmissionShift shift = normalizeAdmissionShift(application.getDesiredShift());
        validateOffering(application.getCampaign(), application.getDesiredCourse().getId(), shift);

        AcademicEnrollmentRequest enrollment = requestRepository.save(new AcademicEnrollmentRequest()
                .setRequestCode(generateRequestCode(EnrollmentRequestType.ENROLLMENT))
                .setInstitution(application.getInstitution())
                .setCampaign(application.getCampaign())
                .setRequestType(EnrollmentRequestType.ENROLLMENT)
                .setAdmissionApplication(application)
                .setTargetCourse(application.getDesiredCourse())
                .setTargetShift(shift.name())
                .setAcademicYear(application.getAcademicYear())
                .setTargetYearLevel(yearLevel)
                .setStatus(EnrollmentRequestStatus.AWAITING_PAYMENT));

        createInvoice(
                enrollment,
                application.getCampaign().getEnrollmentFee(),
                application.getCampaign().getCurrency(),
                request.dueDate(),
                request.paymentReference(),
                request.provider()
        );
        return toResponse(enrollment);
    }

    @Transactional
    public EnrollmentDto.EnrollmentResponse createReenrollment(EnrollmentDto.ReenrollmentRequest request) {
        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new NotFoundException("Estudante não encontrado."));
        Course targetCourse = courseRepository.findById(request.targetCourseId())
                .orElseThrow(() -> new NotFoundException("Curso não encontrado."));

        UUID institutionId = student.getAcademicClass().getCourse().getInstitution().getId();
        if (!targetCourse.getInstitution().getId().equals(institutionId)) {
            throw new IllegalArgumentException("O curso escolhido não pertence à instituição do estudante.");
        }
        if (!Boolean.TRUE.equals(targetCourse.getActive())) {
            throw new IllegalArgumentException("O curso escolhido está inativo.");
        }

        AdmissionCampaign campaign = findActiveCampaign(institutionId);
        if (!campaign.getAcademicYear().equalsIgnoreCase(request.academicYear().trim())) {
            throw new IllegalArgumentException(
                    "O ano académico informado não corresponde à campanha ativa: " + campaign.getAcademicYear() + "."
            );
        }
        if (requestRepository.existsByStudentIdAndAcademicYearAndRequestTypeAndStatusNot(
                student.getId(),
                campaign.getAcademicYear(),
                EnrollmentRequestType.REENROLLMENT,
                EnrollmentRequestStatus.CANCELLED)) {
            throw new IllegalArgumentException("Já existe uma rematrícula ativa para este estudante no ano académico.");
        }

        validateDueDate(request.dueDate());
        int yearLevel = validateYearLevel(request.targetYearLevel());
        AdmissionShift shift = normalizeAdmissionShift(request.targetShift());
        validateOffering(campaign, targetCourse.getId(), shift);

        AcademicEnrollmentRequest reenrollment = requestRepository.save(new AcademicEnrollmentRequest()
                .setRequestCode(generateRequestCode(EnrollmentRequestType.REENROLLMENT))
                .setInstitution(targetCourse.getInstitution())
                .setCampaign(campaign)
                .setRequestType(EnrollmentRequestType.REENROLLMENT)
                .setStudent(student)
                .setTargetCourse(targetCourse)
                .setTargetShift(shift.name())
                .setAcademicYear(campaign.getAcademicYear())
                .setTargetYearLevel(yearLevel)
                .setStatus(EnrollmentRequestStatus.AWAITING_PAYMENT));

        createInvoice(
                reenrollment,
                campaign.getReenrollmentFee(),
                campaign.getCurrency(),
                request.dueDate(),
                request.paymentReference(),
                request.provider()
        );
        return toResponse(reenrollment);
    }

    @Transactional(readOnly = true)
    public EnrollmentDto.EnrollmentResponse get(UUID requestId) {
        return toResponse(findRequest(requestId));
    }

    @Transactional(readOnly = true)
    public List<EnrollmentDto.EnrollmentResponse> list(
            UUID institutionId,
            EnrollmentRequestType requestType,
            EnrollmentRequestStatus status
    ) {
        List<AcademicEnrollmentRequest> rows = requestType == null
                ? requestRepository.findByInstitutionIdOrderByCreatedAtDesc(institutionId)
                : requestRepository.findByInstitutionIdAndRequestTypeOrderByCreatedAtDesc(institutionId, requestType);
        return rows.stream()
                .filter(item -> status == null || item.getStatus() == status)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public EnrollmentDto.PaymentProofResponse submitPaymentProof(
            UUID invoiceId,
            EnrollmentDto.PaymentProofRequest request
    ) {
        AcademicEnrollmentInvoice invoice = findInvoice(invoiceId);
        if (invoice.getStatus() == EnrollmentInvoiceStatus.PAID) {
            throw new IllegalArgumentException("A cobrança já está paga.");
        }

        AcademicEnrollmentPaymentProof proof = proofRepository.save(new AcademicEnrollmentPaymentProof()
                .setInvoice(invoice)
                .setFileUrl(request.fileUrl().trim())
                .setFileName(trimToNull(request.fileName()))
                .setMimeType(trimToNull(request.mimeType()))
                .setStatus(EnrollmentPaymentProofStatus.PENDING_REVIEW));

        invoice.setStatus(EnrollmentInvoiceStatus.UNDER_REVIEW);
        invoiceRepository.save(invoice);
        invoice.getEnrollmentRequest().setStatus(EnrollmentRequestStatus.PAYMENT_UNDER_REVIEW);
        requestRepository.save(invoice.getEnrollmentRequest());
        return toProofResponse(proof);
    }

    @Transactional
    public EnrollmentDto.EnrollmentResponse approvePaymentProof(
            UUID proofId,
            EnrollmentDto.ReviewPaymentRequest request
    ) {
        AcademicEnrollmentPaymentProof proof = findProof(proofId);
        ensurePendingProof(proof);
        proof.setStatus(EnrollmentPaymentProofStatus.APPROVED)
                .setReviewedBy(request.reviewedBy().trim())
                .setReviewNote(trimToNull(request.reviewNote()))
                .setReviewedAt(LocalDateTime.now());
        proofRepository.save(proof);
        return completePayment(proof.getInvoice(), request);
    }

    @Transactional
    public EnrollmentDto.EnrollmentResponse rejectPaymentProof(
            UUID proofId,
            EnrollmentDto.ReviewPaymentRequest request
    ) {
        AcademicEnrollmentPaymentProof proof = findProof(proofId);
        ensurePendingProof(proof);
        proof.setStatus(EnrollmentPaymentProofStatus.REJECTED)
                .setReviewedBy(request.reviewedBy().trim())
                .setReviewNote(trimToNull(request.reviewNote()))
                .setReviewedAt(LocalDateTime.now());
        proofRepository.save(proof);

        AcademicEnrollmentInvoice invoice = proof.getInvoice();
        invoice.setStatus(EnrollmentInvoiceStatus.PENDING);
        invoiceRepository.save(invoice);
        invoice.getEnrollmentRequest().setStatus(EnrollmentRequestStatus.AWAITING_PAYMENT);
        requestRepository.save(invoice.getEnrollmentRequest());
        return toResponse(invoice.getEnrollmentRequest());
    }

    @Transactional
    public EnrollmentDto.EnrollmentResponse confirmPayment(
            UUID invoiceId,
            EnrollmentDto.ReviewPaymentRequest request
    ) {
        return completePayment(findInvoice(invoiceId), request);
    }

    @Transactional(readOnly = true)
    public EnrollmentDto.DashboardResponse dashboard(UUID institutionId) {
        List<AcademicEnrollmentRequest> rows = requestRepository
                .findByInstitutionIdOrderByCreatedAtDesc(institutionId);

        BigDecimal enrollmentRevenue = BigDecimal.ZERO;
        BigDecimal reenrollmentRevenue = BigDecimal.ZERO;
        for (AcademicEnrollmentRequest row : rows) {
            AcademicEnrollmentInvoice invoice = invoiceRepository.findByEnrollmentRequestId(row.getId()).orElse(null);
            if (invoice != null && invoice.getStatus() == EnrollmentInvoiceStatus.PAID) {
                if (row.getRequestType() == EnrollmentRequestType.ENROLLMENT) {
                    enrollmentRevenue = enrollmentRevenue.add(invoice.getAmount());
                } else {
                    reenrollmentRevenue = reenrollmentRevenue.add(invoice.getAmount());
                }
            }
        }

        return new EnrollmentDto.DashboardResponse(
                count(rows, EnrollmentRequestType.ENROLLMENT, null),
                count(rows, EnrollmentRequestType.REENROLLMENT, null),
                count(rows, EnrollmentRequestType.ENROLLMENT, EnrollmentRequestStatus.COMPLETED),
                count(rows, EnrollmentRequestType.REENROLLMENT, EnrollmentRequestStatus.COMPLETED),
                count(rows, EnrollmentRequestType.ENROLLMENT, EnrollmentRequestStatus.AWAITING_PAYMENT),
                count(rows, EnrollmentRequestType.REENROLLMENT, EnrollmentRequestStatus.AWAITING_PAYMENT),
                count(rows, EnrollmentRequestType.ENROLLMENT, EnrollmentRequestStatus.PAYMENT_UNDER_REVIEW),
                count(rows, EnrollmentRequestType.REENROLLMENT, EnrollmentRequestStatus.PAYMENT_UNDER_REVIEW),
                enrollmentRevenue,
                reenrollmentRevenue,
                enrollmentRevenue.add(reenrollmentRevenue),
                rows.stream().map(this::toDashboardRow).toList()
        );
    }

    private EnrollmentDto.EnrollmentResponse completePayment(
            AcademicEnrollmentInvoice invoice,
            EnrollmentDto.ReviewPaymentRequest review
    ) {
        if (invoice.getStatus() == EnrollmentInvoiceStatus.PAID
                && invoice.getEnrollmentRequest().getStatus() == EnrollmentRequestStatus.COMPLETED) {
            return toResponse(invoice.getEnrollmentRequest());
        }

        AcademicEnrollmentRequest enrollment = invoice.getEnrollmentRequest();
        Student student = resolveStudent(enrollment);
        AcademicClass targetClass = resolveAcademicClass(enrollment);

        student.setAcademicClass(targetClass)
                .setStatus(StudentStatus.ACTIVE)
                .setFinanciallyBlocked(false)
                .setBlockedReason(null);
        studentRepository.save(student);

        invoice.setStatus(EnrollmentInvoiceStatus.PAID)
                .setPaymentMethod(trimToNull(review.paymentMethod()))
                .setPaymentReference(firstNonBlank(review.paymentReference(), invoice.getPaymentReference()))
                .setProvider(firstNonBlank(review.provider(), invoice.getProvider()))
                .setExternalTransactionId(trimToNull(review.externalTransactionId()))
                .setPaidAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        enrollment.setStudent(student)
                .setStatus(EnrollmentRequestStatus.COMPLETED)
                .setCompletedAt(LocalDateTime.now());
        requestRepository.save(enrollment);
        return toResponse(enrollment);
    }

    private Student resolveStudent(AcademicEnrollmentRequest enrollment) {
        if (enrollment.getRequestType() == EnrollmentRequestType.REENROLLMENT) {
            if (enrollment.getStudent() == null) {
                throw new IllegalArgumentException("A rematrícula não possui estudante associado.");
            }
            return enrollment.getStudent();
        }

        AdmissionApplication application = enrollment.getAdmissionApplication();
        if (application == null) {
            throw new IllegalArgumentException("A matrícula não possui candidatura associada.");
        }
        studentRepository.findByDocumentNumberIgnoreCase(application.getDocumentNumber())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Já existe um estudante com o documento informado.");
                });

        return new Student()
                .setStudentNumber(generateStudentNumber(enrollment.getAcademicYear()))
                .setFullName(application.getFullName())
                .setDocumentType(application.getDocumentType())
                .setDocumentNumber(application.getDocumentNumber())
                .setEmail(application.getEmail())
                .setPhone(application.getPhone())
                .setWhatsapp(application.getWhatsapp())
                .setBirthDate(application.getBirthDate())
                .setStatus(StudentStatus.ACTIVE)
                .setFinanciallyBlocked(false);
    }

    private AcademicClass resolveAcademicClass(AcademicEnrollmentRequest enrollment) {
        AcademicShift academicShift = toAcademicShift(enrollment.getTargetShift());
        String className = enrollment.getTargetYearLevel() + ".º Ano - "
                + shiftLabel(enrollment.getTargetShift()) + " - " + enrollment.getAcademicYear();
        return academicClassRepository.findFirstByCourseIdAndNameIgnoreCaseAndAcademicYearAndShift(
                        enrollment.getTargetCourse().getId(),
                        className,
                        enrollment.getAcademicYear(),
                        academicShift)
                .orElseGet(() -> academicClassRepository.save(new AcademicClass()
                        .setCourse(enrollment.getTargetCourse())
                        .setName(className)
                        .setAcademicYear(enrollment.getAcademicYear())
                        .setYearLevel(enrollment.getTargetYearLevel())
                        .setShift(academicShift)
                        .setActive(true)));
    }

    private void createInvoice(
            AcademicEnrollmentRequest enrollment,
            BigDecimal amount,
            String currency,
            LocalDate dueDate,
            String paymentReference,
            String provider
    ) {
        invoiceRepository.save(new AcademicEnrollmentInvoice()
                .setEnrollmentRequest(enrollment)
                .setInvoiceCode(generateInvoiceCode(enrollment.getRequestType()))
                .setAmount(amount)
                .setCurrency(currency)
                .setDueDate(dueDate)
                .setPaymentReference(trimToNull(paymentReference))
                .setProvider(trimToNull(provider))
                .setStatus(EnrollmentInvoiceStatus.PENDING));
    }

    private AdmissionCampaign findActiveCampaign(UUID institutionId) {
        return campaignRepository.findFirstByInstitutionIdAndActiveTrueOrderByRegistrationStartDesc(institutionId)
                .orElseThrow(() -> new NotFoundException("Não existe campanha académica ativa para a instituição."));
    }

    private void validateOffering(AdmissionCampaign campaign, UUID courseId, AdmissionShift shift) {
        if (!offeringRepository.existsByCampaignIdAndCourseIdAndShiftAndActiveTrue(
                campaign.getId(), courseId, shift)) {
            throw new IllegalArgumentException("O curso ou turno escolhido não está disponível na campanha ativa.");
        }
    }

    private void validateDueDate(LocalDate dueDate) {
        if (dueDate.isBefore(LocalDate.now(LUANDA_ZONE))) {
            throw new IllegalArgumentException("A data de vencimento não pode estar no passado.");
        }
    }

    private int validateYearLevel(Integer yearLevel) {
        if (yearLevel == null || yearLevel < 1 || yearLevel > 10) {
            throw new IllegalArgumentException("O ano curricular deve estar entre 1 e 10.");
        }
        return yearLevel;
    }

    private AcademicEnrollmentRequest findRequest(UUID id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Matrícula ou rematrícula não encontrada."));
    }

    private AcademicEnrollmentInvoice findInvoice(UUID id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cobrança de matrícula ou rematrícula não encontrada."));
    }

    private AcademicEnrollmentPaymentProof findProof(UUID id) {
        return proofRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Comprovativo de matrícula ou rematrícula não encontrado."));
    }

    private void ensurePendingProof(AcademicEnrollmentPaymentProof proof) {
        if (proof.getStatus() != EnrollmentPaymentProofStatus.PENDING_REVIEW) {
            throw new IllegalArgumentException("O comprovativo já foi analisado.");
        }
    }

    private long count(
            List<AcademicEnrollmentRequest> rows,
            EnrollmentRequestType type,
            EnrollmentRequestStatus status
    ) {
        return rows.stream()
                .filter(item -> item.getRequestType() == type)
                .filter(item -> status == null || item.getStatus() == status)
                .count();
    }

    private AdmissionShift normalizeAdmissionShift(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("O turno é obrigatório.");
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "MANHA", "MORNING" -> AdmissionShift.MANHA;
            case "TARDE", "AFTERNOON" -> AdmissionShift.TARDE;
            case "NOITE", "NIGHT", "EVENING" -> AdmissionShift.NOITE;
            default -> throw new IllegalArgumentException("Turno inválido. Utilize Manhã, Tarde ou Noite.");
        };
    }

    private AcademicShift toAcademicShift(String value) {
        return switch (normalizeAdmissionShift(value)) {
            case MANHA -> AcademicShift.MORNING;
            case TARDE -> AcademicShift.AFTERNOON;
            case NOITE -> AcademicShift.NIGHT;
        };
    }

    private String shiftLabel(String value) {
        return switch (normalizeAdmissionShift(value)) {
            case MANHA -> "Manhã";
            case TARDE -> "Tarde";
            case NOITE -> "Noite";
        };
    }

    private String generateRequestCode(EnrollmentRequestType type) {
        String prefix = type == EnrollmentRequestType.ENROLLMENT ? "IMT-MAT" : "IMT-REMAT";
        String code;
        do {
            code = prefix + "-" + LocalDate.now(LUANDA_ZONE).format(CODE_DATE) + "-"
                    + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        } while (requestRepository.existsByRequestCode(code));
        return code;
    }

    private String generateInvoiceCode(EnrollmentRequestType type) {
        String prefix = type == EnrollmentRequestType.ENROLLMENT ? "IMT-MATR" : "IMT-REMATR";
        String code;
        do {
            code = prefix + "-" + System.currentTimeMillis();
        } while (invoiceRepository.existsByInvoiceCode(code));
        return code;
    }

    private String generateStudentNumber(String academicYear) {
        String year = academicYear == null || academicYear.length() < 4
                ? String.valueOf(LocalDate.now(LUANDA_ZONE).getYear())
                : academicYear.substring(0, 4);
        String number;
        do {
            number = "IMT-" + year + "-" + UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 6)
                    .toUpperCase(Locale.ROOT);
        } while (studentRepository.existsByStudentNumber(number));
        return number;
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

    private EnrollmentDto.EnrollmentResponse toResponse(AcademicEnrollmentRequest enrollment) {
        AcademicEnrollmentInvoice invoice = invoiceRepository
                .findByEnrollmentRequestId(enrollment.getId()).orElse(null);
        AcademicEnrollmentPaymentProof proof = invoice == null
                ? null
                : proofRepository.findFirstByInvoiceIdOrderByCreatedAtDesc(invoice.getId()).orElse(null);
        AdmissionApplication application = enrollment.getAdmissionApplication();
        Student student = enrollment.getStudent();

        return new EnrollmentDto.EnrollmentResponse(
                enrollment.getId(),
                enrollment.getRequestCode(),
                enrollment.getRequestType(),
                enrollment.getStatus(),
                enrollment.getInstitution().getId(),
                enrollment.getInstitution().getName(),
                enrollment.getCampaign().getId(),
                enrollment.getCampaign().getCampaignCode(),
                enrollment.getAcademicYear(),
                application == null ? null : application.getId(),
                application == null ? null : application.getApplicationCode(),
                student == null ? null : student.getId(),
                student == null ? null : student.getStudentNumber(),
                application != null ? application.getFullName() : student == null ? null : student.getFullName(),
                application != null ? application.getDocumentNumber() : student == null ? null : student.getDocumentNumber(),
                enrollment.getTargetCourse().getId(),
                enrollment.getTargetCourse().getName(),
                enrollment.getTargetShift(),
                enrollment.getTargetYearLevel(),
                enrollment.getCompletedAt(),
                toInvoiceResponse(invoice),
                toProofResponse(proof),
                enrollment.getCreatedAt(),
                enrollment.getUpdatedAt()
        );
    }

    private EnrollmentDto.InvoiceResponse toInvoiceResponse(AcademicEnrollmentInvoice invoice) {
        if (invoice == null) return null;
        return new EnrollmentDto.InvoiceResponse(
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

    private EnrollmentDto.PaymentProofResponse toProofResponse(AcademicEnrollmentPaymentProof proof) {
        if (proof == null) return null;
        return new EnrollmentDto.PaymentProofResponse(
                proof.getId(),
                proof.getInvoice().getId(),
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

    private EnrollmentDto.DashboardRow toDashboardRow(AcademicEnrollmentRequest enrollment) {
        AcademicEnrollmentInvoice invoice = invoiceRepository
                .findByEnrollmentRequestId(enrollment.getId()).orElse(null);
        AdmissionApplication application = enrollment.getAdmissionApplication();
        Student student = enrollment.getStudent();
        return new EnrollmentDto.DashboardRow(
                enrollment.getRequestCode(),
                enrollment.getRequestType(),
                application != null ? application.getFullName() : student == null ? null : student.getFullName(),
                student == null ? null : student.getStudentNumber(),
                application != null ? application.getDocumentNumber() : student == null ? null : student.getDocumentNumber(),
                enrollment.getTargetCourse().getName(),
                enrollment.getTargetShift(),
                enrollment.getTargetYearLevel(),
                enrollment.getStatus(),
                invoice == null ? null : invoice.getStatus(),
                invoice == null ? null : invoice.getAmount(),
                enrollment.getCompletedAt()
        );
    }
}
