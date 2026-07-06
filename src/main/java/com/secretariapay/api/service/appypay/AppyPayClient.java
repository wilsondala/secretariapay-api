package com.secretariapay.api.service.appypay;

import com.secretariapay.api.dto.appypay.AppyPayProviderResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AppyPayClient {

    private final RestClient restClient;
    private final AppyPayTokenService tokenService;
    private final boolean enabled;
    private final String apiBaseUrl;

    public AppyPayClient(
            RestClient.Builder restClientBuilder,
            AppyPayTokenService tokenService,
            @Value("${APPYPAY_ENABLED:false}") boolean enabled,
            @Value("${APPYPAY_API_BASE_URL:}") String apiBaseUrl
    ) {
        this.restClient = restClientBuilder.build();
        this.tokenService = tokenService;
        this.enabled = enabled;
        this.apiBaseUrl = stripTrailingSlash(apiBaseUrl);
    }

    public AppyPayProviderResponse createCharge(Map<String, Object> payload) {
        return post("/v2.0/charges", payload);
    }

    public AppyPayProviderResponse findCharge(String merchantTransactionId) {
        AppyPayProviderResponse byPath = get("/v2.0/charges/" + merchantTransactionId.trim());
        if (byPath.isSuccess() || byPath.getHttpStatus() == null || byPath.getHttpStatus() != 404) return byPath;
        return get("/v2.0/charges?merchantTransactionId=" + merchantTransactionId.trim());
    }

    public AppyPayProviderResponse processReferenceInSandbox(String entity, String referenceNumber) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("entity", entity);
        payload.put("referenceNumber", referenceNumber);
        return post("/v2.0/mocks/referenceProcessing", payload);
    }

    private AppyPayProviderResponse post(String path, Map<String, Object> payload) {
        AppyPayProviderResponse configurationError = validateConfiguration();
        if (configurationError != null) return configurationError;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(apiBaseUrl + path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.getToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);
            return ok(200, response);
        } catch (RestClientResponseException exception) {
            return failed(exception.getStatusCode().value(), safeBody(exception.getResponseBodyAsString()));
        } catch (Exception exception) {
            return failed(null, exception.getMessage());
        }
    }

    private AppyPayProviderResponse get(String path) {
        AppyPayProviderResponse configurationError = validateConfiguration();
        if (configurationError != null) return configurationError;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri(apiBaseUrl + path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.getToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Map.class);
            return ok(200, response);
        } catch (RestClientResponseException exception) {
            return failed(exception.getStatusCode().value(), safeBody(exception.getResponseBodyAsString()));
        } catch (Exception exception) {
            return failed(null, exception.getMessage());
        }
    }

    private AppyPayProviderResponse validateConfiguration() {
        if (!enabled) return failed(null, "Integração AppyPay desativada.");
        if (isBlank(apiBaseUrl)) return failed(null, "Configuração AppyPay incompleta.");
        return null;
    }

    private AppyPayProviderResponse ok(Integer httpStatus, Map<String, Object> body) { return new AppyPayProviderResponse().setSuccess(true).setHttpStatus(httpStatus).setBody(body); }
    private AppyPayProviderResponse failed(Integer httpStatus, String errorMessage) { return new AppyPayProviderResponse().setSuccess(false).setHttpStatus(httpStatus).setErrorMessage(errorMessage); }
    private String stripTrailingSlash(String value) { if (value == null || value.isBlank()) return ""; return value.endsWith("/") ? value.substring(0, value.length() - 1) : value.trim(); }
    private boolean isBlank(String value) { return value == null || value.isBlank(); }
    private String safeBody(String body) { if (body == null || body.isBlank()) return "sem detalhes"; return body.length() > 700 ? body.substring(0, 700) + "..." : body; }
}
