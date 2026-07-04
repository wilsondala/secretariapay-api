package com.secretariapay.api.repository.campaign;
import com.secretariapay.api.entity.campaign.BillingCampaignMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface BillingCampaignMessageRepository extends JpaRepository<BillingCampaignMessage, UUID> { List<BillingCampaignMessage> findByCampaignIdOrderByCreatedAtAsc(UUID campaignId); }
