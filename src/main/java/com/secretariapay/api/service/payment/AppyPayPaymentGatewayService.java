package com.secretariapay.api.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secretariapay.api.config.AppyPayProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AppyPayPaymentGatewayService {

    private static final int MAX_MERCHANT_TRANSACTION_ID_LENGTH = 15;

    private final AppyPayProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private String cachedToken;
    private Instant tokenExpiresAt = Instant.EPOCH;

    public AppyPayPaymentGatewayService(AppyPayProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    public AppyPayChargeResponse createMulticaixaExpressCharge(
            BigDecimal amount,
            String description,
            String merchantTransactionId,
            String phoneNumber
    ) {
        String paymentMethod = clean(properties.getGpoPaymentMethod());
        Map<String, Object> paymentInfo = new LinkedHashMap<>();
        paymentInfo.put("phoneNumber", normalizeAngolaPhone(phoneNumber));
        return createCharge(amount, description, merchantTransactionId, paymentMethod, paymentInfo, "GPO");
    }

    public AppyPayChargeResponse createReferenceCharge(
            BigDecimal amount,
            String description,
            String merchantTransactionId
    ) {
        String paymentMethod = clean(properties.getRefPaymentMethod());
        return createCharge(amount, description, merchantTransactionId, paymentMethod, null, "REF");
    }

    private AppyPayChargeResponse createCharge(
            BigDecimal amount,
            String description,
            String merchantTransactionId,
            String paymentMethod,
            Map<String, Object> paymentInfo,
            String methodLabel
    ) {
        String safeMerchantTransactionId = normalizeMerchantTransactionId(merchantTransactionId);

        if (!properties.isEnabled()) {
            return prepared(methodLabel, safeMerchantTransactionId, amount, "AppyPay está preparado, mas ainda está desativado por configuração.");
        }

        if (isBlank(properties.getClientId()) || isBlank(properties.getClientSecret())) {
            return prepared(methodLabel, safeMerchantTransactionId, amount, "Credenciais AppyPay não configuradas no servidor.");
        }

        if (isBlank(paymentMethod)) {
            return prepared(methodLabel, safeMerchantTransactionId, amount, "PaymentMethod AppyPay não configurado para " + methodLabel + ".");
        }

        try {
            String token = getAccessToken();

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("amount", amount);
            body.put("currency", "AOA");
            body.put("description", clean(description));
            body.put("merchantTransactionId", safeMerchantTransactionId);
            body.put("paymentMethod", paymentMethod);
            if (paymentInfo != null && !paymentInfo.isEmpty()) {
                body.put("paymentInfo", paymentInfo);
            }

            String raw = restClient.post()
                    .uri(properties.getChargesUrl())
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> providerData = parseJsonMap(raw);
            String successMessage = buildSuccessMessage(methodLabel, raw);

            return new AppyPayChargeResponse()
                    .setSuccess(true)
                    .setAppyPayEnabled(true)
                    .setStatus("CREATED")
                    .setMessage(successMessage)
                    .setPaymentMethod(methodLabel)
                    .setMerchantTransactionId(safeMerchantTransactionId)
                    .setProviderChargeId(extractProviderChargeId(raw))
                    .setAmount(amount)
                    .setCurrency("AOA")
                    .setRawResponse(raw)
                    .setProviderData(providerData);
        } catch (Exception ex) {
            return new AppyPayChargeResponse()
                    .setSuccess(false)
                    .setAppyPayEnabled(properties.isEnabled())
                    .setStatus("FAILED")
                    .setMessage(summarizeProviderError(ex.getMessage()))
                    .setPaymentMethod(methodLabel)
                    .setMerchantTransactionId(safeMerchantTransactionId)
                    .setAmount(amount)
                    .setCurrency("AOA");
        }
    }

    private synchronized String getAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiresAt.minusSeconds(60))) {
            return cachedToken;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        form.add("resource", properties.getResource());

        String raw = restClient.post()
                .uri(properties.getTokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(raw);
            cachedToken = root.path("access_token").asText("");
            long expiresIn = root.path("expires_in").asLong(3600);
            tokenExpiresAt = Instant.now().plusSeconds(expiresIn);
            return cachedToken;
        } catch (Exception ex) {
            throw new IllegalStateException("Não foi possível interpretar token AppyPay: " + ex.getMessage(), ex);
        }
    }

    private AppyPayChargeResponse prepared(String method, String merchantTransactionId, BigDecimal amount, String message) {
        return new AppyPayChargeResponse()
                .setSuccess(false)
                .setAppyPayEnabled(properties.isEnabled())
                .setStatus("PREPARED")
                .setMessage(message)
                .setPaymentMethod(method)
                .setMerchantTransactionId(merchantTransactionId)
                .setAmount(amount)
                .setCurrency("AOA");
    }

    private String buildSuccessMessage(String methodLabel, String raw) {
        String base = "Cobrança criada no AppyPay Sandbox.";
        if (raw == null || raw.isBlank()) {
            return base;
        }

        try {
            JsonNode root = objectMapper.readTree(raw);
            String entity = findFirstText(root, "entity", "referenceEntity", "entidade", "entityCode");
            String reference = findFirstText(root, "reference", "paymentReference", "referencia", "ref", "referenceNumber");
            String expiresAt = findFirstText(root, "expiresAt", "expireAt", "expiresIn", "expirationDate", "expiryDate", "validUntil", "expireDate");
            String providerStatus = findFirstText(root, "status", "state");

            StringBuilder message = new StringBuilder(base);
            if ("REF".equalsIgnoreCase(methodLabel) && (!entity.isBlank() || !reference.isBlank() || !expiresAt.isBlank())) {
                message.append("\n\n🏦 Dados para pagamento por referência:");
                if (!entity.isBlank()) {
                    message.append("\nEntidade: ").append(entity);
                }
                if (!reference.isBlank()) {
                    message.append("\nReferência: ").append(reference);
                }
                if (!expiresAt.isBlank()) {
                    message.append("\nExpira em: ").append(expiresAt);
                }
            } else if ("GPO".equalsIgnoreCase(methodLabel) && !providerStatus.isBlank()) {
                message.append("\n\nEstado do processador: ").append(providerStatus);
            }

            return message.toString();
        } catch (Exception ignored) {
            return base;
        }
    }

    private String summarizeProviderError(String errorMessage) {
        String safeMessage = clean(errorMessage);
        if (safeMessage.isBlank()) {
            return "Falha ao criar cobrança AppyPay.";
        }

        String providerMessage = extractJsonField(safeMessage, "message");
        String providerCode = extractJsonField(safeMessage, "code");
        if (!providerMessage.isBlank()) {
            StringBuilder builder = new StringBuilder("Falha ao criar cobrança AppyPay.");
            if (!providerCode.isBlank()) {
                builder.append(" Código: ").append(providerCode).append(".");
            }
            builder.append(" Motivo: ").append(providerMessage);
            return builder.toString();
        }

        return safeMessage.length() > 420 ? safeMessage.substring(0, 420) + "..." : safeMessage;
    }

    private String extractJsonField(String text, String field) {
        String marker = "\"" + field + "\":\"";
        int start = text.indexOf(marker);
        if (start < 0) {
            return "";
        }
        start += marker.length();
        int end = text.indexOf("\"", start);
        if (end <= start) {
            return "";
        }
        return text.substring(start, end);
    }

    private String normalizeMerchantTransactionId(String merchantTransactionId) {
        String normalized = clean(merchantTransactionId).replaceAll("[^A-Za-z0-9]", "");
        if (normalized.isBlank()) {
            normalized = "SPAY" + UUID.randomUUID().toString().replaceAll("[^A-Za-z0-9]", "");
        }
        if (normalized.length() > MAX_MERCHANT_TRANSACTION_ID_LENGTH) {
            normalized = normalized.substring(0, MAX_MERCHANT_TRANSACTION_ID_LENGTH);
        }
        return normalized;
    }

    private String extractProviderChargeId(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            JsonNode root = objectMapper.readTree(raw);
            String id = findFirstText(root, "id", "chargeId", "paymentId", "transactionId");
            return id.isBlank() ? null : id;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String findFirstText(JsonNode node, String... keys) {
        if (node == null || node.isNull()) {
            return "";
        }

        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                for (String key : keys) {
                    if (normalizeKey(field.getKey()).equals(normalizeKey(key)) && isTextualValue(field.getValue())) {
                        return field.getValue().asText("").trim();
                    }
                }
            }

            fields = node.fields();
            while (fields.hasNext()) {
                String found = findFirstText(fields.next().getValue(), keys);
                if (!found.isBlank()) {
                    return found;
                }
            }
        }

        if (node.isArray()) {
            for (JsonNode item : node) {
                String found = findFirstText(item, keys);
                if (!found.isBlank()) {
                    return found;
                }
            }
        }

        return "";
    }

    private boolean isTextualValue(JsonNode value) {
        return value != null && !value.isNull() && (value.isTextual() || value.isNumber() || value.isBoolean());
    }

    private String normalizeKey(String value) {
        return clean(value).replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    }

    private Map<String, Object> parseJsonMap(String raw) {
        if (raw == null || raw.isBlank()) return new LinkedHashMap<>();
        try {
            return objectMapper.readValue(raw, new com.fasterxml.jackson.core.type.TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ignored) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("raw", raw);
            return map;
        }
    }

    private String normalizeAngolaPhone(String phoneNumber) {
        String digits = clean(phoneNumber).replaceAll("[^0-9]", "");
        if (digits.startsWith("244") && digits.length() > 9) {
            return digits.substring(digits.length() - 9);
        }
        return digits;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
