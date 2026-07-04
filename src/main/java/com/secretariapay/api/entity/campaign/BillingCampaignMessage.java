package com.secretariapay.api.entity.campaign;

import com.secretariapay.api.entity.enums.campaign.BillingCampaignMessageStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "billing_campaign_messages")
public class BillingCampaignMessage {
    @Id @GeneratedValue(strategy = GenerationType.UUID) public UUID id;
    @Column(name="campaign_id", nullable=false) public UUID campaignId;
    @Column(name="student_id") public UUID studentId;
    @Column(name="student_name", length=180) public String studentName;
    @Column(name="student_number", length=80) public String studentNumber;
    @Column(name="recipient_phone", nullable=false, length=40) public String recipientPhone;
    @Column(name="charge_id") public UUID chargeId;
    @Column(name="charge_code", length=80) public String chargeCode;
    @Column(nullable=false, columnDefinition="text") public String message;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=40) public BillingCampaignMessageStatus status=BillingCampaignMessageStatus.GENERATED;
    @Column(name="provider_message_id", length=160) public String providerMessageId;
    @Column(name="failure_reason", columnDefinition="text") public String failureReason;
    @Column(name="created_at", nullable=false) public LocalDateTime createdAt;
    @Column(name="sent_at") public LocalDateTime sentAt;
    @PrePersist public void prePersist(){ if(status==null) status=BillingCampaignMessageStatus.GENERATED; createdAt=LocalDateTime.now(); }
}
