package com.secretariapay.api.dto.gateway;

import java.util.LinkedHashMap;
import java.util.Map;

public class GatewayProviderResponse {

    private boolean success;
    private Integer httpStatus;
    private String errorMessage;
    private Map<String, Object> body = new LinkedHashMap<>();

    public boolean isSuccess() { return success; }
    public GatewayProviderResponse setSuccess(boolean success) { this.success = success; return this; }
    public Integer getHttpStatus() { return httpStatus; }
    public GatewayProviderResponse setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; return this; }
    public String getErrorMessage() { return errorMessage; }
    public GatewayProviderResponse setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
    public Map<String, Object> getBody() { return body; }
    public GatewayProviderResponse setBody(Map<String, Object> body) { this.body = body == null ? new LinkedHashMap<>() : body; return this; }
}
