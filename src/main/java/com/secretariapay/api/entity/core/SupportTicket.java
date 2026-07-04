
package com.secretariapay.api.entity.core;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity @Table(name="support_tickets")
public class SupportTicket {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @Column(name="protocol_code",nullable=false,unique=true,length=60) private String protocolCode;
 @Column(name="student_id") private UUID studentId; @Column(name="student_name",length=180) private String studentName; @Column(name="student_number",length=80) private String studentNumber;
 @Column(name="requester_phone",length=40) private String requesterPhone; @Column(nullable=false,length=160) private String subject; @Column(length=120) private String reason; @Column(columnDefinition="text") private String description;
 @Column(nullable=false,length=30) private String priority="NORMAL"; @Column(nullable=false,length=30) private String status="OPEN"; @Column(name="assigned_to",length=120) private String assignedTo;
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt; @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt; @Column(name="closed_at") private LocalDateTime closedAt;
 @PrePersist public void prePersist(){LocalDateTime n=LocalDateTime.now();createdAt=n;updatedAt=n;if(status==null)status="OPEN";if(priority==null)priority="NORMAL";}
 @PreUpdate public void preUpdate(){updatedAt=LocalDateTime.now();}
 public UUID getId(){return id;} public String getProtocolCode(){return protocolCode;} public SupportTicket setProtocolCode(String v){protocolCode=v;return this;} public UUID getStudentId(){return studentId;} public SupportTicket setStudentId(UUID v){studentId=v;return this;} public String getStudentName(){return studentName;} public SupportTicket setStudentName(String v){studentName=v;return this;} public String getStudentNumber(){return studentNumber;} public SupportTicket setStudentNumber(String v){studentNumber=v;return this;} public String getRequesterPhone(){return requesterPhone;} public SupportTicket setRequesterPhone(String v){requesterPhone=v;return this;} public String getSubject(){return subject;} public SupportTicket setSubject(String v){subject=v;return this;} public String getReason(){return reason;} public SupportTicket setReason(String v){reason=v;return this;} public String getDescription(){return description;} public SupportTicket setDescription(String v){description=v;return this;} public String getPriority(){return priority;} public SupportTicket setPriority(String v){priority=v;return this;} public String getStatus(){return status;} public SupportTicket setStatus(String v){status=v;return this;} public String getAssignedTo(){return assignedTo;} public SupportTicket setAssignedTo(String v){assignedTo=v;return this;} public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;} public LocalDateTime getClosedAt(){return closedAt;} public SupportTicket setClosedAt(LocalDateTime v){closedAt=v;return this;}
}
