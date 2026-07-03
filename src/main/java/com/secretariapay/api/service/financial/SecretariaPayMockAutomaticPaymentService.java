package com.secretariapay.api.service.financial;

import com.secretariapay.api.dto.financial.MockAutomaticPaymentRequest;
import com.secretariapay.api.dto.financial.MockAutomaticPaymentResponse;
import com.secretariapay.api.dto.financial.ReceiptResponse;
import com.secretariapay.api.dto.whatsapp.SecretariaPayMessageDispatchResult;
import com.secretariapay.api.dto.whatsapp.SecretariaPayMessageResponse;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import com.secretariapay.api.entity.enums.financial.PaymentProofStatus;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.entity.financial.PaymentProof;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.financial.ChargeRepository;
import com.secretariapay.api.repository.financial.PaymentProofRepository;
import com.secretariapay.api.repository.financial.ReceiptRepository;
import com.secretariapay.api.service.whatsapp.SecretariaPayMessageDispatchService;
import com.secretariapay.api.service.whatsapp.SecretariaPayMessageHistoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class SecretariaPayMockAutomaticPaymentService {

    private final ChargeRepository chargeRepository;
    private final PaymentProofRepository paymentProofRepository;
    private final ReceiptRepository receiptRepository;
    private final ReceiptService receiptService;
    private final SecretariaPayMessageHistoryService messageHistoryService;
    private final SecretariaPayMessageDispatchService messageDispatchService;

    public SecretariaPayMockAutomaticPaymentService(
            ChargeRepository chargeRepository,
            PaymentProofRepository paymentProofRepository,
            ReceiptRepository receiptRepository,
            ReceiptService receiptService,
            SecretariaPayMessageHistoryService messageHistoryService,
            SecretariaPayMessageDispatchService messageDispatchService
    ) {
        this.chargeRepository = chargeRepository;
        this.paymentProofRepository = paymentProofRepository;
        this.receiptRepository = receiptRepository;
        this.receiptService = receiptService;
        this.messageHistoryService = messageHistoryService;
        this.messageDispatchService = messageDispatchService;
    }

    @Transactional
    public MockAutomaticPaymentResponse confirmByChargeId(UUID chargeId, String forcedMethod, MockAutomaticPaymentRequest request) {
        Charge charge = chargeRepository.findById(chargeId)
                .orElseThrow(() -> new NotFoundException("Cobrança não encontrada."));

        return confirmPayment(charge, forcedMethod, request);
    }

    @Transactional
    public MockAutomaticPaymentResponse confirmByChargeCode(String chargeCode, String forcedMethod, MockAutomaticPaymentRequest request) {
        Charge charge = chargeRepository.findByChargeCode(chargeCode)
                .orElseThrow(() -> new NotFoundException("Cobrança não encontrada."));

        return confirmPayment(charge, forcedMethod, request);
    }

    private MockAutomaticPaymentResponse confirmPayment(
            Charge charge,
            String forcedMethod,
            MockAutomaticPaymentRequest request
    ) {
        MockAutomaticPaymentRequest safeRequest = request == null ? new MockAutomaticPaymentRequest() : request;
        LocalDateTime now = LocalDateTime.now();

        String method = normalizeMethod(firstNonBlank(forcedMethod, safeRequest.getPaymentMethod(), "MOCK_AUTO"));
        String settlementStatus = normalizeSettlement(firstNonBlank(safeRequest.getSettlementStatus(), defaultSettlementFor(method)));
        String externalTransactionId = firstNonBlank(
                safeRequest.getExternalTransactionId(),
                "MOCK-" + method + "-" + charge.getChargeCode() + "-" + System.currentTimeMillis()
        );

        validateAmountIfProvided(charge, safeRequest.getAmount());

        PaymentProof proof = createAutomaticProof(
                charge,
                safeRequest,
                method,
                settlementStatus,
                externalTransactionId,
                now
        );

        charge
                .setStatus(ChargeStatus.PAID)
                .setPaidAt(now);

        chargeRepository.save(charge);

        ReceiptResponse receipt = receiptRepository.findByChargeId(charge.getId())
                .map(receiptService::toResponse)
                .orElseGet(() -> receiptService.issueForCharge(charge.getId()));

        SecretariaPayMessageResponse receiptMessage = messageHistoryService.generateReceiptIssued(receipt.getId());
        SecretariaPayMessageDispatchResult dispatchResult = messageDispatchService.dispatch(receiptMessage.getId());

        return new MockAutomaticPaymentResponse()
                .setEvent("MOCK_AUTOMATIC_PAYMENT_CONFIRMED")
                .setAutomatic(true)
                .setPaymentMethod(method)
                .setSettlementStatus(settlementStatus)
                .setExternalTransactionId(externalTransactionId)
                .setChargeId(charge.getId())
                .setChargeCode(charge.getChargeCode())
                .setChargeStatus(charge.getStatus() != null ? charge.getStatus().name() : null)
                .setPaymentProofId(proof.getId())
                .setPaymentProofStatus(proof.getStatus() != null ? proof.getStatus().name() : null)
                .setReceiptId(receipt.getId())
                .setReceiptCode(receipt.getReceiptCode())
                .setReceiptPdfUrl(receipt.getPdfUrl())
                .setReceiptValidationUrl(receipt.getValidationUrl())
                .setReceiptMessageId(receiptMessage.getId())
                .setReceiptMessageStatus(dispatchResult.getStatus())
                .setProviderMessageId(dispatchResult.getProviderMessageId())
                .setRecipientPhone(dispatchResult.getRecipientPhone())
                .setNote(buildResponseNote(method, settlementStatus))
                .setProcessedAt(now);
    }

    private PaymentProof createAutomaticProof(
            Charge charge,
            MockAutomaticPaymentRequest request,
            String method,
            String settlementStatus,
            String externalTransactionId,
            LocalDateTime now
    ) {
        Student student = charge.getStudent();

        String payerPhone = firstNonBlank(
                request.getPayerPhone(),
                student != null ? student.getWhatsapp() : null,
                student != null ? student.getPhone() : null
        );

        String payload = buildMockPayload(charge, request, method, settlementStatus, externalTransactionId);

        PaymentProof proof = new PaymentProof()
                .setCharge(charge)
                .setFileUrl("mock-bank-payment://" + method + "/" + externalTransactionId)
                .setFileName("pagamento-automatico-" + charge.getChargeCode() + ".json")
                .setMimeType("application/json")
                .setSubmittedByPhone(payerPhone)
                .setSubmittedAt(now)
                .setStatus(PaymentProofStatus.APPROVED)
                .setReviewedAt(now)
                .setReviewNote(payload);

        return paymentProofRepository.save(proof);
    }

    private void validateAmountIfProvided(Charge charge, BigDecimal amount) {
        if (amount == null) {
            return;
        }

        BigDecimal expected = charge.getTotalAmount();

        if (expected == null) {
            expected = charge.getAmount();
        }

        if (expected == null) {
            return;
        }

        if (amount.compareTo(expected) != 0) {
            throw new IllegalArgumentException(
                    "Valor do pagamento mock não corresponde ao valor total da cobrança. Esperado: "
                            + expected + ", recebido: " + amount
            );
        }
    }

    private String buildMockPayload(
            Charge charge,
            MockAutomaticPaymentRequest request,
            String method,
            String settlementStatus,
            String externalTransactionId
    ) {
        StringBuilder builder = new StringBuilder();

        builder.append("{");
        appendJson(builder, "type", "MOCK_AUTOMATIC_PAYMENT");
        appendJson(builder, "paymentMethod", method);
        appendJson(builder, "settlementStatus", settlementStatus);
        appendJson(builder, "externalTransactionId", externalTransactionId);
        appendJson(builder, "chargeCode", charge.getChargeCode());
        appendJson(builder, "bankName", request.getBankName());
        appendJson(builder, "bankReference", request.getBankReference());
        appendJson(builder, "payerName", request.getPayerName());
        appendJson(builder, "payerPhone", request.getPayerPhone());
        appendJson(builder, "note", firstNonBlank(request.getNote(), "Pagamento confirmado automaticamente por integração mock."));
        builder.append("\"processedBy\":\"SecretariaPayMockAutomaticPaymentService\"");
        builder.append("}");

        return builder.toString();
    }

    private void appendJson(StringBuilder builder, String key, String value) {
        builder.append("\"")
                .append(escapeJson(key))
                .append("\":\"")
                .append(escapeJson(value == null ? "" : value))
                .append("\",");
    }

    private String buildResponseNote(String method, String settlementStatus) {
        return switch (method) {
            case "MULTICAIXA_EXPRESS" ->
                    "Pagamento mock confirmado como entrada automática via Multicaixa Express. Recibo emitido sem validação manual.";
            case "IBAN_MESMO_BANCO" ->
                    "Pagamento mock confirmado por transferência IBAN do mesmo banco. Recibo emitido automaticamente.";
            case "IBAN_OUTRO_BANCO" ->
                    "Pagamento mock liquidado por transferência IBAN de outro banco. Recibo emitido após compensação mock.";
            case "DEPOSITO_BANCARIO" ->
                    "Depósito mock conferido automaticamente após regra de compensação. Recibo emitido.";
            case "UNITEL_MONEY", "AFRIMONEY", "CARTEIRA_MOVEL" ->
                    "Pagamento mock confirmado por carteira móvel. Recibo emitido automaticamente.";
            default ->
                    "Pagamento mock confirmado automaticamente. Status de liquidação: " + settlementStatus + ".";
        };
    }

    private String defaultSettlementFor(String method) {
        return switch (method) {
            case "IBAN_OUTRO_BANCO" -> "SETTLED_AFTER_CLEARING";
            case "DEPOSITO_BANCARIO" -> "SETTLED_AFTER_24H_MOCK";
            case "IBAN_MESMO_BANCO" -> "SETTLED_SAME_DAY";
            default -> "SETTLED";
        };
    }

    private String normalizeMethod(String value) {
        String normalized = firstNonBlank(value, "MOCK_AUTO")
                .trim()
                .toUpperCase(Locale.ROOT)
                .replace("-", "_")
                .replace(" ", "_");

        return switch (normalized) {
            case "MULTICAIXA", "MULTICAIXA_EXPRESS", "MCX", "MULTICAIXA_EXPRESSA" -> "MULTICAIXA_EXPRESS";
            case "IBAN", "TRANSFERENCIA_IBAN", "TRANSFERÊNCIA_IBAN", "IBAN_MESMO_BANCO", "SAME_BANK" -> "IBAN_MESMO_BANCO";
            case "IBAN_OUTRO_BANCO", "OUTRO_BANCO", "OTHER_BANK" -> "IBAN_OUTRO_BANCO";
            case "DEPOSITO", "DEPÓSITO", "DEPOSITO_BANCARIO", "DEPÓSITO_BANCÁRIO" -> "DEPOSITO_BANCARIO";
            case "UNITEL", "UNITEL_MONEY" -> "UNITEL_MONEY";
            case "AFRIMONEY", "AFRI_MONEY" -> "AFRIMONEY";
            case "CARTEIRA", "CARTEIRA_MOVEL", "MOBILE_MONEY" -> "CARTEIRA_MOVEL";
            default -> normalized;
        };
    }

    private String normalizeSettlement(String value) {
        return firstNonBlank(value, "SETTLED")
                .trim()
                .toUpperCase(Locale.ROOT)
                .replace("-", "_")
                .replace(" ", "_");
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }

        return "";
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
