package com.secretariapay.api.repository.campaign;
import com.secretariapay.api.entity.campaign.BillingCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface BillingCampaignRepository extends JpaRepository<BillingCampaign, UUID> { boolean existsByCampaignCode(String campaignCode); }
