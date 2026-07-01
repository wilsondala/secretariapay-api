package com.secretariapay.api.config;

import com.secretariapay.api.entity.enums.PassengerDocumentType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@ConfigurationProperties(prefix = "vairapido.business")
public class BusinessCountryProperties {

    private String country = "BR";
    private String currency = "BRL";

    public String getCountry() {
        return country;
    }

    public BusinessCountryProperties setCountry(String country) {
        this.country = normalizeCountry(country);
        this.currency = resolveDefaultCurrency(this.country);
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public BusinessCountryProperties setCurrency(String currency) {
        if (currency != null && !currency.isBlank()) {
            this.currency = currency.trim().toUpperCase(Locale.ROOT);
        }

        return this;
    }

    public boolean isBrazil() {
        return "BR".equalsIgnoreCase(country);
    }

    public boolean isAngola() {
        return "AO".equalsIgnoreCase(country);
    }

    public PassengerDocumentType getDefaultDocumentType() {
        if (isAngola()) {
            return PassengerDocumentType.BI;
        }

        return PassengerDocumentType.CPF;
    }

    public String getDefaultDocumentLabel() {
        if (PassengerDocumentType.BI.equals(getDefaultDocumentType())) {
            return "BI";
        }

        return "CPF";
    }

    private String normalizeCountry(String value) {
        if (value == null || value.isBlank()) {
            return "BR";
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);

        if ("ANGOLA".equals(normalized)) {
            return "AO";
        }

        if ("BRASIL".equals(normalized) || "BRAZIL".equals(normalized)) {
            return "BR";
        }

        if (!"AO".equals(normalized) && !"BR".equals(normalized)) {
            return "BR";
        }

        return normalized;
    }

    private String resolveDefaultCurrency(String country) {
        if ("AO".equalsIgnoreCase(country)) {
            return "AOA";
        }

        return "BRL";
    }
}
