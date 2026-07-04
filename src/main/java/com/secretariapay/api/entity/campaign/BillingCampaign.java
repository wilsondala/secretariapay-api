package com.secretariapay.api.entity.campaign;

import com.secretariapay.api.entity.enums.campaign.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "billing_campaigns")
public class BillingCampaign {
    @Id @GeneratedValue(strategy = GenerationType.UUID) public UUID id;
    @Column(name="campaign_code", nullable=false, unique=true, length=60) public String campaignCode;
    @Column(name="institution_id") public UUID institutionId;
    @Column(nullable=false, length=180) public String name;
    @Enumerated(EnumType.STRING) @Column(name="campaign_type", nullable=false, length=60) public BillingCampaignType campaignType;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=80) public BillingCampaignAudience audience;
    @Column(nullable=false, length=40) public String channel="WHATSAPP";
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=40) public BillingCampaignStatus status=BillingCampaignStatus.DRAFT;
    @Column(length=180) public String title;
    @Column(name="message_template", nullable=false, columnDefinition="text") public String messageTemplate;
    @Column(name="scheduled_for") public LocalDateTime scheduledFor;
    @Column(name="created_by", length=120) public String createdBy;
    @Column(name="total_recipients", nullable=false) public Integer totalRecipients=0;
    @Column(name="total_generated", nullable=false) public Integer totalGenerated=0;
    @Column(name="total_sent", nullable=false) public Integer totalSent=0;
    @Column(name="total_failed", nullable=false) public Integer totalFailed=0;
    @Column(name="created_at", nullable=false) public LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false) public LocalDateTime updatedAt;
    @Column(name="activated_at") public LocalDateTime activatedAt;
    @Column(name="completed_at") public LocalDateTime completedAt;
    @PrePersist public void prePersist(){ LocalDateTime now=LocalDateTime.now(); if(campaignCode==null||campaignCode.isBlank()) campaignCode="CMP-"+System.currentTimeMillis(); if(status==null) status=BillingCampaignStatus.DRAFT; if(channel==null||channel.isBlank()) channel="WHATSAPP"; if(totalRecipients==null) totalRecipients=0; if(totalGenerated==null) totalGenerated=0; if(totalSent==null) totalSent=0; if(totalFailed==null) totalFailed=0; createdAt=now; updatedAt=now; }
    @PreUpdate public void preUpdate(){ updatedAt=LocalDateTime.now(); }
}
