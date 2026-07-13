package com.secretariapay.api.service.whatsapp;

import com.secretariapay.api.dto.notification.GuideFallbackRequest;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import com.secretariapay.api.entity.financial.AcademicServiceCatalog;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.repository.academic.StudentRepository;
import com.secretariapay.api.repository.financial.AcademicServiceCatalogRepository;
import com.secretariapay.api.repository.financial.ChargeRepository;
import com.secretariapay.api.service.FallbackNotificationService;
import com.secretariapay.api.service.payment.AppyPayChargeResponse;
import com.secretariapay.api.service.payment.AppyPayPaymentGatewayService;
import com.secretariapay.api.service.payment.InfinitePayLinkPaymentResponse;
import com.secretariapay.api.service.payment.InfinitePayTestPaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AcademicServicesWhatsappFlowService {

    private static final int SESSION_MINUTES = 60;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter CODE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final List<String> WHATSAPP_SERVICE_CODES = List.of(
            "ENROLLMENT",
            "ENROLLMENT_CONFIRMATION",
            "REGISTRATION",
            "RESIT_EXAM",
            "SPECIAL_EXAM",
            "DECLARATION_WITH_GRADES",
            "DECLARATION_WITHOUT_GRADES",
            "CERTIFICATE",
            "DIPLOMA"
    );

    private final Map<String, AcademicServiceSession> sessions = new ConcurrentHashMap<>();
    private final AcademicServiceCatalogRepository catalogRepository;
    private final StudentRepository studentRepository;
    private final ChargeRepository chargeRepository;
    private final WhatsAppCloudApiClient whatsAppCloudApiClient;
    private final AppyPayPaymentGatewayService appyPayPaymentGatewayService;
    private final InfinitePayTestPaymentService infinitePayTestPaymentService;
    private final FallbackNotificationService fallbackNotificationService;
    private final String apiBaseUrl;
    private final String demoEmail;

    public AcademicServicesWhatsappFlowService(
            AcademicServiceCatalogRepository catalogRepository,
            StudentRepository studentRepository,
            ChargeRepository chargeRepository,
            WhatsAppCloudApiClient whatsAppCloudApiClient,
            AppyPayPaymentGatewayService appyPayPaymentGatewayService,
            InfinitePayTestPaymentService infinitePayTestPaymentService,
            FallbackNotificationService fallbackNotificationService,
            @Value("${secretariapay.public-api-base-url:https://secretariapay-api.paixaoangola.com}") String apiBaseUrl,
            @Value("${secretariapay.demo.email:dalawilson1244@gmail.com}") String demoEmail
    ) {
        this.catalogRepository = catalogRepository;
        this.studentRepository = studentRepository;
        this.chargeRepository = chargeRepository;
        this.whatsAppCloudApiClient = whatsAppCloudApiClient;
        this.appyPayPaymentGatewayService = appyPayPaymentGatewayService;
        this.infinitePayTestPaymentService = infinitePayTestPaymentService;
        this.fallbackNotificationService = fallbackNotificationService;
        this.apiBaseUrl = stripTrailingSlash(apiBaseUrl);
        this.demoEmail = isBlank(demoEmail) ? "dalawilson1244@gmail.com" : demoEmail.trim();
    }

    public Optional<String> handleIfActive(String fromPhone, String messageType, String rawMessage) {
        String phone = sanitizePhone(fromPhone);
        clearExpired(phone);

        AcademicServiceSession session = sessions.get(phone);
        if (session == null) return Optional.empty();

        String normalized = normalize(rawMessage);
        if (isMenu(normalized)) {
            sessions.remove(phone);
            return Optional.of(buildMainMenu());
        }

        if ("WAITING_SERVICE_CHOICE".equals(session.step())) {
            return Optional.of(handleServiceChoice(phone, normalized));
        }
        if ("WAITING_STUDENT".equals(session.step())) {
            return Optional.of(handleStudentIdentification(phone, session, rawMessage, normalized));
        }
        if ("WAITING_PAYMENT".equals(session.step())) {
            return Optional.of(handlePaymentChoice(phone, session, normalized));
        }
        if ("WAITING_PROOF".equals(session.step())) {
            if (isMedia(messageType) || normalized.contains("imagem recebida") || normalized.contains("documento recebido")) {
                sessions.remove(phone);
                return Optional.of("📎 Comprovativo recebido.\n\nO pagamento será analisado pela DCR. Após validação, o borderô financeiro será atualizado e disponibilizado neste WhatsApp.\n\nInstituto Superior Politécnico Metropolitano de Angola — IMETRO.");
            }
            return Optional.of("📎 Envie o comprovativo de pagamento em imagem ou PDF.\n\nA DCR fará a validação antes da emissão do recibo.");
        }
        if ("WAITING_BANK_CONFIRMATION".equals(session.step())) {
            return Optional.of("✅ A guia foi emitida para transferência entre contas BAI.\n\nEstado: aguardando confirmação bancária.\n\nO recibo será emitido após a confirmação do pagamento. Para outro atendimento, responda menu.");
        }
        if ("WAITING_APPYPAY_PAYMENT".equals(session.step())) {
            return Optional.of("✅ A cobrança já foi enviada para a AppyPay.\n\nEstado: aguardando confirmação do pagamento.\n\nO recibo será emitido após o retorno de sucesso. Para outro atendimento, responda menu.");
        }
        if ("WAITING_INFINITEPAY_PAYMENT".equals(session.step())) {
            return Optional.of("✅ O link InfinitePay já foi gerado para teste.\n\nEstado: aguardando pagamento.\n\nPara outro atendimento, responda menu.");
        }

        sessions.remove(phone);
        return Optional.of(buildMainMenu());
    }

    public String start(String fromPhone) {
        String phone = sanitizePhone(fromPhone);
        List<AcademicServiceCatalog> services = availableServices();
        if (services.isEmpty()) {
            sessions.remove(phone);
            return "⚠️ Os serviços académicos estão temporariamente indisponíveis. Fale com a DCR para continuar.";
        }
        sessions.put(phone, AcademicServiceSession.waitingServiceChoice());
        return buildServiceMenu(services);
    }

    public boolean isServiceIntent(String rawMessage) {
        String normalized = normalize(rawMessage);
        return containsAny(normalized,
                "servicos academicos",
                "serviços académicos",
                "servico academico",
                "serviço académico",
                "certificado",
                "diploma",
                "exame especial",
                "declaracao com nota",
                "declaração com nota",
                "declaracao sem nota",
                "declaração sem nota");
    }

    public String buildMainMenu() {
        return """
                Secretaria Pay (IMETRO) 👋

                Este canal é exclusivo para atendimento financeiro académico do IMETRO.

                Como posso ajudar?

                Responda com o número ou escreva o nome da opção:

                [1] Propinas
                [2] Situação Financeira
                [3] Borderô financeiro
                [4] Serviços académicos
                [5] Falar com a DCR
                """.trim();
    }

    private String handleServiceChoice(String phone, String normalized) {
        List<AcademicServiceCatalog> services = availableServices();
        Integer option = parseOption(normalized);
        int backOption = services.size() + 1;

        if (containsAny(normalized, "voltar", "menu") || (option != null && option == backOption)) {
            sessions.remove(phone);
            return buildMainMenu();
        }

        AcademicServiceCatalog selected = null;
        if (option != null && option >= 1 && option <= services.size()) {
            selected = services.get(option - 1);
        } else {
            for (AcademicServiceCatalog service : services) {
                if (normalized.equals(normalize(service.getName())) || normalized.contains(normalize(service.getName()))) {
                    selected = service;
                    break;
                }
            }
        }

        if (selected == null) return buildServiceMenu(services);

        sessions.put(phone, AcademicServiceSession.waitingStudent(selected.getCode()));
        return """
                📚 Serviço selecionado: %s
                Valor: %s

                Para continuar, informe um dado cadastrado no sistema:

                - Número de matrícula
                - Número do BI
                - Telefone cadastrado

                Exemplo: 202301404

                [1] Tentar novamente
                [2] Voltar ao menu principal
                """.formatted(selected.getName(), money(selected.getUnitPrice())).trim();
    }

    private String handleStudentIdentification(String phone, AcademicServiceSession session, String rawMessage, String normalized) {
        if ("2".equals(normalized) || containsAny(normalized, "voltar", "menu")) {
            sessions.remove(phone);
            return buildMainMenu();
        }
        if ("1".equals(normalized)) {
            return "Informe a matrícula, o BI ou o telefone cadastrado. Exemplo: 202301404";
        }

        Optional<Student> studentOptional = findRegisteredStudent(rawMessage, phone);
        if (studentOptional.isEmpty()) {
            return """
                    ⚠️ Não encontrei nenhum estudante com os dados informados.

                    [1] Tentar novamente
                    [2] Voltar ao menu principal
                    """.trim();
        }

        AcademicServiceCatalog service = loadAvailableService(session.serviceCode()).orElse(null);
        if (service == null) {
            sessions.remove(phone);
            return "⚠️ O serviço selecionado não está disponível neste momento. Fale com a DCR.";
        }

        Student student = studentOptional.get();
        Charge charge = createOrReuseCharge(student, service);
        AcademicServiceSession next = session.withStudentAndCharge(student, charge).withStep("WAITING_PAYMENT");
        sessions.put(phone, next);
        return buildPaymentMenu(next, service, charge);
    }

    private String handlePaymentChoice(String phone, AcademicServiceSession session, String normalized) {
        if ("5".equals(normalized) || containsAny(normalized, "voltar", "menu")) {
            sessions.remove(phone);
            return buildMainMenu();
        }

        AcademicServiceCatalog service = loadAvailableService(session.serviceCode()).orElse(null);
        Charge charge = session.chargeId() == null ? null : chargeRepository.findById(session.chargeId()).orElse(null);
        if (service == null || charge == null) {
            sessions.remove(phone);
            return "⚠️ Não foi possível recuperar a cobrança académica. Inicie novamente pelo menu.";
        }

        if ("1".equals(normalized) || containsAny(normalized, "multicaixa", "express")) {
            return createAppyPayCharge(phone, session, service, charge, true);
        }
        if ("2".equals(normalized) || containsAny(normalized, "referencia", "referência")) {
            return createAppyPayCharge(phone, session, service, charge, false);
        }
        if ("3".equals(normalized) || containsAny(normalized, "transferencia", "transferência", "mesmo banco", "bai")) {
            sendGuide(phone, session, service, charge, "Transferência mesmo banco");
            sessions.put(phone, session.withStep("WAITING_BANK_CONFIRMATION"));
            return """
                    ✅ Guia de pagamento criada.

                    Serviço: %s
                    Forma de pagamento: Transferência mesmo banco
                    Banco: Banco Angolano de Investimento — BAI
                    Valor a pagar: %s
                    Vencimento: %s

                    A guia de pagamento foi enviada neste WhatsApp.
                    ⏳ Aguardando confirmação bancária.

                    O recibo será emitido após a confirmação do pagamento.
                    """.formatted(service.getName(), money(charge.getTotalAmount()), formatDate(charge.getDueDate())).trim();
        }
        if ("4".equals(normalized) || containsAny(normalized, "deposito", "depósito", "outro banco")) {
            sendGuide(phone, session, service, charge, "Depósito ou transferência de outro banco");
            sessions.put(phone, session.withStep("WAITING_PROOF"));
            return """
                    ✅ Guia de pagamento criada.

                    Serviço: %s
                    Forma de pagamento: Depósito ou transferência de outro banco
                    Valor a pagar: %s
                    Vencimento: %s

                    A guia de pagamento foi enviada neste WhatsApp.
                    Após pagar, envie o comprovativo em imagem ou PDF para validação da DCR.
                    """.formatted(service.getName(), money(charge.getTotalAmount()), formatDate(charge.getDueDate())).trim();
        }
        if ("8".equals(normalized) || containsAny(normalized, "infinitepay", "teste brasil", "pix brasil")) {
            return createInfinitePayTest(phone, session, service, charge);
        }

        return buildPaymentMenu(session, service, charge);
    }

    private String createAppyPayCharge(String phone, AcademicServiceSession session, AcademicServiceCatalog service, Charge charge, boolean gpo) {
        String merchantTransactionId = "SPAY-" + charge.getChargeCode();
        AppyPayChargeResponse response = gpo
                ? appyPayPaymentGatewayService.createMulticaixaExpressCharge(charge.getTotalAmount(), service.getName(), merchantTransactionId, session.studentPhone())
                : appyPayPaymentGatewayService.createReferenceCharge(charge.getTotalAmount(), service.getName(), merchantTransactionId);

        if (!response.isSuccess()) {
            return """
                    ⚠️ Não foi possível criar a cobrança na AppyPay.

                    Serviço: %s
                    Valor: %s
                    Estado: %s
                    Mensagem: %s

                    Nenhum pagamento foi confirmado. Tente novamente ou fale com a DCR.
                    """.formatted(service.getName(), money(charge.getTotalAmount()), safe(response.getStatus()), safe(response.getMessage())).trim();
        }

        sendGuide(phone, session, service, charge, gpo ? "Multicaixa Express" : "Pagamento por Referência");
        sessions.put(phone, session.withStep("WAITING_APPYPAY_PAYMENT"));

        return """
                ✅ Guia de pagamento criada.

                Serviço: %s
                Forma de pagamento: %s
                Valor a pagar: %s
                Vencimento: %s
                Estado AppyPay: %s
                Referência da operação: %s

                A guia de pagamento foi enviada neste WhatsApp.
                ⏳ Aguardando confirmação do pagamento pela AppyPay.
                """.formatted(
                service.getName(),
                gpo ? "Multicaixa Express" : "Pagamento por Referência",
                money(charge.getTotalAmount()),
                formatDate(charge.getDueDate()),
                safe(response.getStatus()),
                safe(response.getMerchantTransactionId())
        ).trim();
    }

    private String createInfinitePayTest(String phone, AcademicServiceSession session, AcademicServiceCatalog service, Charge charge) {
        InfinitePayLinkPaymentResponse response = infinitePayTestPaymentService.createLink(
                phone,
                session.studentName(),
                session.studentNumber(),
                firstNonBlank(session.studentEmail(), demoEmail),
                charge.getReferenceMonth(),
                service.getName(),
                charge.getTotalAmount(),
                charge.getAmount(),
                charge.getFineAmount(),
                charge.getInterestAmount(),
                charge.getDueDate()
        );

        if (!response.isSuccess()) {
            return """
                    ⚠️ Não foi possível gerar o link InfinitePay.

                    Serviço: %s
                    Valor académico: %s
                    Estado: %s
                    Mensagem: %s
                    """.formatted(service.getName(), money(charge.getTotalAmount()), safe(response.getStatus()), safe(response.getMessage())).trim();
        }

        sendGuide(phone, session, service, charge, "InfinitePay Brasil — teste");
        sessions.put(phone, session.withStep("WAITING_INFINITEPAY_PAYMENT"));
        return """
                ✅ Link de pagamento gerado para teste.

                Serviço: %s
                Valor académico: %s
                Código do teste: %s

                Pague pelo link:
                %s

                A guia de pagamento foi enviada neste WhatsApp.
                """.formatted(service.getName(), money(charge.getTotalAmount()), safe(response.getOrderNsu()), safe(response.getCheckoutUrl())).trim();
    }

    private Charge createOrReuseCharge(Student student, AcademicServiceCatalog service) {
        String reference = academicReference(service);
        Optional<Charge> existing = chargeRepository.findByStudentIdOrderByDueDateDesc(student.getId()).stream()
                .filter(charge -> charge.getStatus() == ChargeStatus.PENDING
                        || charge.getStatus() == ChargeStatus.OVERDUE
                        || charge.getStatus() == ChargeStatus.PARTIALLY_PAID)
                .filter(charge -> reference.equalsIgnoreCase(safe(charge.getReferenceMonth())))
                .filter(charge -> service.getName().equalsIgnoreCase(safe(charge.getDescription())))
                .findFirst();
        if (existing.isPresent()) return existing.get();

        Charge charge = new Charge()
                .setStudent(student)
                .setChargeCode(buildChargeCode(service, student))
                .setDescription(service.getName())
                .setReferenceMonth(reference)
                .setDueDate(LocalDate.now().plusDays(3))
                .setAmount(service.getUnitPrice())
                .setFineAmount(BigDecimal.ZERO)
                .setInterestAmount(BigDecimal.ZERO)
                .setDiscountAmount(BigDecimal.ZERO)
                .setCurrency(firstNonBlank(service.getCurrency(), "AOA"))
                .setStatus(ChargeStatus.PENDING);
        return chargeRepository.save(charge);
    }

    private void sendGuide(String phone, AcademicServiceSession session, AcademicServiceCatalog service, Charge charge, String paymentMethod) {
        String pdfUrl = apiBaseUrl + "/api/v1/public/payment-guides/" + encode(charge.getChargeCode()) + "/pdf?v=" + System.currentTimeMillis();
        String publicUrl = apiBaseUrl + "/api/v1/public/payment-guides/" + encode(charge.getChargeCode()) + "/pdf";
        String fileName = "Guia_Pagamento_" + sanitizeFilePart(service.getCode()) + "_" + sanitizeFilePart(session.studentNumber()) + ".pdf";
        String caption = """
                SecretáriaPay Académico — guia de pagamento.

                Estudante: %s
                Matrícula: %s
                Serviço: %s
                Forma de pagamento: %s
                Valor: %s
                Vencimento: %s

                Consultar guia: %s
                """.formatted(
                session.studentName(),
                session.studentNumber(),
                service.getName(),
                paymentMethod,
                money(charge.getTotalAmount()),
                formatDate(charge.getDueDate()),
                publicUrl
        ).trim();
        whatsAppCloudApiClient.sendDocumentByLink(phone, pdfUrl, fileName, caption);

        GuideFallbackRequest email = new GuideFallbackRequest();
        email.setStudentName(session.studentName());
        email.setStudentNumber(session.studentNumber());
        email.setEmail(firstNonBlank(session.studentEmail(), demoEmail));
        email.setGuideCode(charge.getChargeCode());
        email.setGuideUrl(publicUrl);
        email.setAmount(charge.getTotalAmount());
        email.setCurrency(firstNonBlank(service.getCurrency(), "AOA"));
        email.setDueDate(charge.getDueDate());
        email.setMessage("Guia de pagamento referente ao serviço " + service.getName() + ".");
        fallbackNotificationService.sendGuideByEmail(email);
    }

    private String buildServiceMenu(List<AcademicServiceCatalog> services) {
        StringBuilder builder = new StringBuilder();
        builder.append("📚 Serviços académicos disponíveis.\n\n")
                .append("Escolha o serviço que deseja solicitar:\n\n");
        for (int index = 0; index < services.size(); index++) {
            AcademicServiceCatalog service = services.get(index);
            builder.append("[").append(index + 1).append("] ")
                    .append(service.getName())
                    .append(" — ")
                    .append(money(service.getUnitPrice()))
                    .append("\n");
        }
        builder.append("[").append(services.size() + 1).append("] Voltar ao menu principal\n\n")
                .append("Os valores são consultados diretamente na tabela institucional do IMETRO.");
        return builder.toString().trim();
    }

    private String buildPaymentMenu(AcademicServiceSession session, AcademicServiceCatalog service, Charge charge) {
        return """
                📄 Guia preparada.

                Estudante: %s
                Matrícula: %s
                Serviço: %s
                Valor: %s
                Vencimento: %s

                Escolha a forma de pagamento:

                [1] Multicaixa Express - AppyPay GPO
                [2] Pagamento por Referência - AppyPay REF
                [3] Transferência mesmo banco
                [4] Depósito bancário / transferência de outro banco
                [8] Teste real Brasil - InfinitePay
                [5] Voltar
                """.formatted(
                session.studentName(),
                session.studentNumber(),
                service.getName(),
                money(charge.getTotalAmount()),
                formatDate(charge.getDueDate())
        ).trim();
    }

    private List<AcademicServiceCatalog> availableServices() {
        List<AcademicServiceCatalog> result = new ArrayList<>();
        for (String code : WHATSAPP_SERVICE_CODES) {
            loadAvailableService(code).ifPresent(result::add);
        }
        result.sort(Comparator.comparingInt(item -> WHATSAPP_SERVICE_CODES.indexOf(item.getCode())));
        return result;
    }

    private Optional<AcademicServiceCatalog> loadAvailableService(String code) {
        return catalogRepository.findByCodeIgnoreCase(code)
                .filter(AcademicServiceCatalog::isActive)
                .filter(AcademicServiceCatalog::isAvailableWhatsapp)
                .filter(AcademicServiceCatalog::isGeneratesGuide)
                .filter(service -> service.getUnitPrice() != null && service.getUnitPrice().compareTo(BigDecimal.ZERO) > 0);
    }

    private Optional<Student> findRegisteredStudent(String input, String fromPhone) {
        String clean = safe(input).trim();
        if (!clean.isBlank()) {
            Optional<Student> byNumber = studentRepository.findByStudentNumber(clean);
            if (byNumber.isPresent()) return byNumber;
            Optional<Student> byDocument = studentRepository.findByDocumentNumberIgnoreCase(clean);
            if (byDocument.isPresent()) return byDocument;
            Optional<Student> byEmail = studentRepository.findByEmailIgnoreCase(clean);
            if (byEmail.isPresent()) return byEmail;
            Optional<Student> byPhone = findByPhoneVariants(clean);
            if (byPhone.isPresent()) return byPhone;
        }
        return findByPhoneVariants(fromPhone);
    }

    private Optional<Student> findByPhoneVariants(String rawPhone) {
        String digits = sanitizePhone(rawPhone);
        if (digits.isBlank()) return Optional.empty();
        List<String> variants = List.of(digits, "+" + digits, digits.replaceFirst("^244", "0"), digits.replaceFirst("^55", "0"));
        for (String variant : variants) {
            Optional<Student> byWhatsapp = studentRepository.findByWhatsapp(variant);
            if (byWhatsapp.isPresent()) return byWhatsapp;
            Optional<Student> byPhone = studentRepository.findByPhone(variant);
            if (byPhone.isPresent()) return byPhone;
            Optional<Student> byGuardian = studentRepository.findByGuardianPhone(variant);
            if (byGuardian.isPresent()) return byGuardian;
        }
        return Optional.empty();
    }

    private String academicReference(AcademicServiceCatalog service) {
        String year = String.valueOf(LocalDate.now().getYear());
        return switch (service.getCode()) {
            case "ENROLLMENT" -> "Matrícula " + year;
            case "ENROLLMENT_CONFIRMATION" -> "Conf. matrícula " + year;
            case "REGISTRATION" -> "Inscrição " + year;
            case "RESIT_EXAM" -> "Recurso " + year;
            case "SPECIAL_EXAM" -> "Exame especial " + year;
            case "DECLARATION_WITH_GRADES" -> "Decl. c/ nota " + year;
            case "DECLARATION_WITHOUT_GRADES" -> "Decl. s/ nota " + year;
            case "CERTIFICATE" -> "Certificado " + year;
            case "DIPLOMA" -> "Diploma " + year;
            default -> abbreviate(service.getName(), 15) + " " + year;
        };
    }

    private String buildChargeCode(AcademicServiceCatalog service, Student student) {
        String code = sanitizeFilePart(service.getCode());
        if (code.length() > 18) code = code.substring(0, 18);
        String studentPart = sanitizeFilePart(student.getStudentNumber());
        if (studentPart.length() > 12) studentPart = studentPart.substring(studentPart.length() - 12);
        String candidate = "IMT-" + code + "-" + LocalDate.now().format(CODE_DATE_FORMAT) + "-" + studentPart + "-" + shortId();
        return candidate.length() <= 60 ? candidate : candidate.substring(0, 60);
    }

    private void clearExpired(String phone) {
        AcademicServiceSession session = sessions.get(phone);
        if (session != null && session.expiresAt().isBefore(LocalDateTime.now())) sessions.remove(phone);
    }

    private boolean isMenu(String normalized) {
        return containsAny(normalized, "menu", "inicio", "início", "voltar ao menu");
    }

    private boolean isMedia(String type) {
        return "image".equalsIgnoreCase(type) || "document".equalsIgnoreCase(type);
    }

    private Integer parseOption(String value) {
        try {
            return Integer.parseInt(safe(value).trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
    }

    private boolean containsAny(String value, String... terms) {
        if (value == null || value.isBlank()) return false;
        for (String term : terms) {
            if (value.contains(normalize(term))) return true;
        }
        return false;
    }

    private String money(BigDecimal value) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        return String.format(Locale.forLanguageTag("pt-AO"), "%,.2f", safeValue)
                .replace(',', '#')
                .replace('.', ',')
                .replace('#', '.') + " Kz";
    }

    private String formatDate(LocalDate value) {
        return value == null ? "-" : value.format(DATE_FORMAT);
    }

    private String encode(String value) {
        return URLEncoder.encode(safe(value), StandardCharsets.UTF_8);
    }

    private String sanitizePhone(String value) {
        return value == null ? "" : value.replaceAll("[^0-9]", "");
    }

    private String sanitizeFilePart(String value) {
        String sanitized = safe(value).trim().replaceAll("[^A-Za-z0-9._-]", "-").replaceAll("-+", "-");
        return sanitized.isBlank() ? "documento" : sanitized;
    }

    private String stripTrailingSlash(String value) {
        String resolved = isBlank(value) ? "https://secretariapay-api.paixaoangola.com" : value.trim();
        return resolved.endsWith("/") ? resolved.substring(0, resolved.length() - 1) : resolved;
    }

    private String shortId() {
        return UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT);
    }

    private String abbreviate(String value, int maxLength) {
        String safe = safe(value).trim();
        return safe.length() <= maxLength ? safe : safe.substring(0, maxLength).trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (!isBlank(value)) return value.trim();
        }
        return "";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record AcademicServiceSession(
            String step,
            String serviceCode,
            String studentNumber,
            String studentName,
            String studentEmail,
            String studentPhone,
            UUID chargeId,
            LocalDateTime expiresAt
    ) {
        static AcademicServiceSession waitingServiceChoice() {
            return new AcademicServiceSession("WAITING_SERVICE_CHOICE", "", "", "", "", "", null, LocalDateTime.now().plusMinutes(SESSION_MINUTES));
        }

        static AcademicServiceSession waitingStudent(String serviceCode) {
            return new AcademicServiceSession("WAITING_STUDENT", serviceCode, "", "", "", "", null, LocalDateTime.now().plusMinutes(SESSION_MINUTES));
        }

        AcademicServiceSession withStudentAndCharge(Student student, Charge charge) {
            return new AcademicServiceSession(
                    step,
                    serviceCode,
                    student.getStudentNumber(),
                    student.getFullName(),
                    student.getEmail(),
                    firstNonBlankStatic(student.getWhatsapp(), student.getPhone(), student.getGuardianPhone()),
                    charge.getId(),
                    LocalDateTime.now().plusMinutes(SESSION_MINUTES)
            );
        }

        AcademicServiceSession withStep(String newStep) {
            return new AcademicServiceSession(newStep, serviceCode, studentNumber, studentName, studentEmail, studentPhone, chargeId, LocalDateTime.now().plusMinutes(SESSION_MINUTES));
        }

        private static String firstNonBlankStatic(String... values) {
            if (values == null) return "";
            for (String value : values) {
                if (value != null && !value.isBlank()) return value.trim();
            }
            return "";
        }
    }
}
