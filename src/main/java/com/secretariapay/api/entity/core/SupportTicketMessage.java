
package com.secretariapay.api.entity.core;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity @Table(name="support_ticket_messages")
public class SupportTicketMessage {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="ticket_id",nullable=false) private SupportTicket ticket;
 @Column(name="sender_type",nullable=false,length=30) private String senderType="STUDENT"; @Column(name="sender_name",length=140) private String senderName; @Column(nullable=false,columnDefinition="text") private String message; @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
 @PrePersist public void prePersist(){createdAt=LocalDateTime.now(); if(senderType==null)senderType="STUDENT";}
 public UUID getId(){return id;} public SupportTicket getTicket(){return ticket;} public SupportTicketMessage setTicket(SupportTicket v){ticket=v;return this;} public String getSenderType(){return senderType;} public SupportTicketMessage setSenderType(String v){senderType=v;return this;} public String getSenderName(){return senderName;} public SupportTicketMessage setSenderName(String v){senderName=v;return this;} public String getMessage(){return message;} public SupportTicketMessage setMessage(String v){message=v;return this;} public LocalDateTime getCreatedAt(){return createdAt;}
}
