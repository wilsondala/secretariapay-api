package com.secretariapay.api.entity.admission;

import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.enums.admission.AdmissionShift;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "admission_course_offerings")
public class AdmissionCourseOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private AdmissionCampaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "department_code", nullable = false, length = 20)
    private String departmentCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdmissionShift shift;

    @Column(name = "decree_reference", length = 220)
    private String decreeReference;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (active == null) active = true;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        if (active == null) active = true;
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public AdmissionCampaign getCampaign() { return campaign; }
    public AdmissionCourseOffering setCampaign(AdmissionCampaign campaign) { this.campaign = campaign; return this; }
    public Course getCourse() { return course; }
    public AdmissionCourseOffering setCourse(Course course) { this.course = course; return this; }
    public String getDepartmentCode() { return departmentCode; }
    public AdmissionCourseOffering setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; return this; }
    public AdmissionShift getShift() { return shift; }
    public AdmissionCourseOffering setShift(AdmissionShift shift) { this.shift = shift; return this; }
    public String getDecreeReference() { return decreeReference; }
    public AdmissionCourseOffering setDecreeReference(String decreeReference) { this.decreeReference = decreeReference; return this; }
    public Boolean getActive() { return active; }
    public AdmissionCourseOffering setActive(Boolean active) { this.active = active; return this; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
