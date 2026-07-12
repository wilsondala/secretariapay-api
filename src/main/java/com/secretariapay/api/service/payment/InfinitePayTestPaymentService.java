package com.secretariapay.api.service.payment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secretariapay.api.config.InfinitePayProperties;
import com.secretariapay.api.dto.financial.ReceiptResponse;
import com.secretariapay.api.dto.notification.GuideFallbackRequest;
import com.secretariapay.api.dto.whatsapp.WhatsAppCloudSendResult;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.repository.academic.StudentRepository;
import com.secretariapay.api.repository.financial.ChargeRepository;
import com.secretariapay.api.service.FallbackNotificationService;
import com.secretariapay.api.service.financial.FinancialChargeCalculation;
import com.secretariapay.api.service.financial.FinancialPenaltyCalculatorService;
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
import java.time.YearMonth;
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
    private static final BigDecimal DEFAULT_PROPINA = new BigDecimal("45000.00");

    private final InfinitePayProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final WhatsAppCloudApiClient whatsAppCloudApiClient;
    private final FallbackNotificationService fallbackNotificationService;
    private final StudentRepository studentRepository;
    private final ChargeRepository chargeRepository;
    private final ReceiptService receiptService;
    private final FinancialPenaltyCalculatorService penaltyCalculatorService;
    private final Map<String, PendingPayment> pendingPayments = new ConcurrentHashMap<>();

    public InfinitePayTestPaymentService(
            InfinitePayProperties properties,
            RestClient.Builder restClientBuilder,
            WhatsAppCloudApiClient whatsAppCloudApiClient,
            FallbackNotificationService fallbackNotificationService,
            StudentRepository studentRepository,
            ChargeRepository chargeRepository,
            ReceiptService receiptService,
            FinancialPenaltyCalculatorService penaltyCalculatorService
    ) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
        this.objectMapper = new ObjectMapper();
        this.whatsAppCloudApiClient = whatsAppCloudApiClient;
        this.fallbackNotificationService = fallbackNotificationService;
        this.studentRepository = studentRepository;
        this.chargeRepository = chargeRepository;
        this.receiptService = receiptService;
        this.penaltyCalculatorService = penaltyCalculatorService;
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
            LocalDate dueDate
    ) {
        BigDecimal amountBrl = normalizeAmount(properties.getTestAmountBrl());
        String orderNsu = generateOrderNsu();

        if (!properties.isEnabled()) return prepared(orderNsu, amountBrl, "InfinitePay desativado.");
        if (properties.getHandle().isBlank()) return prepared(orderNsu, amountBrl, "INFINITEPAY_HANDLE não configurado.");

        try {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("quantity", 1);
            item.put("price", amountBrl.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValue());
            item.put("description", "Teste SecretáriaPay - " + safe(serviceName));

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

            Map<String, Object> data = parseJsonMap(raw);
            String checkoutUrl = checkoutUrl(data);
            if (checkoutUrl.isBlank()) {
                return new InfinitePayLinkPaymentResponse()
                        .setSuccess(false)
                        .setEnabled(true)
                        .setStatus("FAILED")
                        .setOrderNsu(orderNsu)
                        .setAmountBrl(amountBrl)
                        .setMessage("InfinitePay não retornou link.")
                        .setRawResponse(raw)
                        .setProviderData(data);
            }

            PendingPayment pending = new PendingPayment(
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
                    buildLines(referenceMonth, serviceName, baseAmountAoa, academicAmountAoa, fineAmountAoa, interestAmountAoa, dueDate),
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
                    .setProviderData(data);
        } catch (RestClientResponseException ex) {
            return new InfinitePayLinkPaymentResponse()
                    .setSuccess(false)
                    .setEnabled(true)
                    .setStatus("FAILED")
                    .setOrderNsu(orderNsu)
                    .setAmountBrl(amountBrl)
                    .setMessage("Falha InfinitePay HTTP " + ex.getStatusCode().value() + ": " + safeBody(ex.getResponseBodyAsString()));
        } catch (Exception ex) {
            return new InfinitePayLinkPaymentResponse()
                    .setSuccess(false)
                    .setEnabled(true)
                    .setStatus("FAILED")
                    .setOrderNsu(orderNsu)
                    .setAmountBrl(amountBrl)
                    .setMessage("Falha ao criar link InfinitePay: " + ex.getMessage());
        }
    }

    public Optional<PendingPayment> findPending(String orderNsu) {
        if (orderNsu == null || orderNsu.isBlank()) return Optional.empty();
        return Optional.ofNullable(pendingPayments.get(orderNsu.trim()));
    }

    @Transactional
    public InfinitePayConfirmationResult confirmBySuccessReturn(String orderNsu) {
        if (orderNsu == null || orderNsu.isBlank()) return new InfinitePayConfirmationResult(false, "order_nsu não informado.", null, null);

        PendingPayment pending = pendingPayments.remove(orderNsu.trim());
        if (pending == null) return new InfinitePayConfirmationResult(false, "Pagamento não encontrado ou já processado.", orderNsu, null);

        List<PersistedPayment> persisted = persistPayments(pending);
        if (persisted.isEmpty()) {
            String receiptCode = "BORD-IP-" + shortId();
            String pdfUrl = API_BASE_URL + "/api/v1/public/demo/receipts/" + encode(receiptCode) + "/pdf";
            sendDemoReceipt(pending, receiptCode, pdfUrl);
            return new InfinitePayConfirmationResult(true, "Pagamento confirmado, mas estudante não encontrado no banco.", pending.orderNsu(), receiptCode);
        }

        sendPersistedReceipts(pending, persisted);
        return new InfinitePayConfirmationResult(true, "Pagamento confirmado e gravado no painel financeiro.", pending.orderNsu(), receiptCodes(persisted));
    }

    public InfinitePayConfirmationResult cancel(String orderNsu) {
        pendingPayments.remove(safe(orderNsu));
        return new InfinitePayConfirmationResult(true, "Pagamento InfinitePay cancelado.", orderNsu, null);
    }

    private List<PersistedPayment> persistPayments(PendingPayment pending) {
        Optional<Student> studentOptional = studentRepository.findByStudentNumber(pending.studentNumber());
        if (studentOptional.isEmpty()) return List.of();

        Student student = studentOptional.get();
        List<PersistedPayment> result = new ArrayList<>();

        for (AcademicLine line : pending.lines()) {
            Charge charge = new Charge()
                    .setStudent(student)
                    .setChargeCode(generateChargeCode())
                    .setDescription("Teste real InfinitePay - " + line.description())
                    .setReferenceMonth(line.referenceMonth())
                    .setDueDate(line.dueDate())
                    .setAmount(line.baseAmount())
                    .setFineAmount(line.fineAmount())
                    .setInterestAmount(line.interestAmount())
                    .setDiscountAmount(BigDecimal.ZERO)
                    .setCurrency("AOA")
                    .setStatus(ChargeStatus.PAID)
                    .setPaidAt(LocalDateTime.now());

            Charge saved = chargeRepository.save(charge);
            ReceiptResponse receipt = receiptService.issueOrFindForCharge(saved.getId());
            result.add(new PersistedPayment(line, saved, receipt));
        }
        return result;
    }

    private void sendPersistedReceipts(PendingPayment pending, List<PersistedPayment> payments) {
        int whatsappDocumentsSent = 0;
        int whatsappDocumentsFailed = 0;
        int emailCopiesTriggered = 0;

        for (PersistedPayment payment : payments) {
            ReceiptResponse receipt = payment.receipt();
            AcademicLine line = payment.line();
            String publicPdfUrl = buildOfficialReceiptPdfUrl(receipt, false);
            String deliveryPdfUrl = buildOfficialReceiptPdfUrl(receipt, true);
            String caption = buildOfficialReceiptCaption(pending, line, receipt, publicPdfUrl);

            if (!pending.whatsappPhone().isBlank()) {
                WhatsAppCloudSendResult result = whatsAppCloudApiClient.sendDocumentByLink(
                        pending.whatsappPhone(),
                        deliveryPdfUrl,
                        buildOfficialReceiptFileName(pending, receipt),
                        caption
                );

                if (result.isSuccess()) {
                    whatsappDocumentsSent++;
                } else {
                    whatsappDocumentsFailed++;
                    whatsAppCloudApiClient.sendText(
                            pending.whatsappPhone(),
                            ("O comprovativo oficial foi emitido, mas o anexo não pôde ser entregue automaticamente neste momento.\n\n"
                                    + "Comprovativo: %s\n"
                                    + "Link público: %s")
                                    .formatted(receipt.getReceiptCode(), publicPdfUrl)
                    );
                }
            }

            sendReceiptEmail(pending, payment, publicPdfUrl);
            if (!pending.email().isBlank()) {
                emailCopiesTriggered++;
            }
        }

        if (!pending.whatsappPhone().isBlank()) {
            whatsAppCloudApiClient.sendText(pending.whatsappPhone(), ("""
                    Pagamento confirmado pela InfinitePay.

                    A baixa foi gravada no painel financeiro do SecretáriaPay.

                    Valor teste Brasil recebido: %s
                    Código teste: %s

                    Meses/serviços baixados:
                    %s

                    Comprovativos oficiais enviados:
                    - WhatsApp: %d
                    - Falhas no anexo: %d
                    - E-mail acionado: %d
                    """).formatted(
                    moneyBrl(pending.amountBrl()),
                    pending.orderNsu(),
                    summary(payments),
                    whatsappDocumentsSent,
                    whatsappDocumentsFailed,
                    emailCopiesTriggered
            ).trim());
        }
    }

    private void sendDemoReceipt(PendingPayment pending, String receiptCode, String pdfUrl) {
        if (!pending.whatsappPhone().isBlank()) {
            whatsAppCloudApiClient.sendDocumentByLink(pending.whatsappPhone(), pdfUrl, "bordereau-" + receiptCode + ".pdf", "Bordereau demo: " + receiptCode);
        }
        GuideFallbackRequest request = new GuideFallbackRequest();
        request.setStudentName(pending.studentName());
        request.setStudentNumber(pending.studentNumber());
        request.setEmail(pending.email());
        request.setGuideCode(receiptCode);
        request.setGuideUrl(pdfUrl);
        request.setAmount(pending.academicAmount());
        request.setCurrency("AOA");
        request.setDueDate(pending.dueDate());
        request.setMessage("Bordereau demo emitido após teste InfinitePay.");
        fallbackNotificationService.sendGuideByEmail(request);
    }

    private void sendReceiptEmail(PendingPayment pending, PersistedPayment payment, String pdfUrl) {
        AcademicLine line = payment.line();
        ReceiptResponse receipt = payment.receipt();
        GuideFallbackRequest request = new GuideFallbackRequest();
        request.setStudentName(pending.studentName());
        request.setStudentNumber(pending.studentNumber());
        request.setEmail(pending.email());
        request.setGuideCode(receipt.getReceiptCode());
        request.setGuideUrl(pdfUrl);
        request.setAmount(line.totalAmount());
        request.setCurrency("AOA");
        request.setDueDate(line.dueDate());
        request.setMessage("Comprovativo oficial emitido automaticamente após confirmação do pagamento. Referência: "
                + line.referenceMonth() + ". Base: " + moneyAoa(line.baseAmount())
                + ". Multa: " + moneyAoa(line.fineAmount())
                + ". Juros: " + moneyAoa(line.interestAmount())
                + ". Total: " + moneyAoa(line.totalAmount()) + ".");
        fallbackNotificationService.sendGuideByEmail(request);
    }

    private String buildOfficialReceiptPdfUrl(ReceiptResponse receipt, boolean bustCache) {
        String baseUrl = API_BASE_URL + "/api/v1/public/receipts/" + encode(receipt.getReceiptCode()) + "/pdf";
        return bustCache ? baseUrl + "?v=" + System.currentTimeMillis() : baseUrl;
    }

    private String buildOfficialReceiptFileName(PendingPayment pending, ReceiptResponse receipt) {
        return "Comprovativo_Pagamentos_"
                + safe(pending.studentNumber()).replaceAll("[^A-Za-z0-9._-]", "-")
                + "_"
                + safe(receipt.getReceiptCode()).replaceAll("[^A-Za-z0-9._-]", "-")
                + ".pdf";
    }

    private String buildOfficialReceiptCaption(PendingPayment pending, AcademicLine line, ReceiptResponse receipt, String publicPdfUrl) {
        return ("""
                SecretáriaPay Académico: comprovativo oficial emitido.

                Comprovativo: %s
                Estudante: %s
                Matrícula: %s
                Referência: %s
                Descrição: %s
                Base: %s
                Multa: %s
                Juros: %s
                Total académico: %s
                Valor teste Brasil: %s
                Código teste: %s
                Link público: %s
                """).formatted(
                receipt.getReceiptCode(),
                pending.studentName(),
                pending.studentNumber(),
                line.referenceMonth(),
                line.description(),
                moneyAoa(line.baseAmount()),
                moneyAoa(line.fineAmount()),
                moneyAoa(line.interestAmount()),
                moneyAoa(line.totalAmount()),
                moneyBrl(pending.amountBrl()),
                pending.orderNsu(),
                publicPdfUrl
        ).trim();
    }

    private List<AcademicLine> buildLines(String referenceMonth, String serviceName, BigDecimal baseAmount, BigDecimal totalAmount, BigDecimal fineAmount, BigDecimal interestAmount, LocalDate dueDate) {
        String ref = safe(referenceMonth).toLowerCase(Locale.ROOT);
        List<YearMonthRef> refs = new ArrayList<>();
        if (ref.contains("maio/2026")) refs.add(new YearMonthRef("Maio/2026", YearMonth.of(2026, 5)));
        if (ref.contains("junho/2026")) refs.add(new YearMonthRef("Junho/2026", YearMonth.of(2026, 6)));
        if (ref.contains("julho/2026")) refs.add(new YearMonthRef("Julho/2026", YearMonth.of(2026, 7)));

        if (!refs.isEmpty()) {
            BigDecimal totalBase = safeAmount(baseAmount);
            BigDecimal monthlyBase = totalBase.compareTo(BigDecimal.ZERO) > 0 ? totalBase.divide(BigDecimal.valueOf(refs.size()), 2, RoundingMode.HALF_UP) : DEFAULT_PROPINA;
            return refs.stream().map(item -> fromCalculation(penaltyCalculatorService.calculate(item.label(), monthlyBase, item.yearMonth()))).toList();
        }

        BigDecimal base = safeAmount(baseAmount);
        BigDecimal fine = safeAmount(fineAmount);
        BigDecimal interest = safeAmount(interestAmount);
        BigDecimal total = safeAmount(totalAmount);
        if (total.compareTo(BigDecimal.ZERO) <= 0) total = base.add(fine).add(interest);
        return List.of(new AcademicLine(firstNonBlank(referenceMonth, "Pagamento académico"), firstNonBlank(serviceName, "Pagamento académico"), base, fine, interest, total, dueDate == null ? LocalDate.now() : dueDate));
    }

    private AcademicLine fromCalculation(FinancialChargeCalculation calculation) {
        return new AcademicLine(calculation.getReferenceMonth(), "Propina " + calculation.getReferenceMonth(), calculation.getBaseAmount(), calculation.getFineAmount(), calculation.getInterestAmount(), calculation.getTotalAmount(), calculation.getDueDate());
    }

    private InfinitePayLinkPaymentResponse prepared(String orderNsu, BigDecimal amountBrl, String message) {
        return new InfinitePayLinkPaymentResponse().setSuccess(false).setEnabled(properties.isEnabled()).setStatus("PREPARED").setOrderNsu(orderNsu).setAmountBrl(amountBrl).setMessage(message);
    }

    private String buildSuccessUrl(String orderNsu) { return appendQuery(properties.getSuccessUrl(), "order_nsu=" + encode(orderNsu) + "&status=success"); }
    private String appendQuery(String baseUrl, String query) { String safeBase = safe(baseUrl).isBlank() ? API_BASE_URL + "/api/v1/public/infinitepay/success" : baseUrl.trim(); return safeBase + (safeBase.contains("?") ? "&" : "?") + query; }
    private String checkoutUrl(Map<String, Object> response) { return firstNonBlank(value(response.get("link")), value(response.get("url")), value(response.get("checkout_url")), value(response.get("payment_url")), value(response.get("redirect_url"))); }
    private Map<String, Object> parseJsonMap(String raw) { if (raw == null || raw.isBlank()) return new LinkedHashMap<>(); try { return objectMapper.readValue(raw, new TypeReference<LinkedHashMap<String, Object>>() {}); } catch (Exception ignored) { Map<String, Object> map = new LinkedHashMap<>(); map.put("raw", raw); return map; } }
    private String summary(List<PersistedPayment> payments) { StringBuilder builder = new StringBuilder(); int i = 1; for (PersistedPayment payment : payments) { AcademicLine line = payment.line(); builder.append(i++).append(". ").append(line.referenceMonth()).append(" — Base: ").append(moneyAoa(line.baseAmount())).append(" | Multa: ").append(moneyAoa(line.fineAmount())).append(" | Juros: ").append(moneyAoa(line.interestAmount())).append(" | Total: ").append(moneyAoa(line.totalAmount())).append(" | Bordereau: ").append(payment.receipt().getReceiptCode()).append("\n"); } return builder.toString().trim(); }
    private String receiptCodes(List<PersistedPayment> payments) { return payments.stream().map(p -> p.receipt().getReceiptCode()).reduce((a, b) -> a + ", " + b).orElse(""); }
    private BigDecimal normalizeAmount(BigDecimal amount) { BigDecimal safe = safeAmount(amount); if (safe.compareTo(BigDecimal.ZERO) <= 0) safe = new BigDecimal("5.00"); return safe.setScale(2, RoundingMode.HALF_UP); }
    private BigDecimal safeAmount(BigDecimal amount) { return (amount == null ? BigDecimal.ZERO : amount).setScale(2, RoundingMode.HALF_UP); }
    private String moneyAoa(BigDecimal value) { BigDecimal safeValue = value == null ? BigDecimal.ZERO : value; return String.format(Locale.forLanguageTag("pt-AO"), "%,.2f", safeValue).replace(',', '#').replace('.', ',').replace('#', '.') + " Kz"; }
    private String moneyBrl(BigDecimal value) { BigDecimal safeValue = value == null ? BigDecimal.ZERO : value; return "R$ " + String.format(Locale.forLanguageTag("pt-BR"), "%,.2f", safeValue); }
    private String generateOrderNsu() { return "SP" + UUID.randomUUID().toString().replaceAll("[^A-Za-z0-9]", "").substring(0, 14).toUpperCase(Locale.ROOT); }
    private String generateChargeCode() { String code; do { code = "IP" + System.currentTimeMillis() + shortId(); } while (chargeRepository.existsByChargeCode(code)); return code; }
    private String shortId() { return UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT); }
    private String encode(String value) { return URLEncoder.encode(safe(value), StandardCharsets.UTF_8); }
    private String firstNonBlank(String... values) { if (values == null) return ""; for (String item : values) if (item != null && !item.trim().isBlank()) return item.trim(); return ""; }
    private String value(Object object) { return object == null ? "" : object.toString().trim(); }
    private String safe(String value) { return value == null ? "" : value.trim(); }
    private String safeBody(String body) { if (body == null || body.isBlank()) return "sem detalhes"; return body.length() > 500 ? body.substring(0, 500) + "..." : body; }

    public record PendingPayment(String orderNsu, String whatsappPhone, String studentName, String studentNumber, String email, String referenceMonth, String serviceName, BigDecimal academicAmount, BigDecimal baseAmount, BigDecimal fineAmount, BigDecimal interestAmount, BigDecimal amountBrl, LocalDate dueDate, List<AcademicLine> lines, LocalDateTime expiresAt) {}
    private record AcademicLine(String referenceMonth, String description, BigDecimal baseAmount, BigDecimal fineAmount, BigDecimal interestAmount, BigDecimal totalAmount, LocalDate dueDate) {}
    private record PersistedPayment(AcademicLine line, Charge charge, ReceiptResponse receipt) {}
    private record YearMonthRef(String label, YearMonth yearMonth) {}
    public record InfinitePayConfirmationResult(boolean success, String message, String orderNsu, String receiptCode) {}
}
