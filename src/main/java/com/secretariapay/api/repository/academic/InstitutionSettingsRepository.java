package com.secretariapay.api.repository.academic;

import com.secretariapay.api.entity.academic.InstitutionSettings;
import com.secretariapay.api.entity.enums.institution.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InstitutionSettingsRepository extends JpaRepository<InstitutionSettings, UUID> {

    Optional<InstitutionSettings> findByInstitutionId(UUID institutionId);

    Optional<InstitutionSettings> findByPublicSlug(String publicSlug);

    boolean existsByPublicSlug(String publicSlug);

    List<InstitutionSettings> findBySubscriptionStatusOrderByCreatedAtDesc(SubscriptionStatus subscriptionStatus);

    List<InstitutionSettings> findByActiveTrueOrderByCreatedAtDesc();
}
