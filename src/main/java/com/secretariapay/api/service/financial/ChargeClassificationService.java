package com.secretariapay.api.service.financial;

import com.secretariapay.api.entity.enums.financial.ChargeCategory;
import com.secretariapay.api.entity.financial.Charge;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;

@Service
public class ChargeClassificationService {

    public ChargeCategory resolveCategory(Charge charge) {
        if (charge == null) return ChargeCategory.OTHER;
        if (charge.getChargeCategory() != null && charge.getChargeCategory() != ChargeCategory.OTHER) {
            return charge.getChargeCategory();
        }
        return resolveCategory(charge.getServiceCode(), charge.getDescription(), charge.getReferenceMonth(), charge.getChargeCode());
    }

    public ChargeCategory resolveCategory(String serviceCode, String description, String referenceMonth, String chargeCode) {
        String normalizedCode = normalize(serviceCode);
        String text = normalize(String.join(" ", safe(description), safe(referenceMonth), safe(chargeCode), safe(serviceCode)));
        if ("tuition".equals(normalizedCode) || "propina".equals(normalizedCode) || text.contains("propina") || text.contains("imt propina")) {
            return ChargeCategory.TUITION;
        }
        if (isAcademicServiceCode(normalizedCode) || containsAcademicService(text)) {
            return ChargeCategory.ACADEMIC_SERVICE;
        }
        return ChargeCategory.OTHER;
    }

    public String resolveServiceCode(Charge charge) {
        if (charge == null) return "OTHER";
        if (charge.getServiceCode() != null && !charge.getServiceCode().isBlank()) {
            return normalizeCode(charge.getServiceCode());
        }
        return resolveServiceCode(charge.getDescription(), charge.getReferenceMonth(), charge.getChargeCode());
    }

    public String resolveServiceCode(String description, String referenceMonth, String chargeCode) {
        String text = normalize(String.join(" ", safe(description), safe(referenceMonth), safe(chargeCode)));
        if (text.contains("propina") || text.contains("imt propina")) return "TUITION";
        if ((text.contains("confirmacao") || text.contains("confirmar")) && text.contains("matricula")) return "ENROLLMENT_CONFIRMATION";
        if (text.contains("matricula")) return "ENROLLMENT";
        if (text.contains("inscricao")) return "REGISTRATION";
        if (text.contains("recurso")) return "RESIT_EXAM";
        if (text.contains("exame especial")) return "SPECIAL_EXAM";
        if (text.contains("declaracao") && text.contains("com nota")) return "DECLARATION_WITH_GRADES";
        if (text.contains("declaracao") && text.contains("sem nota")) return "DECLARATION_WITHOUT_GRADES";
        if (text.contains("declaracao")) return "DECLARATION_WITHOUT_GRADES";
        if (text.contains("certificado")) return "CERTIFICATE";
        if (text.contains("diploma")) return "DIPLOMA";
        return "OTHER";
    }

    public void classify(Charge charge) {
        if (charge == null) return;
        String serviceCode = resolveServiceCode(charge);
        charge.setServiceCode(serviceCode);
        charge.setChargeCategory(resolveCategory(serviceCode, charge.getDescription(), charge.getReferenceMonth(), charge.getChargeCode()));
    }

    public boolean isTuition(Charge charge) {
        return resolveCategory(charge) == ChargeCategory.TUITION;
    }

    public boolean isAcademicService(Charge charge) {
        return resolveCategory(charge) == ChargeCategory.ACADEMIC_SERVICE;
    }

    private boolean containsAcademicService(String text) {
        return text.contains("matricula")
                || text.contains("inscricao")
                || text.contains("recurso")
                || text.contains("exame especial")
                || text.contains("declaracao")
                || text.contains("certificado")
                || text.contains("diploma")
                || text.contains("imt servico");
    }

    private boolean isAcademicServiceCode(String code) {
        return switch (code) {
            case "enrollment", "enrollment confirmation", "registration", "resit exam", "special exam",
                    "declaration with grades", "declaration without grades", "certificate", "diploma" -> true;
            default -> false;
        };
    }

    private String normalizeCode(String value) {
        String normalized = normalize(value).replace(' ', '_').toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? "OTHER" : normalized;
    }

    private String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
