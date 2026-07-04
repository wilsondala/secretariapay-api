package com.secretariapay.api.entity.academic;

import com.secretariapay.api.entity.enums.academic.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "academic_requests")
public class AcademicRequest {
    @Id @GeneratedValue(strategy = GenerationType.UUID) public UUID id;
    @Column(name="request_code", nullable=false, unique=true, length=60) public String requestCode;
    @Column(name="institution_id") public UUID institutionId;
    @Column(name="student_id", nullable=false) public UUID studentId;
    @Column(name="student_name", nullable=false, length=180) public String studentName;
    @Column(name="student_number", length=80) public String studentNumber;
    @Enumerated(EnumType.STRING) @Column(name="request_type", nullable=false, length=60) public AcademicRequestType requestType;
    @Column(nullable=false, length=180) public String subject;
    @Column(columnDefinition="text") public String description;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=40) public AcademicRequestStatus status = AcademicRequestStatus.REQUESTED;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=30) public AcademicRequestPriority priority = AcademicRequestPriority.NORMAL;
    @Column(name="requester_phone", length=40) public String requesterPhone;
    @Column(name="assigned_to", length=120) public String assignedTo;
    @Column(name="review_note", columnDefinition="text") public String reviewNote;
    @Column(name="completed_note", columnDefinition="text") public String completedNote;
    @Column(name="requested_at", nullable=false) public LocalDateTime requestedAt;
    @Column(name="reviewed_at") public LocalDateTime reviewedAt;
    @Column(name="completed_at") public LocalDateTime completedAt;
    @Column(name="created_at", nullable=false) public LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false) public LocalDateTime updatedAt;
    @PrePersist public void prePersist(){ LocalDateTime now=LocalDateTime.now(); if(requestCode==null||requestCode.isBlank()) requestCode="ACD-"+System.currentTimeMillis(); if(status==null) status=AcademicRequestStatus.REQUESTED; if(priority==null) priority=AcademicRequestPriority.NORMAL; if(requestedAt==null) requestedAt=now; createdAt=now; updatedAt=now; }
    @PreUpdate public void preUpdate(){ updatedAt=LocalDateTime.now(); }
}
