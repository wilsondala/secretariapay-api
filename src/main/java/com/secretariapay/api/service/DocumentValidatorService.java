package com.secretariapay.api.service;

import com.secretariapay.api.config.BusinessCountryProperties;
import com.secretariapay.api.entity.enums.PassengerDocumentType;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class DocumentValidatorService {

    private final BusinessCountryProperties businessCountryProperties;

    public DocumentValidatorService(BusinessCountryProperties businessCountryProperties) {
        this.businessCountryProperties = businessCountryProperties;
    }

    public PassengerDocumentType defaultDocumentType() {
        return businessCountryProperties.getDefaultDocumentType();
    }

    public String defaultDocumentLabel() {
        return label(defaultDocumentType());
    }

    public String defaultCurrency() {
        return businessCountryProperties.getCurrency();
    }

    public String normalize(PassengerDocumentType type, String value) {
        PassengerDocumentType resolvedType = resolveType(type);

        if (value == null) {
            return "";
        }

        if (PassengerDocumentType.CPF.equals(resolvedType)) {
            return value.replaceAll("\\D", "");
        }

        return value
                .trim()
                .replace(" ", "")
                .replace("-", "")
                .replace(".", "")
                .toUpperCase(Locale.ROOT);
    }

    public boolean isValid(PassengerDocumentType type, String value) {
        PassengerDocumentType resolvedType = resolveType(type);
        String normalized = normalize(resolvedType, value);

        if (PassengerDocumentType.CPF.equals(resolvedType)) {
            return isValidCpf(normalized);
        }

        if (PassengerDocumentType.BI.equals(resolvedType)) {
            return isValidBi(normalized);
        }

        if (PassengerDocumentType.PASSPORT.equals(resolvedType)) {
            return isValidPassport(normalized);
        }

        return false;
    }

    public String label(PassengerDocumentType type) {
        PassengerDocumentType resolvedType = resolveType(type);

        if (PassengerDocumentType.BI.equals(resolvedType)) {
            return "BI";
        }

        if (PassengerDocumentType.PASSPORT.equals(resolvedType)) {
            return "Passaporte";
        }

        return "CPF";
    }

    public String mask(PassengerDocumentType type, String value) {
        PassengerDocumentType resolvedType = resolveType(type);
        String normalized = normalize(resolvedType, value);

        if (normalized.isBlank()) {
            return "***";
        }

        if (PassengerDocumentType.CPF.equals(resolvedType) && normalized.length() == 11) {
            return "***.***.***-" + normalized.substring(9);
        }

        if (PassengerDocumentType.BI.equals(resolvedType)) {
            if (normalized.length() <= 6) {
                return "***" + normalized.substring(Math.max(0, normalized.length() - 2));
            }

            return normalized.substring(0, Math.min(3, normalized.length()))
                    + "****"
                    + normalized.substring(Math.max(3, normalized.length() - 4));
        }

        if (normalized.length() > 4) {
            return "***" + normalized.substring(normalized.length() - 4);
        }

        return "***";
    }

    public String example(PassengerDocumentType type) {
        PassengerDocumentType resolvedType = resolveType(type);

        if (PassengerDocumentType.BI.equals(resolvedType)) {
            return "001058899UE035";
        }

        if (PassengerDocumentType.PASSPORT.equals(resolvedType)) {
            return "A1234567";
        }

        return "52998224725";
    }

    private PassengerDocumentType resolveType(PassengerDocumentType type) {
        if (type == null) {
            return defaultDocumentType();
        }

        return type;
    }

    private boolean isValidCpf(String cpf) {
        if (cpf == null || !cpf.matches("\\d{11}")) {
            return false;
        }

        if (cpf.chars().distinct().count() == 1) {
            return false;
        }

        int firstDigit = calculateCpfDigit(cpf.substring(0, 9), 10);
        int secondDigit = calculateCpfDigit(cpf.substring(0, 9) + firstDigit, 11);

        return cpf.equals(cpf.substring(0, 9) + firstDigit + secondDigit);
    }

    private int calculateCpfDigit(String base, int weight) {
        int sum = 0;

        for (int i = 0; i < base.length(); i++) {
            int number = Character.getNumericValue(base.charAt(i));
            sum += number * (weight - i);
        }

        int remainder = sum % 11;

        return remainder < 2 ? 0 : 11 - remainder;
    }

    private boolean isValidBi(String bi) {
        if (bi == null || bi.isBlank()) {
            return false;
        }

        String normalized = normalize(PassengerDocumentType.BI, bi);

        /*
         * Padrão BI Angola:
         * 9 números + 2 letras + 3 números
         *
         * Exemplo:
         * 001058899UE035
         * 006543219LA042
         */
        return normalized.matches("\\d{9}[A-Z]{2}\\d{3}");
    }

    private boolean isValidPassport(String passport) {
        return passport != null && passport.matches("[A-Z0-9]{6,20}");
    }
}
