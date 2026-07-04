package com.secretariapay.api.entity.imports;

import com.secretariapay.api.entity.enums.imports.AcademicStudentImportRowStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "academic_student_import_rows")
public class AcademicStudentImportRow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "row_number")
    private Integer rowNumber;

    @Column(name = "academic_year", length = 20)
    private String academicYear;

    @Column(name = "semester_number")
    private Integer semesterNumber;

    @Column(name = "student_number", length = 80)
    private String studentNumber;

    @Column(name = "full_name", length = 220)
    private String fullName;

    @Column(name = "course_name", length = 220)
    private String courseName;

    @Column(name = "class_name", length = 80)
    private String className;

    @Column(name = "shift_name", length = 80)
    private String shiftName;

    @Column(name = "department_name", length = 160)
    private String departmentName;

    @Column(length = 160)
    private String email;

    @Column(length = 60)
    private String phone;

    @Column(length = 60)
    private String whatsapp;

    @Column(name = "responsible_name", length = 180)
    private String responsibleName;

    @Column(name = "responsible_phone", length = 60)
    private String responsiblePhone;

    @Column(name = "responsible_email", length = 160)
    private String responsibleEmail;

    @Column(name = "source_action", length = 80)
    private String sourceAction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AcademicStudentImportRowStatus status = AcademicStudentImportRowStatus.PENDING;

    @Column(name = "validation_message", columnDefinition = "text")
    private String validationMessage;

    @Column(name = "matched_student_id")
    private UUID matchedStudentId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (status == null) {
            status = AcademicStudentImportRowStatus.PENDING;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getBatchId() { return batchId; }
    public AcademicStudentImportRow setBatchId(UUID batchId) { this.batchId = batchId; return this; }
    public UUID getInstitutionId() { return institutionId; }
    public AcademicStudentImportRow setInstitutionId(UUID institutionId) { this.institutionId = institutionId; return this; }
    public Integer getRowNumber() { return rowNumber; }
    public AcademicStudentImportRow setRowNumber(Integer rowNumber) { this.rowNumber = rowNumber; return this; }
    public String getAcademicYear() { return academicYear; }
    public AcademicStudentImportRow setAcademicYear(String academicYear) { this.academicYear = academicYear; return this; }
    public Integer getSemesterNumber() { return semesterNumber; }
    public AcademicStudentImportRow setSemesterNumber(Integer semesterNumber) { this.semesterNumber = semesterNumber; return this; }
    public String getStudentNumber() { return studentNumber; }
    public AcademicStudentImportRow setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; return this; }
    public String getFullName() { return fullName; }
    public AcademicStudentImportRow setFullName(String fullName) { this.fullName = fullName; return this; }
    public String getCourseName() { return courseName; }
    public AcademicStudentImportRow setCourseName(String courseName) { this.courseName = courseName; return this; }
    public String getClassName() { return className; }
    public AcademicStudentImportRow setClassName(String className) { this.className = className; return this; }
    public String getShiftName() { return shiftName; }
    public AcademicStudentImportRow setShiftName(String shiftName) { this.shiftName = shiftName; return this; }
    public String getDepartmentName() { return departmentName; }
    public AcademicStudentImportRow setDepartmentName(String departmentName) { this.departmentName = departmentName; return this; }
    public String getEmail() { return email; }
    public AcademicStudentImportRow setEmail(String email) { this.email = email; return this; }
    public String getPhone() { return phone; }
    public AcademicStudentImportRow setPhone(String phone) { this.phone = phone; return this; }
    public String getWhatsapp() { return whatsapp; }
    public AcademicStudentImportRow setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; return this; }
    public String getResponsibleName() { return responsibleName; }
    public AcademicStudentImportRow setResponsibleName(String responsibleName) { this.responsibleName = responsibleName; return this; }
    public String getResponsiblePhone() { return responsiblePhone; }
    public AcademicStudentImportRow setResponsiblePhone(String responsiblePhone) { this.responsiblePhone = responsiblePhone; return this; }
    public String getResponsibleEmail() { return responsibleEmail; }
    public AcademicStudentImportRow setResponsibleEmail(String responsibleEmail) { this.responsibleEmail = responsibleEmail; return this; }
    public String getSourceAction() { return sourceAction; }
    public AcademicStudentImportRow setSourceAction(String sourceAction) { this.sourceAction = sourceAction; return this; }
    public AcademicStudentImportRowStatus getStatus() { return status; }
    public AcademicStudentImportRow setStatus(AcademicStudentImportRowStatus status) { this.status = status; return this; }
    public String getValidationMessage() { return validationMessage; }
    public AcademicStudentImportRow setValidationMessage(String validationMessage) { this.validationMessage = validationMessage; return this; }
    public UUID getMatchedStudentId() { return matchedStudentId; }
    public AcademicStudentImportRow setMatchedStudentId(UUID matchedStudentId) { this.matchedStudentId = matchedStudentId; return this; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
