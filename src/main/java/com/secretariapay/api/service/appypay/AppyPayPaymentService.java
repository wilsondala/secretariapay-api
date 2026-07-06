package com.secretariapay.api.service.appypay;

import com.secretariapay.api.dto.appypay.AppyPayChargeRequest;
import com.secretariapay.api.dto.appypay.AppyPayChargeResponse;
import com.secretariapay.api.dto.appypay.AppyPayProviderResponse;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.financial.ChargeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AppyPayPaymentService {

    private static final int MAX_MERCHANT_TRANSACTION_ID_LENGTH = 15;

    private final AppyPayClient appyPayClient;
    private final ChargeRepository chargeRepository;
    private final String referencePaymentMethod;
    private final String gpoPaymentMethod;
    private final String referenceEntity;

    public AppyPayPaymentService(
            AppyPayClient appyPayClient,
            ChargeRepository chargeRepository,
            @Value("${APPYPAY_PAYMENT_METHOD_REF:}") String referencePaymentMethod,
            @Value("${APPYPAY_PAYMENT_METHOD_GPO:}") String gpoPaymentMethod,
            @Value("${APPYPAY_REFERENCE_ENTITY:00348}") String referenceEntity
    ) {
        this.appyPayClient = appyPayClient;
        this.chargeRepository = chargeRepository;
        this.referencePaymentMethod = referencePaymentMethod == null ? "" : referencePaymentMethod.trim();
        this.gpoPaymentMethod = gpoPaymentMethod == null ? "" : gpoPaymentMethod.trim();
        this.referenceEntity = referenceEntity == null || referenceEntity.isBlank() ? "00348" : referenceEntity.trim();
    }

    @Transactional(readOnly = true)
    public AppyPayChargeResponse createReferenceCharge(AppyPayChargeRequest request) {
        Charge charge = findCharge(request.getChargeId());
        String merchantTransactionId = merchantTransactionIdFor(charge);
        Map<String, Object> payload = basePayload(charge, merchantTransactionId, firstNonBlank(request.getDescription(), charge.getDescription()));
        payload.put("paymentMethod", referencePaymentMethod);
        AppyPayProviderResponse provider = appyPayClient.createCharge(payload);
        return toChargeResponse("REFERENCE", charge, merchantTransactionId, provider)
                .setReferenceEntity(firstNonBlank(extractString(provider.getBody(), "entity", "referenceEntity", "paymentEntity"), referenceEntity))
                .setReferenceNumber(extractString(provider.getBody(), "referenceNumber", "reference", "paymentReference"));
    }

    @Transactional(readOnly = true)
    public AppyPayChargeResponse createGpoCharge(AppyPayChargeRequest request) {
        Charge charge = findCharge(request.getChargeId());
        String phoneNumber = normalizePhone(request.getPhoneNumber());
        if (phoneNumber.isBlank()) throw new IllegalArgumentException("Telefone Multicaixa Express obrigatório.");
        String merchantTransactionId = merchantTransactionIdFor(charge);
        Map<String, Object> payload = basePayload(charge, merchantTransactionId, firstNonBlank(request.getDescription(), charge.getDescription()));
        payload.put("paymentMethod", gpoPaymentMethod);
        payload.put("paymentInfo", Map.of("phoneNumber", phoneNumber));
        AppyPayProviderResponse provider = appyPayClient.createCharge(payload);
        return toChargeResponse("GPO", charge, merchantTransactionId, provider);
    }

    public AppyPayProviderResponse processReferenceInSandbox(String entity, String referenceNumber) {
        return appyPayClient.processReferenceInSandbox(firstNonBlank(entity, referenceEntity), referenceNumber);
    }

    private Map<String, Object> basePayload(Charge charge, String merchantTransactionId, String description) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("amount", charge.getTotalAmount() == null ? BigDecimal.ZERO : charge.getTotalAmount());
        payload.put("currency", firstNonBlank(charge.getCurrency(), "AOA"));
        payload.put("description", firstNonBlank(description, "Propina IMETRO - " + charge.getChargeCode()));
        payload.put("merchantTransactionId", merchantTransactionId);
        return payload;
    }

    private AppyPayChargeResponse toChargeResponse(String paymentMethod, Charge charge, String merchantTransactionId, AppyPayProviderResponse provider) {
        return new AppyPayChargeResponse()
                .setSuccess(provider.isSuccess())
                .setMessage(provider.isSuccess() ? "Pedido enviado à AppyPay." : "Falha ao comunicar com a AppyPay.")
                .setPaymentMethod(paymentMethod)
                .setChargeId(charge.getId())
                .setChargeCode(charge.getChargeCode())
                .setMerchantTransactionId(merchantTransactionId)
                .setAmount(charge.getTotalAmount())
                .setCurrency(charge.getCurrency())
                .setProviderStatus(extractString(provider.getBody(), "status", "state", "paymentStatus", "transactionStatus"))
                .setProviderHttpStatus(provider.getHttpStatus())
                .setProviderError(provider.getErrorMessage())
                .setProviderResponse(provider.getBody());
    }

    private Charge findCharge(UUID chargeId) {
        return chargeRepository.findById(chargeId).orElseThrow(() -> new NotFoundException("Cobrança não encontrada."));
    }

    private String merchantTransactionIdFor(Charge charge) {
        String source = firstNonBlank(charge.getChargeCode(), charge.getId() != null ? charge.getId().toString() : "");
        String sanitized = source.replaceAll("[^A-Za-z0-9]", "");

        if (sanitized.isBlank()) {
            sanitized = "SP" + System.currentTimeMillis();
        }

        if (sanitized.length() <= MAX_MERCHANT_TRANSACTION_ID_LENGTH) {
            return sanitized;
        }

        return sanitized.substring(sanitized.length() - MAX_MERCHANT_TRANSACTION_ID_LENGTH);
    }

    private String normalizePhone(String phoneNumber) {
        if (phoneNumber == null) return "";
        String digits = phoneNumber.replaceAll("[^0-9]", "");
        if (digits.startsWith("00")) digits = digits.substring(2);
        if (digits.length() == 9 && digits.startsWith("9")) digits = "244" + digits;
        return digits;
    }

    private String extractString(Object source, String... keys) {
        if (source == null || keys == null) return "";
        if (source instanceof Map<?, ?> map) {
            for (String key : keys) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() != null && key.equalsIgnoreCase(entry.getKey().toString())) {
                        return entry.getValue() == null ? "" : entry.getValue().toString();
                    }
                }
            }
        }
        return "";
    }

    private String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) if (value != null && !value.isBlank()) return value.trim();
        return "";
    }
}
