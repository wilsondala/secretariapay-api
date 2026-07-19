package com.secretariapay.api.service.academic;

import com.secretariapay.api.dto.academic.AcademicDocumentDto;
import com.secretariapay.api.dto.academic.AcademicServiceOrderDto;
import com.secretariapay.api.dto.whatsapp.WhatsAppCloudSendResult;
import com.secretariapay.api.entity.academic.AcademicClass;
import com.secretariapay.api.entity.academic.AcademicDocumentRequest;
import com.secretariapay.api.entity.academic.AcademicServiceOrder;
import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.academic.AcademicServiceOrderStatus;
import com.secretariapay.api.entity.enums.financial.ChargeCategory;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import com.secretariapay.api.entity.financial.AcademicServiceCatalog;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.academic.AcademicDocumentRequestRepository;
import com.secretariapay.api.repository.academic.AcademicServiceOrderRepository;
import com.secretariapay.api.repository.academic.StudentRepository;
import com.secretariapay.api.repository.financial.AcademicServiceCatalogRepository;
import com.secretariapay.api.repository.financial.ChargeRepository;
import com.secretariapay.api.service.whatsapp.WhatsAppCloudApiClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
public class AcademicServiceOrderService {

    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AcademicServiceOrderRepository orderRepository;
    private final StudentRepository studentRepository;
    private final AcademicServiceCatalogRepository catalogRepository;
    private final ChargeRepository chargeRepository;
    private final AcademicDocumentRequestRepository documentRepository;
    private final AcademicDocumentService academicDocumentService;
    private final WhatsAppCloudApiClient whatsAppCloudApiClient;

    public AcademicServiceOrderService(
            AcademicServiceOrderRepository orderRepository,
            StudentRepository studentRepository,
            AcademicServiceCatalogRepository catalogRepository,
            ChargeRepository chargeRepository,
            AcademicDocumentRequestRepository documentRepository,
            AcademicDocumentService academicDocumentService,
            WhatsAppCloudApiClient whatsAppCloudApiClient
    ) {
        this.orderRepository = orderRepository;
        this.studentRepository = studentRepository;
        this.catalogRepository = catalogRepository;
        this.chargeRepository = chargeRepository;
        this.documentRepository = documentRepository;
        this.academicDocumentService = academicDocumentService;
        this.whatsAppCloudApiClient = whatsAppCloudApiClient;
    }

    @Transactional(readOnly = true)
    public List<AcademicServiceOrderDto.Response> list(AcademicServiceOrderStatus status, UUID studentId) {
        List<AcademicServiceOrder> orders;
        if (studentId != null) {
            orders = orderRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
        } else if (status != null) {
            orders = orderRepository.findByStatusOrderByCreatedAtAsc(status);
        } else {
            orders = orderRepository.findAllByOrderByCreatedAtDesc();
        }
        return orders.stream()
                .filter(order -> status == null || order.getStatus() == status)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AcademicServiceOrderDto.Response> archive() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(order -> order.getStatus() != null && order.getStatus().isArchived())
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AcademicServiceOrderDto.Response findById(UUID id) {
        return toResponse(load(id));
    }

    @Transactional
    public AcademicServiceOrderDto.Response create(AcademicServiceOrderDto.CreateRequest request) {
        if (request == null || request.studentId() == null || request.serviceId() == null) {
            throw new IllegalArgumentException("Estudante e serviço académico são obrigatórios.");
        }

        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new NotFoundException("Estudante não encontrado."));
        AcademicServiceCatalog service = catalogRepository.findById(request.serviceId())
                .orElseThrow(() -> new NotFoundException("Serviço académico não encontrado."));
        if (!service.isActive()) {
            throw new IllegalStateException("O serviço académico selecionado está inativo.");
        }

        AcademicServiceOrder order = new AcademicServiceOrder()
                .setOrderCode(nextOrderCode())
                .setStudent(student)
                .setService(service)
                .setStatus(AcademicServiceOrderStatus.SOLICITADO)
                .setPurpose(firstNonBlank(request.purpose(), "Solicitação de " + service.getName()))
                .setNotes(trimToNull(request.notes()))
                .setRequestedAt(LocalDateTime.now())
                .setRequestedBy(currentActor());

        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public AcademicServiceOrderDto.Response requestPayment(UUID id, AcademicServiceOrderDto.RequestPaymentRequest request) {
        AcademicServiceOrder order = load(id);
        requireStatus(order, AcademicServiceOrderStatus.SOLICITADO);
        AcademicServiceCatalog service = order.getService();
        if (service.getUnitPrice() == null || service.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("O serviço não possui preço válido configurado.");
        }
        LocalDate dueDate = request == null || request.dueDate() == null
                ? LocalDate.now().plusDays(3)
                : request.dueDate();
        if (dueDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("A data de vencimento não pode estar no passado.");
        }

        Charge charge = new Charge()
                .setStudent(order.getStudent())
                .setChargeCode(nextChargeCode(service.getCode()))
                .setDescription(service.getName())
                .setReferenceMonth(null)
                .setChargeCategory(ChargeCategory.ACADEMIC_SERVICE)
                .setServiceCode(service.getCode())
                .setDueDate(dueDate)
                .setAmount(service.getUnitPrice())
                .setFineAmount(BigDecimal.ZERO)
                .setInterestAmount(BigDecimal.ZERO)
                .setDiscountAmount(BigDecimal.ZERO)
                .setCurrency(firstNonBlank(service.getCurrency(), "AOA"))
                .setStatus(ChargeStatus.PENDING);

        order.setCharge(chargeRepository.save(charge))
                .setStatus(AcademicServiceOrderStatus.AGUARDANDO_PAGAMENTO)
                .setPaymentRequestedAt(LocalDateTime.now());
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public void confirmPaymentByCharge(Charge charge) {
        if (charge == null || charge.getId() == null || charge.getStatus() != ChargeStatus.PAID) return;
        orderRepository.findByChargeId(charge.getId()).ifPresent(order -> {
            if (order.getStatus() == AcademicServiceOrderStatus.AGUARDANDO_PAGAMENTO) {
                order.setStatus(AcademicServiceOrderStatus.PAGO)
                        .setPaymentConfirmedAt(firstNonNull(charge.getPaidAt(), LocalDateTime.now()));
                orderRepository.save(order);
            }
        });
    }

    @Transactional
    public AcademicServiceOrderDto.Response generateDocument(UUID id) {
        AcademicServiceOrder order = load(id);
        requireStatus(order, AcademicServiceOrderStatus.PAGO);
        ensurePaid(order);

        AcademicDocumentRequest document = new AcademicDocumentRequest()
                .setDocumentCode(nextDocumentCode(order))
                .setStudent(order.getStudent())
                .setServiceCode(order.getService().getCode())
                .setDocumentType(resolveDocumentType(order.getService().getCode()))
                .setStatus("DRAFT")
                .setPurpose(firstNonBlank(order.getPurpose(), "Emissão de " + order.getService().getName()))
                .setDeclarationText(buildDocumentText(order))
                .setSignatoryName("Zakeu António Zengo")
                .setSignatoryRole("Presidente da Instituição")
                .setDemoMode(true)
                .setVersionNumber(1)
                .setIssuedAt(LocalDateTime.now())
                .setCreatedBy(currentActor());

        order.setDocumentRequest(documentRepository.save(document))
                .setStatus(AcademicServiceOrderStatus.DOCUMENTO_GERADO)
                .setDocumentGeneratedAt(LocalDateTime.now());
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public AcademicServiceOrderDto.Response markReadyForPrint(UUID id) {
        AcademicServiceOrder order = load(id);
        requireStatus(order, AcademicServiceOrderStatus.DOCUMENTO_GERADO);
        ensureDocument(order);
        order.setStatus(AcademicServiceOrderStatus.PRONTO_PARA_IMPRESSAO)
                .setReadyForPrintAt(LocalDateTime.now());
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public AcademicServiceOrderDto.Response markPrinted(UUID id) {
        AcademicServiceOrder order = load(id);
        requireStatus(order, AcademicServiceOrderStatus.PRONTO_PARA_IMPRESSAO);
        ensureDocument(order);
        order.setStatus(AcademicServiceOrderStatus.IMPRESSO)
                .setPrintedAt(LocalDateTime.now())
                .setPrintedBy(currentActor());
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public AcademicServiceOrderDto.Response submitForSignature(UUID id) {
        AcademicServiceOrder order = load(id);
        requireStatus(order, AcademicServiceOrderStatus.IMPRESSO);
        ensureDocument(order);
        academicDocumentService.markReadyForSignature(order.getDocumentRequest().getId());
        order.setStatus(AcademicServiceOrderStatus.AGUARDANDO_ASSINATURA)
                .setWaitingSignatureAt(LocalDateTime.now());
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public AcademicServiceOrderDto.Response sign(UUID id) {
        AcademicServiceOrder order = load(id);
        requireStatus(order, AcademicServiceOrderStatus.AGUARDANDO_ASSINATURA);
        ensureDocument(order);
        AcademicDocumentDto.Response signed = academicDocumentService.signDemo(order.getDocumentRequest().getId());
        order.setStatus(AcademicServiceOrderStatus.ASSINADO)
                .setSignedAt(firstNonNull(signed.signedAt(), LocalDateTime.now()))
                .setSignedBy(currentActor());
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public AcademicServiceOrderDto.Response markReadyForPickup(UUID id, AcademicServiceOrderDto.ActionRequest request) {
        AcademicServiceOrder order = load(id);
        requireStatus(order, AcademicServiceOrderStatus.ASSINADO);
        ensureDocument(order);
        String location = firstNonBlank(request == null ? null : request.physicalLocation(), "Secretaria Académica do IMETRO");
        order.setPhysicalLocation(location)
                .setStatus(AcademicServiceOrderStatus.PRONTO_PARA_LEVANTAMENTO)
                .setReadyForPickupAt(LocalDateTime.now());
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public AcademicServiceOrderDto.Response sendPickupWhatsapp(UUID id) {
        AcademicServiceOrder order = load(id);
        requireStatus(order, AcademicServiceOrderStatus.PRONTO_PARA_LEVANTAMENTO);
        Student student = order.getStudent();
        String recipient = firstNonBlank(student.getWhatsapp(), student.getPhone(), student.getGuardianPhone());
        if (isBlank(recipient)) {
            throw new IllegalStateException("O estudante não possui WhatsApp ou telefone cadastrado.");
        }

        String message = """
                Caro(a) estudante,

                Informamos que o seu documento já se encontra assinado e disponível para levantamento.

                Pedido: %s
                Documento: %s
                Matrícula: %s
                Local de levantamento: %s

                Apresente um documento de identificação no momento do levantamento.

                Atenciosamente,
                Secretaria Académica
                IMETRO
                """.formatted(
                order.getOrderCode(),
                order.getService().getName(),
                student.getStudentNumber(),
                firstNonBlank(order.getPhysicalLocation(), "Secretaria Académica do IMETRO")
        ).trim();

        WhatsAppCloudSendResult result = whatsAppCloudApiClient.sendText(recipient, message);
        if (!result.isSuccess()) {
            throw new IllegalStateException(firstNonBlank(result.getErrorMessage(), "Não foi possível enviar a notificação pelo WhatsApp."));
        }

        order.setStatus(AcademicServiceOrderStatus.WHATSAPP_ENVIADO)
                .setWhatsappSentAt(LocalDateTime.now())
                .setWhatsappSentBy(currentActor());
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public AcademicServiceOrderDto.Response deliver(UUID id, AcademicServiceOrderDto.ActionRequest request) {
        AcademicServiceOrder order = load(id);
        requireStatus(order, AcademicServiceOrderStatus.WHATSAPP_ENVIADO);
        String recipientName = trimToNull(request == null ? null : request.recipientName());
        String recipientDocument = trimToNull(request == null ? null : request.recipientDocumentNumber());
        if (recipientName == null || recipientDocument == null) {
            throw new IllegalArgumentException("Nome e documento de identificação de quem levantou são obrigatórios.");
        }

        order.setStatus(AcademicServiceOrderStatus.ENTREGUE)
                .setDeliveredAt(LocalDateTime.now())
                .setDeliveredBy(currentActor())
                .setRecipientName(recipientName)
                .setRecipientDocumentNumber(recipientDocument)
                .setDeliveryNotes(trimToNull(request.notes()));
        return toResponse(orderRepository.save(order));
    }

    private AcademicServiceOrder load(UUID id) {
        return orderRepository.findOneById(id)
                .orElseThrow(() -> new NotFoundException("Pedido de serviço académico não encontrado."));
    }

    private void ensurePaid(AcademicServiceOrder order) {
        if (order.getCharge() == null || order.getCharge().getStatus() != ChargeStatus.PAID) {
            throw new IllegalStateException("O documento só pode entrar na fila da Secretaria depois do pagamento confirmado.");
        }
    }

    private void ensureDocument(AcademicServiceOrder order) {
        if (order.getDocumentRequest() == null || order.getDocumentRequest().getId() == null) {
            throw new IllegalStateException("O pedido ainda não possui documento gerado.");
        }
    }

    private void requireStatus(AcademicServiceOrder order, AcademicServiceOrderStatus expected) {
        if (order.getStatus() != expected) {
            throw new IllegalStateException("Transição inválida. Estado atual: " + order.getStatus() + "; estado esperado: " + expected + ".");
        }
    }

    private String nextOrderCode() {
        String code;
        do {
            code = "IMT-SRV-" + LocalDate.now().format(CODE_DATE) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        } while (orderRepository.existsByOrderCode(code));
        return code;
    }

    private String nextChargeCode(String serviceCode) {
        String safeService = firstNonBlank(serviceCode, "OTHER").toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "-");
        String code;
        do {
            code = "IMT-SERVICO-" + safeService + "-" + System.currentTimeMillis();
            if (code.length() > 60) code = code.substring(0, 60);
        } while (chargeRepository.existsByChargeCode(code));
        return code;
    }

    private String nextDocumentCode(AcademicServiceOrder order) {
        String student = sanitize(order.getStudent().getStudentNumber());
        if (student.length() > 18) student = student.substring(student.length() - 18);
        return "IMT-DOC-" + LocalDate.now().format(CODE_DATE) + "-" + student + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    private String resolveDocumentType(String serviceCode) {
        String code = firstNonBlank(serviceCode, "OTHER").toUpperCase(Locale.ROOT);
        return switch (code) {
            case "DECLARATION_WITH_GRADES" -> "DECLARATION_WITH_GRADES";
            case "DECLARATION_WITHOUT_GRADES" -> "SIMPLE_DECLARATION";
            case "CERTIFICATE" -> "CERTIFICATE";
            case "DIPLOMA" -> "DIPLOMA";
            default -> "ACADEMIC_SERVICE_DOCUMENT";
        };
    }

    private String buildDocumentText(AcademicServiceOrder order) {
        Student student = order.getStudent();
        AcademicClass academicClass = student.getAcademicClass();
        Course course = academicClass == null ? null : academicClass.getCourse();
        String courseName = course == null ? "curso registado na instituição" : course.getName();
        String academicYear = academicClass == null ? String.valueOf(LocalDate.now().getYear()) : academicClass.getAcademicYear();
        return "O Instituto Superior Politécnico Metropolitano de Angola - IMETRO declara, para os devidos efeitos, que "
                + student.getFullName() + ", titular da matrícula n.º " + student.getStudentNumber()
                + ", frequenta o curso de " + courseName + " no ano académico de " + academicYear
                + ". O presente documento corresponde ao serviço académico \"" + order.getService().getName() + "\""
                + " solicitado sob o código " + order.getOrderCode() + ".";
    }

    private AcademicServiceOrderDto.Response toResponse(AcademicServiceOrder order) {
        Student student = order.getStudent();
        AcademicClass academicClass = student == null ? null : student.getAcademicClass();
        Course course = academicClass == null ? null : academicClass.getCourse();
        AcademicServiceCatalog service = order.getService();
        Charge charge = order.getCharge();
        AcademicDocumentRequest document = order.getDocumentRequest();
        return new AcademicServiceOrderDto.Response(
                order.getId(),
                order.getOrderCode(),
                order.getStatus(),
                student == null ? null : student.getId(),
                student == null ? null : student.getStudentNumber(),
                student == null ? null : student.getFullName(),
                student == null ? null : firstNonBlank(student.getWhatsapp(), student.getPhone()),
                course == null ? null : course.getName(),
                service == null ? null : service.getId(),
                service == null ? null : service.getCode(),
                service == null ? null : service.getName(),
                service == null ? null : service.getCategory(),
                charge != null ? charge.getTotalAmount() : service == null ? null : service.getUnitPrice(),
                charge != null ? charge.getCurrency() : service == null ? null : service.getCurrency(),
                charge == null ? null : charge.getId(),
                charge == null ? null : charge.getChargeCode(),
                charge == null ? null : charge.getStatus(),
                document == null ? null : document.getId(),
                document == null ? null : document.getDocumentCode(),
                document == null ? null : document.getDocumentType(),
                order.getPurpose(),
                order.getNotes(),
                order.getPhysicalLocation(),
                order.getRequestedBy(),
                order.getPrintedBy(),
                order.getSignedBy(),
                order.getWhatsappSentBy(),
                order.getDeliveredBy(),
                order.getRecipientName(),
                order.getRecipientDocumentNumber(),
                order.getDeliveryNotes(),
                order.getRequestedAt(),
                order.getPaymentRequestedAt(),
                order.getPaymentConfirmedAt(),
                order.getDocumentGeneratedAt(),
                order.getReadyForPrintAt(),
                order.getPrintedAt(),
                order.getWaitingSignatureAt(),
                order.getSignedAt(),
                order.getReadyForPickupAt(),
                order.getWhatsappSentAt(),
                order.getDeliveredAt(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || isBlank(authentication.getName()) ? "SYSTEM" : authentication.getName();
    }

    private String sanitize(String value) {
        String safe = value == null ? "" : value.trim().replaceAll("[^A-Za-z0-9._-]", "-").replaceAll("-+", "-");
        return safe.isBlank() ? "estudante" : safe;
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (!isBlank(value)) return value.trim();
        }
        return "";
    }

    private LocalDateTime firstNonNull(LocalDateTime value, LocalDateTime fallback) {
        return value == null ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}