package com.secretariapay.api.service.appypay;

import com.secretariapay.api.dto.appypay.AppyPayWebhookResponse;
import com.secretariapay.api.dto.financial.ReceiptResponse;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.repository.financial.ChargeRepository;
import com.secretariapay.api.service.financial.ReceiptDeliveryService;
import com.secretariapay.api.service.financial.ReceiptService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AppyPayWebhookService {

    private static final int MAX_MERCHANT_TRANSACTION_ID_LENGTH = 15;

    private final ChargeRepository chargeRepository;
    private final ReceiptService receiptService;
    private final ReceiptDeliveryService receiptDeliveryService;

    public AppyPayWebhookService(
            ChargeRepository chargeRepository,
            ReceiptService receiptService,
            ReceiptDeliveryService receiptDeliveryService
    ) {
        this.chargeRepository = chargeRepository;
        this.receiptService = receiptService;
        this.receiptDeliveryService = receiptDeliveryService;
    }

    @Transactional
    public AppyPayWebhookResponse process(Map<String, Object> payload) {
        String merchantTransactionId = extractString(payload, "merchantTransactionId", "merchant_transaction_id", "transactionId", "transaction_id");
        String status = extractString(payload, "status", "state", "paymentStatus", "transactionStatus");

        AppyPayWebhookResponse response = new AppyPayWebhookResponse()
                .setReceived(true)
                .setMerchantTransactionId(merchantTransactionId)
                .setProviderStatus(status);

        if (merchantTransactionId == null || merchantTransactionId.isBlank()) {
            return response.setProcessed(false).setMessage("Webhook recebido sem merchantTransactionId.");
        }

        Optional<Charge> optionalCharge = findChargeByMerchantTransactionId(merchantTransactionId);
        if (optionalCharge.isEmpty()) {
            return response.setProcessed(false).setMessage("Cobrança não encontrada.");
        }

        Charge charge = optionalCharge.get();
        response.setChargeCode(charge.getChargeCode());

        if (!isPaidStatus(status, payload)) {
            return response.setProcessed(true).setPaid(false).setMessage("Estado ainda não confirmado como pago.");
        }

        if (charge.getStatus() != ChargeStatus.PAID) {
            charge.setStatus(ChargeStatus.PAID).setPaidAt(LocalDateTime.now());
            chargeRepository.save(charge);
        }

        ReceiptResponse receipt = receiptService.issueOrFindForCharge(charge.getId());
        receiptDeliveryService.sendAfterGateway(charge, receipt);

        return response
                .setProcessed(true)
                .setPaid(true)
                .setReceiptCode(receipt.getReceiptCode())
                .setReceiptPdfUrl(receipt.getPdfUrl())
                .setMessage("Pagamento confirmado pela AppyPay, recibo emitido e envio automático acionado.");
    }

    private Optional<Charge> findChargeByMerchantTransactionId(String merchantTransactionId) {
        String normalized = normalizeMerchantTransactionId(merchantTransactionId);
        if (normalized.isBlank()) return Optional.empty();

        Optional<Charge> exact = chargeRepository.findByChargeCode(merchantTransactionId.trim());
        if (exact.isPresent()) return exact;

        return chargeRepository.findAll()
                .stream()
                .filter(charge -> normalized.equals(merchantTransactionIdFor(charge)))
                .findFirst();
    }

    private String merchantTransactionIdFor(Charge charge) {
        String source = firstNonBlank(charge.getChargeCode(), charge.getId() != null ? charge.getId().toString() : "");
        String sanitized = normalizeMerchantTransactionId(source);

        if (sanitized.isBlank()) {
            sanitized = "SP" + System.currentTimeMillis();
        }

        if (sanitized.length() <= MAX_MERCHANT_TRANSACTION_ID_LENGTH) {
            return sanitized;
        }

        return sanitized.substring(sanitized.length() - MAX_MERCHANT_TRANSACTION_ID_LENGTH);
    }

    private String normalizeMerchantTransactionId(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").trim();
    }

    private boolean isPaidStatus(String status, Map<String, Object> payload) {
        String value = firstNonBlank(status, extractString(payload, "result", "code", "message")).toLowerCase();
        return containsAny(value, List.of("success", "approved", "paid", "completed", "confirmed", "processed", "aceite", "aprovado", "pago"));
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
            for (Object value : map.values()) {
                String found = extractString(value, keys);
                if (!found.isBlank()) return found;
            }
        }
        if (source instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                String found = extractString(item, keys);
                if (!found.isBlank()) return found;
            }
        }
        return "";
    }

    private boolean containsAny(String value, List<String> terms) {
        if (value == null) return false;
        for (String term : terms) if (value.contains(term)) return true;
        return false;
    }

    private String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) if (value != null && !value.isBlank()) return value.trim();
        return "";
    }
}
