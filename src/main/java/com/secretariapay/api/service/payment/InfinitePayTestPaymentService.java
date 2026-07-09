package com.secretariapay.api.service.payment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secretariapay.api.config.InfinitePayProperties;
import com.secretariapay.api.dto.financial.ReceiptResponse;
import com.secretariapay.api.dto.notification.GuideFallbackRequest;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.repository.academic.StudentRepository;
import com.secretariapay.api.repository.financial.ChargeRepository;
import com.secretariapay.api.service.FallbackNotificationService;
import com.secretariapay.api.service.financial.ReceiptService;
import com.secretariapay.api.service.whatsapp.WhatsAppCloudApiClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InfinitePayTestPaymentService {

    private static final String API_BASE_URL = "https://secretariapay-api.paixaoangola.com";

    private final InfinitePayProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final WhatsAppCloudApiClient whatsAppCloudApiClient;
    private final FallbackNotificationService fallbackNotificationService;
    private final StudentRepository studentRepository;
    private final ChargeRepository chargeRepository;
    private final ReceiptService receiptService;
    private final Map<String, PendingInfinitePayPayment> pendingPayments = new ConcurrentHashMap<>();

    public InfinitePayTestPaymentService(
            InfinitePayProperties properties,
            RestClient.Builder restClientBuilder,
            WhatsAppCloudApiClient whatsAppCloudApiClient,
            FallbackNotificationService fallbackNotificationService,
            StudentRepository studentRepository,
            ChargeRepository chargeRepository,
            ReceiptService receiptService
    ) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
        this.objectMapper = new ObjectMapper();
        this.whatsAppCloudApiClient = whatsAppCloudApiClient;
        this.fallbackNotificationService = fallbackNotificationService;
        this.studentRepository = studentRepository;
        this.chargeRepository = chargeRepository;
        this.receiptService = receiptService;
    }

    public boolean isEnabled() {
        return properties.isEnabled() && !properties.getHandle().isBlank();
    }

    public BigDecimal getTestAmountBrl() {
        return properties.getTestAmountBrl();
    }

    public InfinitePayLinkPaymentResponse createLink(
            String whatsappPhone,
            String studentName,
            String studentNumber,
            String email,
            String referenceMonth,
            String serviceName,
            BigDecimal academicAmountAoa,
            BigDecimal baseAmountAoa,
            BigDecimal fineAmountAoa,
            BigDecimal interestAmountAoa,
            LocalDate dueDate,
            List<InfinitePayAcademicLine> academicLines
    ) {
        BigDecimal amountBrl = normalizeAmount(properties.getTestAmountBrl());
        String orderNsu = generateOrderNsu();
        List<InfinitePayAcademicLine> safeLines = normalizeLines(referenceMonth, serviceName, academicAmountAoa, baseAmountAoa, fineAmountAoa, interestAmountAoa, dueDate, academicLines);

        if (!properties.isEnabled()) {
            return prepared(orderNsu, amountBrl, "InfinitePay está desativado por configuração.");
        }

        if (properties.getHandle().isBlank()) {
            return prepared(orderNsu, amountBrl, "INFINITEPAY_HANDLE não configurado no servidor.");
        }

        try {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("quantity", 1);
            item.put("price", amountBrl.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValue());
            item.put("description", "Teste real SecretáriaPay - " + safe(serviceName));

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("handle", properties.getHandle());
            payload.put("redirect_url", buildSuccessUrl(orderNsu));
            payload.put("order_nsu", orderNsu);
            payload.put("items", List.of(item));

            String raw = restClient.post()
                    .uri(properties.getLinksUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> providerData = parseJsonMap(raw);
            String checkoutUrl = extractCheckoutUrl(providerData);
            if (checkoutUrl.isBlank()) {
                return new InfinitePayLinkPaymentResponse()
                        .setSuccess(false)
                        .setEnabled(true)
                        .setStatus("FAILED")
                        .setOrderNsu(orderNsu)
                        .setAmountBrl(amountBrl)
                        .setMessage("InfinitePay não retornou link de checkout.")
                        .setRawResponse(raw)
                        .setProviderData(providerData);
            }

            PendingInfinitePayPayment pending = new PendingInfinitePayPayment(
                    orderNsu,
                    safe(whatsappPhone),
                    safe(studentName),
                    safe(studentNumber),
                    safe(email),
                    safe(referenceMonth),
                    safe(serviceName),
                    safeAmount(academicAmountAoa),
                    safeAmount(baseAmountAoa),
                    safeAmount(fineAmountAoa),
                    safeAmount(interestAmountAoa),
                    amountBrl,
                    dueDate == null ? LocalDate.now() : dueDate,
                    safeLines,
                    LocalDateTime.now().plusHours(3)
            );
            pendingPayments.put(orderNsu, pending);

            return new InfinitePayLinkPaymentResponse()
                    .setSuccess(true)
                    .setEnabled(true)
                    .setStatus("PENDING")
                    .setOrderNsu(orderNsu)
                    .setCheckoutUrl(checkoutUrl)
                    .setAmountBrl(amountBrl)
                    .setMessage("Link InfinitePay criado com sucesso.")
                    .setRawResponse(raw)
                    .setProviderData(providerData);
        } catch (RestClientResponseException exception) {
            return new InfinitePayLinkPaymentResponse()
                    .setSuccess(false)
                    .setEnabled(true)
                    .setStatus("FAILED")
                    .setOrderNsu(orderNsu)
                    .setAmountBrl(amountBrl)
                    .setMessage("Falha InfinitePay HTTP " + exception.getStatusCode().value() + ": " + safeBody(exception.getResponseBodyAsString()));
        } catch (Exception exception) {
            return new InfinitePayLinkPaymentResponse()
                    .setSuccess(false)
                    .setEnabled(true)
                    .setStatus("FAILED")
                    .setOrderNsu(orderNsu)
                    .setAmountBrl(amountBrl)
                    .setMessage("Falha ao criar link InfinitePay: " + exception.getMessage());
        }
    }

    public Optional<PendingInfinitePayPayment> findPending(String orderNsu) {
        if (orderNsu == null || orderNsu.isBlank()) return Optional.empty();
        return Optional.ofNullable(pendingPayments.get(orderNsu.trim()));
    }

    @Transactional
    public InfinitePayConfirmationResult confirmBySuccessReturn(String orderNsu) {
        if (orderNsu == null || orderNsu.isBlank()) {
            return new InfinitePayConfirmationResult(false, "order_nsu não informado.", null, null);
        }

        PendingInfinitePayPayment pending = pendingPayments.remove(orderNsu.trim());
        if (pending == null) {
            return new InfinitePayConfirmationResult(false, "Pagamento não encontrado ou já processado.", orderNsu, null);
        }

        List<PersistedAcademicPayment> persistedPayments = persistAcademicPayments(pending);

        if (persistedPayments.isEmpty()) {
            String receiptCode = "BORD-IP-" + shortId();
            String pdfUrl = API_BASE_URL + "/api/v1/public/demo/receipts/" + encode(receiptCode) + "/pdf"
                    + "?student=" + encode(pending.studentName())
                    + "&month=" + encode(pending.referenceMonth())
                    + "&method=" + encode("InfinitePay Brasil - teste real");
            sendFallbackDemoReceipt(pending, receiptCode, pdfUrl);
            return new InfinitePayConfirmationResult(true, "Pagamento InfinitePay confirmado, mas não foi possível vincular ao estudante no banco. Bordereau demo enviado.", pending.orderNsu(), receiptCode);
        }

        String receipts = joinReceiptCodes(persistedPayments);
        sendPersistedReceipts(pending, persistedPayments);

        return new InfinitePayConfirmationResult(true, "Pagamento InfinitePay confirmado, mensalidades gravadas no painel e bordereaux enviados.", pending.orderNsu(), receipts);
    }

    public InfinitePayConfirmationResult cancel(String orderNsu) {
        if (orderNsu == null || orderNsu.isBlank()) {
            return new InfinitePayConfirmationResult(false, "order_nsu não informado.", null, null);
        }
        pendingPayments.remove(orderNsu.trim());
        return new InfinitePayConfirmationResult(true, "Pagamento InfinitePay cancelado pelo cliente.", orderNsu, null);
    }

    private List<PersistedAcademicPayment> persistAcademicPayments(PendingInfinitePayPayment pending) {
        Optional<Student> studentOptional = studentRepository.findByStudentNumber(pending.studentNumber());
        if (studentOptional.isEmpty()) {
            return List.of();
        }

        Student student = studentOptional.get();
        List<PersistedAcademicPayment> persisted = new ArrayList<>();

        for (InfinitePayAcademicLine line : pending.academicLines()) {
            Charge charge = new Charge()
                    .setStudent(student)
                    .setChargeCode(generateChargeCode())
                    .setDescription("Teste real InfinitePay - " + firstNonBlank(line.description(), pending.serviceName()))
                    .setReferenceMonth(line.referenceMonth())
                    .setDueDate(line.dueDate() == null ? pending.dueDate() : line.dueDate())
                    .setAmount(safeAmount(line.baseAmountAoa()))
                    .setFineAmount(safeAmount(line.fineAmountAoa()))
                    .setInterestAmount(safeAmount(line.interestAmountAoa()))
                    .setDiscountAmount(BigDecimal.ZERO)
                    .setCurrency("AOA")
                    .setStatus(ChargeStatus.PAID)
                    .setPaidAt(LocalDateTime.now());

            Charge savedCharge = chargeRepository.save(charge);
            ReceiptResponse receipt = receiptService.issueOrFindForCharge(savedCharge.getId());
            persisted.add(new PersistedAcademicPayment(line, savedCharge, receipt));
        }

        return persisted;
    }

    private void sendPersistedReceipts(PendingInfinitePayPayment pending, List<PersistedAcademicPayment> persistedPayments) {
        String summary = buildPaidMonthsSummary(persistedPayments);

        for (PersistedAcademicPayment payment : persistedPayments) {
            ReceiptResponse receipt = payment.receipt();
            String pdfUrl = firstNonBlank(receipt.getPdfUrl(), API_BASE_URL + "/api/v1/public/receipts/" + receipt.getReceiptCode() + "/pdf");
            InfinitePayAcademicLine line = payment.line();

            String caption = ("""
                    ✅ Pagamento confirmado via InfinitePay Brasil.

                    SecretáriaPay Académico: bordereau/comprovativo emitido.

                    Bordereau: %s
                    Estudante: %s
                    Matrícula: %s
                    Mês/serviço: %s
                    Base: %s
                    Multa: %s
                    Juros: %s
                    Total académico: %s
                    Valor teste Brasil: %s
                    Código teste: %s
                    """).formatted(
                    receipt.getReceiptCode(),
                    pending.studentName(),
                    pending.studentNumber(),
                    line.referenceMonth(),
                    moneyAoa(line.baseAmountAoa()),
                    moneyAoa(line.fineAmountAoa()),
                    moneyAoa(line.interestAmountAoa()),
                    moneyAoa(line.totalAmountAoa()),
                    moneyBrl(pending.amountBrl()),
                    pending.orderNsu()
            ).trim();

            if (!pending.whatsappPhone().isBlank()) {
                whatsAppCloudApiClient.sendDocumentByLink(pending.whatsappPhone(), pdfUrl, "bordereau-" + receipt.getReceiptCode() + ".pdf", caption);
            }

            sendReceiptEmail(pending, payment, pdfUrl);
        }

        if (!pending.whatsappPhone().isBlank()) {
            whatsAppCloudApiClient.sendText(pending.whatsappPhone(), ("""
                    ✅ Pagamento confirmado com sucesso pela InfinitePay.

                    📄 O pagamento foi gravado no painel financeiro do SecretáriaPay.

                    Forma de pagamento: InfinitePay Brasil - teste real
                    Valor teste Brasil recebido: %s
                    Código teste: %s

                    Meses/serviços baixados no painel:
                    %s

                    Enviei os bordereaux em PDF neste WhatsApp.
                    📧 Também enviei cópia para o e-mail cadastrado.
                    """).formatted(
                    moneyBrl(pending.amountBrl()),
                    pending.orderNsu(),
                    summary
            ).trim());
        }
    }

    private void sendFallbackDemoReceipt(PendingInfinitePayPayment pending, String receiptCode, String pdfUrl) {
        String caption = ("""
                ✅ Pagamento confirmado via InfinitePay Brasil.

                SecretáriaPay Académico: bordereau/comprovativo demo emitido.

                Bordereau: %s
                Estudante: %s
                Matrícula: %s
                Referência académica: %s
                Valor académico: %s
                Valor teste Brasil: %s
                Provedor: InfinitePay
                Código teste: %s
                """).formatted(
                receiptCode,
                pending.studentName(),
                pending.studentNumber(),
                pending.referenceMonth(),
                moneyAoa(pending.academicAmountAoa()),
                moneyBrl(pending.amountBrl()),
                pending.orderNsu()
        ).trim();

        if (!pending.whatsappPhone().isBlank()) {
            whatsAppCloudApiClient.sendDocumentByLink(pending.whatsappPhone(), pdfUrl, "bordereau-" + receiptCode + ".pdf", caption);
            whatsAppCloudApiClient.sendText(pending.whatsappPhone(), ("""
                    ✅ Pagamento confirmado com sucesso pela InfinitePay.

                    📄 O bordereau/comprovativo demo foi emitido automaticamente.

                    Atenção: o estudante não foi encontrado no banco pelo número de matrícula, por isso esta baixa não apareceu no painel financeiro.
                    Matrícula usada: %s
                    """).formatted(pending.studentNumber()).trim());
        }

        sendFallbackReceiptEmail(pending, receiptCode, pdfUrl);
    }

    private void sendReceiptEmail(PendingInfinitePayPayment pending, PersistedAcademicPayment payment, String pdfUrl) {
        InfinitePayAcademicLine line = payment.line();
        ReceiptResponse receipt = payment.receipt();

        GuideFallbackRequest request = new GuideFallbackRequest();
        request.setStudentName(pending.studentName());
        request.setStudentNumber(pending.studentNumber());
        request.setEmail(pending.email());
        request.setGuideCode(receipt.getReceiptCode());
        request.setGuideUrl(pdfUrl);
        request.setAmount(line.totalAmountAoa());
        request.setCurrency("AOA");
        request.setDueDate(line.dueDate());
        request.setMessage("Bordereau/comprovativo emitido após teste real InfinitePay Brasil. Mês/serviço: "
                + line.referenceMonth() + ". Base: " + moneyAoa(line.baseAmountAoa())
                + ". Multa: " + moneyAoa(line.fineAmountAoa())
                + ". Juros: " + moneyAoa(line.interestAmountAoa())
                + ". Total: " + moneyAoa(line.totalAmountAoa())
                + ". Valor teste Brasil: " + moneyBrl(pending.amountBrl()) + ".");
        fallbackNotificationService.sendGuideByEmail(request);
    }

    private void sendFallbackReceiptEmail(PendingInfinitePayPayment pending, String receiptCode, String pdfUrl) {
        GuideFallbackRequest request = new GuideFallbackRequest();
        request.setStudentName(pending.studentName());
        request.setStudentNumber(pending.studentNumber());
        request.setEmail(pending.email());
        request.setGuideCode(receiptCode);
        request.setGuideUrl(pdfUrl);
        request.setAmount(pending.academicAmountAoa());
        request.setCurrency("AOA");
        request.setDueDate(pending.dueDate());
        request.setMessage("Bordereau/comprovativo demo emitido após teste real InfinitePay Brasil. Valor académico: "
                + moneyAoa(pending.academicAmountAoa()) + ". Valor teste Brasil: " + moneyBrl(pending.amountBrl()) + ".");
        fallbackNotificationService.sendGuideByEmail(request);
    }

    private InfinitePayLinkPaymentResponse prepared(String orderNsu, BigDecimal amountBrl, String message) {
        return new InfinitePayLinkPaymentResponse()
                .setSuccess(false)
                .setEnabled(properties.isEnabled())
                .setStatus("PREPARED")
                .setOrderNsu(orderNsu)
                .setAmountBrl(amountBrl)
                .setMessage(message);
    }

    private String buildSuccessUrl(String orderNsu) {
        return appendQuery(properties.getSuccessUrl(), "order_nsu=" + encode(orderNsu) + "&status=success");
    }

    public String buildCancelUrl(String orderNsu) {
        return appendQuery(properties.getCancelUrl(), "order_nsu=" + encode(orderNsu) + "&status=cancel");
    }

    private String appendQuery(String baseUrl, String query) {
        String safeBase = safe(baseUrl).isBlank() ? API_BASE_URL + "/api/v1/public/infinitepay/success" : baseUrl.trim();
        return safeBase + (safeBase.contains("?") ? "&" : "?") + query;
    }

    private String extractCheckoutUrl(Map<String, Object> response) {
        if (response == null || response.isEmpty()) return "";
        return firstNonBlank(
                value(response.get("link")),
                value(response.get("url")),
                value(response.get("checkout_url")),
                value(response.get("payment_url")),
                value(response.get("redirect_url"))
        );
    }

    private Map<String, Object> parseJsonMap(String raw) {
        if (raw == null || raw.isBlank()) return new LinkedHashMap<>();
        try {
            return objectMapper.readValue(raw, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ignored) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("raw", raw);
            return map;
        }
    }

    private List<InfinitePayAcademicLine> normalizeLines(
            String referenceMonth,
            String serviceName,
            BigDecimal academicAmountAoa,
            BigDecimal baseAmountAoa,
            BigDecimal fineAmountAoa,
            BigDecimal interestAmountAoa,
            LocalDate dueDate,
            List<InfinitePayAcademicLine> academicLines
    ) {
        if (academicLines != null && !academicLines.isEmpty()) {
            return academicLines.stream()
                    .map(this::normalizeLine)
                    .toList();
        }

        BigDecimal base = safeAmount(baseAmountAoa);
        BigDecimal fine = safeAmount(fineAmountAoa);
        BigDecimal interest = safeAmount(interestAmountAoa);
        BigDecimal total = safeAmount(academicAmountAoa);
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            total = base.add(fine).add(interest);
        }

        return List.of(new InfinitePayAcademicLine(
                firstNonBlank(referenceMonth, "Pagamento académico"),
                firstNonBlank(serviceName, "Pagamento académico"),
                base,
                fine,
                interest,
                total,
                dueDate == null ? LocalDate.now() : dueDate
        ));
    }

    private InfinitePayAcademicLine normalizeLine(InfinitePayAcademicLine line) {
        BigDecimal base = safeAmount(line.baseAmountAoa());
        BigDecimal fine = safeAmount(line.fineAmountAoa());
        BigDecimal interest = safeAmount(line.interestAmountAoa());
        BigDecimal total = safeAmount(line.totalAmountAoa());
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            total = base.add(fine).add(interest);
        }
        return new InfinitePayAcademicLine(
                firstNonBlank(line.referenceMonth(), "Pagamento académico"),
                firstNonBlank(line.description(), "Pagamento académico"),
                base,
                fine,
                interest,
                total,
                line.dueDate() == null ? LocalDate.now() : line.dueDate()
        );
    }

    private String buildPaidMonthsSummary(List<PersistedAcademicPayment> persistedPayments) {
        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (PersistedAcademicPayment payment : persistedPayments) {
            InfinitePayAcademicLine line = payment.line();
            builder.append(index++).append(". ")
                    .append(line.referenceMonth())
                    .append(" — Base: ").append(moneyAoa(line.baseAmountAoa()))
                    .append(" | Multa: ").append(moneyAoa(line.fineAmountAoa()))
                    .append(" | Juros: ").append(moneyAoa(line.interestAmountAoa()))
                    .append(" | Total: ").append(moneyAoa(line.totalAmountAoa()))
                    .append(" | Bordereau: ").append(payment.receipt().getReceiptCode())
                    .append("\n");
        }
        return builder.toString().trim();
    }

    private String joinReceiptCodes(List<PersistedAcademicPayment> persistedPayments) {
        return persistedPayments.stream()
                .map(payment -> payment.receipt().getReceiptCode())
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        BigDecimal safe = safeAmount(amount);
        if (safe.compareTo(BigDecimal.ZERO) <= 0) {
            safe = new BigDecimal("5.00");
        }
        return safe.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return (amount == null ? BigDecimal.ZERO : amount).setScale(2, RoundingMode.HALF_UP);
    }

    private String moneyAoa(BigDecimal value) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        return String.format(Locale.forLanguageTag("pt-AO"), "%,.2f", safeValue).replace(',', '#').replace('.', ',').replace('#', '.') + " Kz";
    }

    private String moneyBrl(BigDecimal value) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        return "R$ " + String.format(Locale.forLanguageTag("pt-BR"), "%,.2f", safeValue);
    }

    private String generateOrderNsu() {
        return "SP" + UUID.randomUUID().toString().replaceAll("[^A-Za-z0-9]", "").substring(0, 14).toUpperCase(Locale.ROOT);
    }

    private String generateChargeCode() {
        String code;
        do {
            code = "IP" + System.currentTimeMillis() + shortId();
        } while (chargeRepository.existsByChargeCode(code));
        return code;
    }

    private String shortId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String encode(String value) {
        return URLEncoder.encode(safe(value), StandardCharsets.UTF_8);
    }

    private String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String item : values) {
            if (item != null && !item.trim().isBlank()) return item.trim();
        }
        return "";
    }

    private String value(Object object) {
        return object == null ? "" : object.toString().trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeBody(String body) {
        if (body == null || body.isBlank()) return "sem detalhes";
        return body.length() > 500 ? body.substring(0, 500) + "..." : body;
    }

    public record InfinitePayAcademicLine(
            String referenceMonth,
            String description,
            BigDecimal baseAmountAoa,
            BigDecimal fineAmountAoa,
            BigDecimal interestAmountAoa,
            BigDecimal totalAmountAoa,
            LocalDate dueDate
    ) {}

    public record PendingInfinitePayPayment(
            String orderNsu,
            String whatsappPhone,
            String studentName,
            String studentNumber,
            String email,
            String referenceMonth,
            String serviceName,
            BigDecimal academicAmountAoa,
            BigDecimal baseAmountAoa,
            BigDecimal fineAmountAoa,
            BigDecimal interestAmountAoa,
            BigDecimal amountBrl,
            LocalDate dueDate,
            List<InfinitePayAcademicLine> academicLines,
            LocalDateTime expiresAt
    ) {}

    private record PersistedAcademicPayment(
            InfinitePayAcademicLine line,
            Charge charge,
            ReceiptResponse receipt
    ) {}

    public record InfinitePayConfirmationResult(
            boolean success,
            String message,
            String orderNsu,
            String receiptCode
    ) {}
}
