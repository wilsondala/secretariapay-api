package com.secretariapay.api.repository.config;

import com.secretariapay.api.entity.config.InstitutionDcrPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InstitutionDcrPolicyRepository extends JpaRepository<InstitutionDcrPolicy, UUID> {

    Optional<InstitutionDcrPolicy> findByInstitutionIdAndActiveTrue(UUID institutionId);

    Optional<InstitutionDcrPolicy> findByPolicyCode(String policyCode);
}
