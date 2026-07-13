package com.secretariapay.api.service.academic;

import com.secretariapay.api.dto.academic.AcademicDocumentDto;
import com.secretariapay.api.dto.whatsapp.WhatsAppCloudSendResult;
import com.secretariapay.api.entity.academic.AcademicClass;
import com.secretariapay.api.entity.academic.AcademicDocumentRequest;
import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.academic.AcademicDocumentRequestRepository;
import com.secretariapay.api.repository.academic.StudentRepository;
import com.secretariapay.api.service.whatsapp.WhatsAppCloudApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AcademicDocumentService {

    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String DEMO_SIGNATORY_NAME = "Zakeu António Zengo";
    private static final String DEMO_SIGNATORY_ROLE = "Presidente da Instituição";

    private final AcademicDocumentRequestRepository repository;
    private final StudentRepository studentRepository;
    private final AcademicDocumentPdfService pdfService;
    private final WhatsAppCloudApiClient whatsAppCloudApiClient;
    private final String apiBaseUrl;

    public AcademicDocumentService(
            AcademicDocumentRequestRepository repository,
            StudentRepository studentRepository,
            AcademicDocumentPdfService pdfService,
            WhatsAppCloudApiClient whatsAppCloudApiClient,
            @Value("${secretariapay.public-api-base-url:https://secretariapay-api.paixaoangola.com}") String apiBaseUrl
    ) {
        this.repository = repository;
        this.studentRepository = studentRepository;
        this.pdfService = pdfService;
        this.whatsAppCloudApiClient = whatsAppCloudApiClient;
        this.apiBaseUrl = stripTrailingSlash(apiBaseUrl);
    }

    @Transactional(readOnly = true)
    public List<AcademicDocumentDto.Response> list() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AcademicDocumentDto.Response findById(UUID id) {
        return toResponse(load(id));
    }

    @Transactional
    public AcademicDocumentDto.Response createDemoDeclaration(AcademicDocumentDto.CreateDemoRequest request) {
        if (request == null || isBlank(request.studentNumber())) {
            throw new IllegalArgumentException("Matrícula do estudante é obrigatória.");
        }

        Student student = studentRepository.findByStudentNumber(request.studentNumber().trim())
                .orElseThrow(() -> new NotFoundException("Estudante não encontrado."));

        AcademicDocumentRequest entity = new AcademicDocumentRequest()
                .setDocumentCode(nextDocumentCode(student))
                .setStudent(student)
                .setServiceCode("DECLARATION_WITHOUT_GRADES")
                .setDocumentType("SIMPLE_DECLARATION")
                .setStatus("DRAFT")
                .setPurpose(firstNonBlank(request.purpose(), "Comprovação da situação académica"))
                .setDeclarationText(firstNonBlank(request.declarationText(), defaultDeclarationText(student)))
                .setSignatoryName(DEMO_SIGNATORY_NAME)
                .setSignatoryRole(DEMO_SIGNATORY_ROLE)
                .setDemoMode(true)
                .setVersionNumber(1)
                .setIssuedAt(LocalDateTime.now())
                .setCreatedBy(currentActor());

        return toResponse(repository.save(entity));
    }

    @Transactional
    public AcademicDocumentDto.Response update(UUID id, AcademicDocumentDto.UpdateRequest request) {
        AcademicDocumentRequest entity = load(id);
        ensureEditable(entity);
        if (request == null) throw new IllegalArgumentException("Dados do documento são obrigatórios.");
        if (!isBlank(request.purpose())) entity.setPurpose(request.purpose().trim());
        if (!isBlank(request.declarationText())) entity.setDeclarationText(request.declarationText().trim());
        entity.setStatus("DRAFT");
        entity.setDocumentHash(null);
        entity.setSignedAt(null);
        entity.setSignatureMethod(null);
        entity.setSignedBy(null);
        return toResponse(repository.save(entity));
    }

    @Transactional
    public AcademicDocumentDto.Response markReadyForSignature(UUID id) {
        AcademicDocumentRequest entity = load(id);
        ensureEditable(entity);
        if (isBlank(entity.getDeclarationText())) throw new IllegalArgumentException("O texto da declaração é obrigatório.");
        entity.setStatus("READY_FOR_SIGNATURE");
        return toResponse(repository.save(entity));
    }

    @Transactional
    public AcademicDocumentDto.Response signDemo(UUID id) {
        AcademicDocumentRequest entity = load(id);
        if (!"READY_FOR_SIGNATURE".equals(entity.getStatus())) {
            throw new IllegalStateException("O documento precisa estar pronto para assinatura.");
        }

        LocalDateTime signedAt = LocalDateTime.now();
        entity.setSignatoryName(DEMO_SIGNATORY_NAME)
                .setSignatoryRole(DEMO_SIGNATORY_ROLE)
                .setSignatureMethod("SECRETARIAPAY_DEMO_ELECTRONIC_SIGNATURE")
                .setSignedAt(signedAt)
                .setSignedBy(currentActor())
                .setStatus("SIGNED")
                .setDocumentHash(calculateHash(entity, signedAt));
        return toResponse(repository.save(entity));
    }

    @Transactional
    public AcademicDocumentDto.Response sendByWhatsapp(UUID id) {
        AcademicDocumentRequest entity = load(id);
        ensurePubliclyAvailable(entity);

        Student student = entity.getStudent();
        String recipient = firstNonBlank(student.getWhatsapp(), student.getPhone(), student.getGuardianPhone());
        if (isBlank(recipient)) throw new IllegalStateException("O estudante não possui WhatsApp ou telefone cadastrado.");

        String pdfUrl = publicPdfUrl(entity) + "?v=" + System.currentTimeMillis();
        String fileName = "Declaracao_IMETRO_" + sanitize(student.getStudentNumber()) + "_" + sanitize(entity.getDocumentCode()) + ".pdf";
        String caption = """
                ✅ Documento académico concluído.

                Documento: Declaração simples
                Estudante: %s
                Matrícula: %s
                Estado: Assinado eletronicamente — demonstração

                O documento segue em anexo.

                Instituto Superior Politécnico Metropolitano de Angola — IMETRO.
                """.formatted(student.getFullName(), student.getStudentNumber()).trim();

        WhatsAppCloudSendResult result = whatsAppCloudApiClient.sendDocumentByLink(recipient, pdfUrl, fileName, caption);
        if (!result.isSuccess()) {
            throw new IllegalStateException(firstNonBlank(result.getErrorMessage(), "Não foi possível enviar o documento pelo WhatsApp."));
        }

        entity.setStatus("SENT").setSentAt(LocalDateTime.now());
        return toResponse(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public byte[] generatePdf(UUID id) {
        return pdfService.generate(load(id));
    }

    @Transactional(readOnly = true)
    public byte[] generatePublicPdf(String documentCode) {
        AcademicDocumentRequest entity = loadByCode(documentCode);
        ensurePubliclyAvailable(entity);
        return pdfService.generate(entity);
    }

    @Transactional(readOnly = true)
    public AcademicDocumentDto.ValidationResponse validatePublic(String documentCode) {
        AcademicDocumentRequest entity = loadByCode(documentCode);
        ensurePubliclyAvailable(entity);
        Student student = entity.getStudent();
        return new AcademicDocumentDto.ValidationResponse(
                true,
                entity.getDocumentCode(),
                entity.getStatus(),
                student.getStudentNumber(),
                student.getFullName(),
                entity.getDocumentType(),
                entity.getSignatoryName(),
                entity.getSignatoryRole(),
                entity.getDocumentHash(),
                entity.getVersionNumber(),
                entity.isDemoMode(),
                entity.getIssuedAt(),
                entity.getSignedAt()
        );
    }

    private AcademicDocumentRequest load(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Documento académico não encontrado."));
    }

    private AcademicDocumentRequest loadByCode(String documentCode) {
        if (isBlank(documentCode)) throw new NotFoundException("Documento académico não encontrado.");
        return repository.findByDocumentCode(documentCode.trim())
                .orElseThrow(() -> new NotFoundException("Documento académico não encontrado."));
    }

    private void ensureEditable(AcademicDocumentRequest entity) {
        if ("SIGNED".equals(entity.getStatus()) || "SENT".equals(entity.getStatus())) {
            throw new IllegalStateException("Documento assinado não pode ser alterado. Crie uma nova versão.");
        }
        if ("CANCELLED".equals(entity.getStatus())) {
            throw new IllegalStateException("Documento cancelado não pode ser alterado.");
        }
    }

    private void ensurePubliclyAvailable(AcademicDocumentRequest entity) {
        boolean signed = "SIGNED".equals(entity.getStatus()) || "SENT".equals(entity.getStatus());
        if (!signed || isBlank(entity.getDocumentHash()) || entity.getSignedAt() == null) {
            throw new NotFoundException("Documento assinado não encontrado.");
        }
    }

    private String nextDocumentCode(Student student) {
        String studentPart = sanitize(student.getStudentNumber());
        if (studentPart.length() > 18) studentPart = studentPart.substring(studentPart.length() - 18);
        return "IMT-DECL-SN-" + LocalDate.now().format(CODE_DATE) + "-" + studentPart + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase(Locale.ROOT);
    }

    private String defaultDeclarationText(Student student) {
        AcademicClass academicClass = student.getAcademicClass();
        Course course = academicClass == null ? null : academicClass.getCourse();
        String documentNumber = firstNonBlank(student.getDocumentNumber(), "não informado");
        String courseName = course == null ? "curso registado na instituição" : course.getName();
        String academicYear = academicClass == null ? String.valueOf(LocalDate.now().getYear()) : academicClass.getAcademicYear();
        return "O Instituto Superior Politécnico Metropolitano de Angola - IMETRO, por intermédio da sua Secretaria Académica, declara, para os devidos efeitos, que "
                + student.getFullName() + ", portador do documento de identificação n.º " + documentNumber
                + ", matrícula n.º " + student.getStudentNumber() + ", encontra-se regularmente matriculado no curso de "
                + courseName + ", no ano académico de " + academicYear + ".";
    }

    private String calculateHash(AcademicDocumentRequest entity, LocalDateTime signedAt) {
        String payload = String.join("|",
                entity.getDocumentCode(),
                entity.getStudent().getStudentNumber(),
                entity.getDocumentType(),
                entity.getDeclarationText(),
                entity.getSignatoryName(),
                entity.getSignatoryRole(),
                signedAt.toString(),
                String.valueOf(entity.getVersionNumber()),
                String.valueOf(entity.isDemoMode())
        );
        try {
            return HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível gerar o hash do documento.", exception);
        }
    }

    private AcademicDocumentDto.Response toResponse(AcademicDocumentRequest entity) {
        Student student = entity.getStudent();
        AcademicClass academicClass = student == null ? null : student.getAcademicClass();
        Course course = academicClass == null ? null : academicClass.getCourse();
        return new AcademicDocumentDto.Response(
                entity.getId(),
                entity.getDocumentCode(),
                student == null ? null : student.getId(),
                student == null ? null : student.getStudentNumber(),
                student == null ? null : student.getFullName(),
                student == null ? null : student.getDocumentNumber(),
                course == null ? null : course.getName(),
                academicClass == null ? null : academicClass.getName(),
                academicClass == null ? null : academicClass.getAcademicYear(),
                entity.getServiceCode(),
                entity.getDocumentType(),
                entity.getStatus(),
                entity.getPurpose(),
                entity.getDeclarationText(),
                entity.getSignatoryName(),
                entity.getSignatoryRole(),
                entity.getSignatureMethod(),
                entity.getDocumentHash(),
                entity.getVersionNumber(),
                entity.isDemoMode(),
                entity.getIssuedAt(),
                entity.getSignedAt(),
                entity.getSentAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                publicPdfUrl(entity),
                publicValidationUrl(entity)
        );
    }

    private String publicPdfUrl(AcademicDocumentRequest entity) {
        return apiBaseUrl + "/api/v1/public/academic-documents/" + entity.getDocumentCode() + "/pdf";
    }

    private String publicValidationUrl(AcademicDocumentRequest entity) {
        return apiBaseUrl + "/api/v1/public/academic-documents/" + entity.getDocumentCode() + "/validate";
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || isBlank(authentication.getName()) ? "SYSTEM" : authentication.getName();
    }

    private String sanitize(String value) {
        String safe = value == null ? "" : value.trim().replaceAll("[^A-Za-z0-9._-]", "-").replaceAll("-+", "-");
        return safe.isBlank() ? "documento" : safe;
    }

    private String stripTrailingSlash(String value) {
        String resolved = isBlank(value) ? "https://secretariapay-api.paixaoangola.com" : value.trim();
        return resolved.endsWith("/") ? resolved.substring(0, resolved.length() - 1) : resolved;
    }

    private String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (!isBlank(value)) return value.trim();
        }
        return "";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
