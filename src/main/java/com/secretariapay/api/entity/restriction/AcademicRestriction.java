package com.secretariapay.api.entity.restriction;

import com.secretariapay.api.entity.enums.restriction.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "academic_restrictions")
public class AcademicRestriction {
    @Id @GeneratedValue(strategy = GenerationType.UUID) public UUID id;
    @Column(name="restriction_code", nullable=false, unique=true, length=60) public String restrictionCode;
    @Column(name="institution_id") public UUID institutionId;
    @Column(name="student_id", nullable=false) public UUID studentId;
    @Column(name="student_name", nullable=false, length=180) public String studentName;
    @Column(name="student_number", length=80) public String studentNumber;
    @Enumerated(EnumType.STRING) @Column(name="restriction_type", nullable=false, length=60) public AcademicRestrictionType restrictionType;
    @Column(nullable=false, length=180) public String reason;
    @Column(columnDefinition="text") public String description;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=40) public AcademicRestrictionStatus status = AcademicRestrictionStatus.ACTIVE;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=60) public AcademicRestrictionSource source = AcademicRestrictionSource.MANUAL;
    @Column(name="related_charge_id") public UUID relatedChargeId;
    @Column(name="related_charge_code", length=80) public String relatedChargeCode;
    @Column(name="applied_by", length=120) public String appliedBy;
    @Column(name="released_by", length=120) public String releasedBy;
    @Column(name="release_note", columnDefinition="text") public String releaseNote;
    @Column(name="applied_at", nullable=false) public LocalDateTime appliedAt;
    @Column(name="released_at") public LocalDateTime releasedAt;
    @Column(name="created_at", nullable=false) public LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false) public LocalDateTime updatedAt;
    @PrePersist public void prePersist(){ LocalDateTime now=LocalDateTime.now(); if(restrictionCode==null||restrictionCode.isBlank()) restrictionCode="RST-"+System.currentTimeMillis(); if(status==null) status=AcademicRestrictionStatus.ACTIVE; if(source==null) source=AcademicRestrictionSource.MANUAL; if(appliedAt==null) appliedAt=now; createdAt=now; updatedAt=now; }
    @PreUpdate public void preUpdate(){ updatedAt=LocalDateTime.now(); }
}
