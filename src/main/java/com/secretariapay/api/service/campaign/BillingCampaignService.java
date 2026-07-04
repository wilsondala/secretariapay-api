package com.secretariapay.api.service.campaign;
import com.secretariapay.api.entity.campaign.*;
import com.secretariapay.api.entity.enums.campaign.*;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.campaign.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
@Service
public class BillingCampaignService {
    private final BillingCampaignRepository campaigns; private final BillingCampaignMessageRepository messages;
    public BillingCampaignService(BillingCampaignRepository campaigns, BillingCampaignMessageRepository messages){ this.campaigns=campaigns; this.messages=messages; }
    @Transactional public BillingCampaign create(BillingCampaign c){ if(c.campaignCode==null||c.campaignCode.isBlank()) c.campaignCode=code(); if(c.status==null) c.status=BillingCampaignStatus.DRAFT; return campaigns.save(c); }
    @Transactional(readOnly=true) public List<BillingCampaign> findAll(){ return campaigns.findAll().stream().sorted((a,b)->b.createdAt.compareTo(a.createdAt)).toList(); }
    @Transactional(readOnly=true) public BillingCampaign findById(UUID id){ return campaigns.findById(id).orElseThrow(()->new NotFoundException("Campanha de cobrança não encontrada.")); }
    @Transactional public BillingCampaign activate(UUID id){ BillingCampaign c=findById(id); c.status=BillingCampaignStatus.ACTIVE; c.activatedAt=LocalDateTime.now(); return campaigns.save(c); }
    @Transactional public BillingCampaign complete(UUID id){ BillingCampaign c=findById(id); c.status=BillingCampaignStatus.COMPLETED; c.completedAt=LocalDateTime.now(); return campaigns.save(c); }
    @Transactional public BillingCampaignMessage addMessage(UUID campaignId, BillingCampaignMessage m){ BillingCampaign c=findById(campaignId); m.campaignId=c.id; if(m.status==null) m.status=BillingCampaignMessageStatus.GENERATED; BillingCampaignMessage saved=messages.save(m); c.totalRecipients=(c.totalRecipients==null?0:c.totalRecipients)+1; c.totalGenerated=(c.totalGenerated==null?0:c.totalGenerated)+1; campaigns.save(c); return saved; }
    @Transactional(readOnly=true) public List<BillingCampaignMessage> findMessages(UUID campaignId){ return messages.findByCampaignIdOrderByCreatedAtAsc(campaignId); }
    private String code(){ String c; do{ c="CMP-"+System.currentTimeMillis(); }while(campaigns.existsByCampaignCode(c)); return c; }
}
