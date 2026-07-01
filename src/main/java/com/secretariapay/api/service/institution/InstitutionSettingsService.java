package com.secretariapay.api.service.institution;

import com.secretariapay.api.dto.institution.InstitutionSettingsRequest;
import com.secretariapay.api.dto.institution.InstitutionSettingsResponse;
import com.secretariapay.api.entity.academic.Institution;
import com.secretariapay.api.entity.academic.InstitutionSettings;
import com.secretariapay.api.entity.enums.institution.SubscriptionPlan;
import com.secretariapay.api.entity.enums.institution.SubscriptionStatus;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.academic.InstitutionRepository;
import com.secretariapay.api.repository.academic.InstitutionSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InstitutionSettingsService {

    private final InstitutionSettingsRepository settingsRepository;
    private final InstitutionRepository institutionRepository;

    public InstitutionSettingsService(
            InstitutionSettingsRepository settingsRepository,
            InstitutionRepository institutionRepository
    ) {
        this.settingsRepository = settingsRepository;
        this.institutionRepository = institutionRepository;
    }

    @Transactional
    public InstitutionSettingsResponse createOrUpdate(UUID institutionId, InstitutionSettingsRequest request) {
        Institution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new NotFoundException("Instituição não encontrada."));

        String slug = normalizeSlug(request.getPublicSlug());

        InstitutionSettings settings = settingsRepository.findByInstitutionId(institutionId)
                .orElseGet(() -> new InstitutionSettings().setInstitution(institution));

        boolean slugInUseByAnotherInstitution = settingsRepository.findByPublicSlug(slug)
                .filter(existing -> !existing.getInstitution().getId().equals(institutionId))
                .isPresent();

        if (slugInUseByAnotherInstitution) {
            throw new IllegalArgumentException("Já existe uma instituição usando este slug público.");
        }

        settings
                .setPublicSlug(slug)
                .setOfficialWhatsapp(request.getOfficialWhatsapp())
                .setSupportEmail(request.getSupportEmail())
                .setTimezone(defaultIfBlank(request.getTimezone(), "Africa/Luanda"))
                .setCountry(defaultIfBlank(request.getCountry(), "AO"))
                .setCurrency(defaultIfBlank(request.getCurrency(), "AOA"))
                .setSubscriptionPlan(request.getSubscriptionPlan() == null ? SubscriptionPlan.PILOT : request.getSubscriptionPlan())
                .setSubscriptionStatus(request.getSubscriptionStatus() == null ? SubscriptionStatus.TRIAL : request.getSubscriptionStatus())
                .setAcademicPortalBaseUrl(request.getAcademicPortalBaseUrl())
                .setAllowAcademicBlocking(request.getAllowAcademicBlocking() != null && request.getAllowAcademicBlocking())
                .setAutoUnblockAfterPayment(request.getAutoUnblockAfterPayment() == null || request.getAutoUnblockAfterPayment())
                .setPaymentGraceDays(request.getPaymentGraceDays() == null ? 5 : request.getPaymentGraceDays())
                .setMonthlyDueDay(request.getMonthlyDueDay() == null ? 10 : request.getMonthlyDueDay())
                .setActive(request.getActive() == null || request.getActive());

        return toResponse(settingsRepository.save(settings));
    }

    @Transactional(readOnly = true)
    public List<InstitutionSettingsResponse> findAll() {
        return settingsRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InstitutionSettingsResponse findByInstitutionId(UUID institutionId) {
        InstitutionSettings settings = settingsRepository.findByInstitutionId(institutionId)
                .orElseThrow(() -> new NotFoundException("Configurações da instituição não encontradas."));

        return toResponse(settings);
    }

    @Transactional(readOnly = true)
    public InstitutionSettingsResponse findByPublicSlug(String publicSlug) {
        InstitutionSettings settings = settingsRepository.findByPublicSlug(normalizeSlug(publicSlug))
                .orElseThrow(() -> new NotFoundException("Instituição não encontrada para este slug."));

        return toResponse(settings);
    }

    private InstitutionSettingsResponse toResponse(InstitutionSettings settings) {
        Institution institution = settings.getInstitution();

        return new InstitutionSettingsResponse()
                .setId(settings.getId())
                .setInstitutionId(institution.getId())
                .setInstitutionName(institution.getName())
                .setPublicSlug(settings.getPublicSlug())
                .setOfficialWhatsapp(settings.getOfficialWhatsapp())
                .setSupportEmail(settings.getSupportEmail())
                .setTimezone(settings.getTimezone())
                .setCountry(settings.getCountry())
                .setCurrency(settings.getCurrency())
                .setSubscriptionPlan(settings.getSubscriptionPlan())
                .setSubscriptionStatus(settings.getSubscriptionStatus())
                .setAcademicPortalBaseUrl(settings.getAcademicPortalBaseUrl())
                .setAllowAcademicBlocking(settings.getAllowAcademicBlocking())
                .setAutoUnblockAfterPayment(settings.getAutoUnblockAfterPayment())
                .setPaymentGraceDays(settings.getPaymentGraceDays())
                .setMonthlyDueDay(settings.getMonthlyDueDay())
                .setActive(settings.getActive())
                .setCreatedAt(settings.getCreatedAt())
                .setUpdatedAt(settings.getUpdatedAt());
    }

    private String normalizeSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("O slug público é obrigatório.");
        }

        return slug.trim()
                .toLowerCase()
                .replace(" ", "-")
                .replace("_", "-");
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
