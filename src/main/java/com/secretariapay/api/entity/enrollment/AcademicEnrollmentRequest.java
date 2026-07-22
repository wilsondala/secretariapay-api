package com.secretariapay.api.entity.enrollment;

import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.academic.Institution;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.admission.AdmissionApplication;
import com.secretariapay.api.entity.admission.AdmissionCampaign;
import com.secretariapay.api.entity.enums.enrollment.EnrollmentRequestStatus;
import com.secretariapay.api.entity.enums.enrollment.EnrollmentRequestType;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "academic_enrollment_requests")
public class AcademicEnrollmentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "request_code", nullable = false, unique = true, length = 80)
    private String requestCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private AdmissionCampaign campaign;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 30)
    private EnrollmentRequestType requestType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admission_application_id")
    private AdmissionApplication admissionApplication;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_course_id", nullable = false)
    private Course targetCourse;

    @Column(name = "target_shift", nullable = false, length = 20)
    private String targetShift;

    @Column(name = "academic_year", nullable = false, length = 20)
    private String academicYear;

    @Column(name = "target_year_level", nullable = false)
    private Integer targetYearLevel = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EnrollmentRequestStatus status = EnrollmentRequestStatus.AWAITING_PAYMENT;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (status == null) status = EnrollmentRequestStatus.AWAITING_PAYMENT;
        if (targetYearLevel == null) targetYearLevel = 1;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        if (status == null) status = EnrollmentRequestStatus.AWAITING_PAYMENT;
        if (targetYearLevel == null) targetYearLevel = 1;
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public String getRequestCode() { return requestCode; }
    public AcademicEnrollmentRequest setRequestCode(String requestCode) { this.requestCode = requestCode; return this; }
    public Institution getInstitution() { return institution; }
    public AcademicEnrollmentRequest setInstitution(Institution institution) { this.institution = institution; return this; }
    public AdmissionCampaign getCampaign() { return campaign; }
    public AcademicEnrollmentRequest setCampaign(AdmissionCampaign campaign) { this.campaign = campaign; return this; }
    public EnrollmentRequestType getRequestType() { return requestType; }
    public AcademicEnrollmentRequest setRequestType(EnrollmentRequestType requestType) { this.requestType = requestType; return this; }
    public AdmissionApplication getAdmissionApplication() { return admissionApplication; }
    public AcademicEnrollmentRequest setAdmissionApplication(AdmissionApplication admissionApplication) { this.admissionApplication = admissionApplication; return this; }
    public Student getStudent() { return student; }
    public AcademicEnrollmentRequest setStudent(Student student) { this.student = student; return this; }
    public Course getTargetCourse() { return targetCourse; }
    public AcademicEnrollmentRequest setTargetCourse(Course targetCourse) { this.targetCourse = targetCourse; return this; }
    public String getTargetShift() { return targetShift; }
    public AcademicEnrollmentRequest setTargetShift(String targetShift) { this.targetShift = targetShift; return this; }
    public String getAcademicYear() { return academicYear; }
    public AcademicEnrollmentRequest setAcademicYear(String academicYear) { this.academicYear = academicYear; return this; }
    public Integer getTargetYearLevel() { return targetYearLevel; }
    public AcademicEnrollmentRequest setTargetYearLevel(Integer targetYearLevel) { this.targetYearLevel = targetYearLevel; return this; }
    public EnrollmentRequestStatus getStatus() { return status; }
    public AcademicEnrollmentRequest setStatus(EnrollmentRequestStatus status) { this.status = status; return this; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public AcademicEnrollmentRequest setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; return this; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
