package com.secretariapay.api.dto.appypay;

import java.util.LinkedHashMap;
import java.util.Map;

public class AppyPayProviderResponse {

    private boolean success;
    private Integer httpStatus;
    private String errorMessage;
    private Map<String, Object> body = new LinkedHashMap<>();

    public boolean isSuccess() {
        return success;
    }

    public AppyPayProviderResponse setSuccess(boolean success) {
        this.success = success;
        return this;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public AppyPayProviderResponse setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
        return this;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public AppyPayProviderResponse setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public Map<String, Object> getBody() {
        return body;
    }

    public AppyPayProviderResponse setBody(Map<String, Object> body) {
        this.body = body == null ? new LinkedHashMap<>() : body;
        return this;
    }
}
