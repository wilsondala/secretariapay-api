package com.vairapido.api.service;

import com.vairapido.api.entity.enums.PassengerDocumentType;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class DocumentValidatorService {

    public String normalize(PassengerDocumentType type, String value) {
        if (value == null) {
            return "";
        }

        if (PassengerDocumentType.CPF.equals(type)) {
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
        String normalized = normalize(type, value);

        if (PassengerDocumentType.CPF.equals(type)) {
            return isValidCpf(normalized);
        }

        if (PassengerDocumentType.BI.equals(type)) {
            return isValidBi(normalized);
        }

        if (PassengerDocumentType.PASSPORT.equals(type)) {
            return isValidPassport(normalized);
        }

        return false;
    }

    public String label(PassengerDocumentType type) {
        if (PassengerDocumentType.BI.equals(type)) {
            return "BI";
        }

        if (PassengerDocumentType.PASSPORT.equals(type)) {
            return "Passaporte";
        }

        return "CPF";
    }

    public String mask(PassengerDocumentType type, String value) {
        String normalized = normalize(type, value);

        if (normalized.isBlank()) {
            return "***";
        }

        if (PassengerDocumentType.CPF.equals(type) && normalized.length() == 11) {
            return "***.***.***-" + normalized.substring(9);
        }

        if (PassengerDocumentType.BI.equals(type)) {
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

        if (!bi.matches("[A-Z0-9]{6,20}")) {
            return false;
        }

        boolean hasDigit = bi.matches(".*\\d.*");
        boolean hasLetter = bi.matches(".*[A-Z].*");

        return hasDigit && hasLetter;
    }

    private boolean isValidPassport(String passport) {
        return passport != null && passport.matches("[A-Z0-9]{6,20}");
    }
}