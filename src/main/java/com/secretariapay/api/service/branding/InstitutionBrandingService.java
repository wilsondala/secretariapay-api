package com.secretariapay.api.service.branding;

import com.secretariapay.api.dto.branding.InstitutionBrandingResponse;
import com.secretariapay.api.entity.academic.Institution;
import com.secretariapay.api.entity.academic.InstitutionSettings;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.academic.InstitutionSettingsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class InstitutionBrandingService {

    private final InstitutionSettingsRepository institutionSettingsRepository;
    private final String publicBaseUrl;

    public InstitutionBrandingService(
            InstitutionSettingsRepository institutionSettingsRepository,
            @Value("${secretariapay.public-base-url:https://secretariapay-api.paixaoangola.com}") String publicBaseUrl
    ) {
        this.institutionSettingsRepository = institutionSettingsRepository;
        this.publicBaseUrl = normalizeBaseUrl(publicBaseUrl);
    }

    public InstitutionBrandingResponse getBySlug(String slug) {
        InstitutionSettings settings = institutionSettingsRepository.findByPublicSlug(slug)
                .orElseThrow(() -> new NotFoundException("Branding público da instituição não encontrado."));

        Institution institution = settings.getInstitution();

        return new InstitutionBrandingResponse()
                .setInstitutionName(institution.getName())
                .setLegalName(resolveLegalName(institution))
                .setSlug(settings.getPublicSlug())
                .setSlogan(resolveSlogan(settings.getPublicSlug()))
                .setAddress(institution.getAddress())
                .setCountry(settings.getCountry())
                .setCurrency(settings.getCurrency())
                .setTimezone(settings.getTimezone())
                .setOfficialWhatsapp(resolveWhatsapp(settings, institution))
                .setSupportEmail(resolveSupportEmail(settings, institution))
                .setAcademicPortalBaseUrl(settings.getAcademicPortalBaseUrl())
                .setPlatform("SecretáriaPay Académico")
                .setPlatformLogoUrl(publicBaseUrl + "/branding/secretariapay-logo.png")
                .setPrimaryColor(resolvePrimaryColor(settings.getPublicSlug()))
                .setSecondaryColor(resolveSecondaryColor(settings.getPublicSlug()))
                .setAccentColor(resolveAccentColor(settings.getPublicSlug()))
                .setActive(Boolean.TRUE.equals(institution.getActive()) && Boolean.TRUE.equals(settings.getActive()));
    }

    private String resolveLegalName(Institution institution) {
        if (institution.getLegalName() != null && !institution.getLegalName().isBlank()) {
            return institution.getLegalName();
        }
        return institution.getName();
    }

    private String resolveWhatsapp(InstitutionSettings settings, Institution institution) {
        if (settings.getOfficialWhatsapp() != null && !settings.getOfficialWhatsapp().isBlank()) {
            return settings.getOfficialWhatsapp();
        }
        return institution.getWhatsapp();
    }

    private String resolveSupportEmail(InstitutionSettings settings, Institution institution) {
        if (settings.getSupportEmail() != null && !settings.getSupportEmail().isBlank()) {
            return settings.getSupportEmail();
        }
        return institution.getEmail();
    }

    private String resolveSlogan(String slug) {
        if ("imetro".equalsIgnoreCase(slug)) {
            return "A Marca da Educação";
        }
        return "Educação, gestão e inovação";
    }

    private String resolvePrimaryColor(String slug) {
        return "#0B3B82";
    }

    private String resolveSecondaryColor(String slug) {
        if ("imetro".equalsIgnoreCase(slug)) {
            return "#D4AF37";
        }
        return "#16A34A";
    }

    private String resolveAccentColor(String slug) {
        if ("imetro".equalsIgnoreCase(slug)) {
            return "#16A34A";
        }
        return "#D4AF37";
    }

    private String normalizeBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            return "https://secretariapay-api.paixaoangola.com";
        }

        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        return trimmed;
    }
}
