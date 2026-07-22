package com.secretariapay.api.repository.admission;

import com.secretariapay.api.entity.admission.AdmissionCampaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdmissionCampaignRepository extends JpaRepository<AdmissionCampaign, UUID> {
    Optional<AdmissionCampaign> findFirstByInstitutionIdAndActiveTrueOrderByRegistrationStartDesc(UUID institutionId);
    Optional<AdmissionCampaign> findByCampaignCodeIgnoreCase(String campaignCode);
    List<AdmissionCampaign> findByActiveTrueOrderByRegistrationStartDesc();
}
