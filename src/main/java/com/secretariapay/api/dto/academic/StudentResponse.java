package com.secretariapay.api.dto.academic;

import com.secretariapay.api.entity.enums.academic.StudentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class StudentResponse {
    private UUID id;
    private UUID academicClassId;
    private String academicClassName;
    private String courseName;
    private String studentNumber;
    private String fullName;
    private String documentType;
    private String documentNumber;
    private String email;
    private String phone;
    private String whatsapp;
    private LocalDate birthDate;
    private String guardianName;
    private String guardianPhone;
    private String guardianEmail;
    private StudentStatus status;
    private Boolean financiallyBlocked;
    private String blockedReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public StudentResponse setId(UUID id) { this.id = id; return this; }
    public UUID getAcademicClassId() { return academicClassId; }
    public StudentResponse setAcademicClassId(UUID academicClassId) { this.academicClassId = academicClassId; return this; }
    public String getAcademicClassName() { return academicClassName; }
    public StudentResponse setAcademicClassName(String academicClassName) { this.academicClassName = academicClassName; return this; }
    public String getCourseName() { return courseName; }
    public StudentResponse setCourseName(String courseName) { this.courseName = courseName; return this; }
    public String getStudentNumber() { return studentNumber; }
    public StudentResponse setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; return this; }
    public String getFullName() { return fullName; }
    public StudentResponse setFullName(String fullName) { this.fullName = fullName; return this; }
    public String getDocumentType() { return documentType; }
    public StudentResponse setDocumentType(String documentType) { this.documentType = documentType; return this; }
    public String getDocumentNumber() { return documentNumber; }
    public StudentResponse setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; return this; }
    public String getEmail() { return email; }
    public StudentResponse setEmail(String email) { this.email = email; return this; }
    public String getPhone() { return phone; }
    public StudentResponse setPhone(String phone) { this.phone = phone; return this; }
    public String getWhatsapp() { return whatsapp; }
    public StudentResponse setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; return this; }
    public LocalDate getBirthDate() { return birthDate; }
    public StudentResponse setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; return this; }
    public String getGuardianName() { return guardianName; }
    public StudentResponse setGuardianName(String guardianName) { this.guardianName = guardianName; return this; }
    public String getGuardianPhone() { return guardianPhone; }
    public StudentResponse setGuardianPhone(String guardianPhone) { this.guardianPhone = guardianPhone; return this; }
    public String getGuardianEmail() { return guardianEmail; }
    public StudentResponse setGuardianEmail(String guardianEmail) { this.guardianEmail = guardianEmail; return this; }
    public StudentStatus getStatus() { return status; }
    public StudentResponse setStatus(StudentStatus status) { this.status = status; return this; }
    public Boolean getFinanciallyBlocked() { return financiallyBlocked; }
    public StudentResponse setFinanciallyBlocked(Boolean financiallyBlocked) { this.financiallyBlocked = financiallyBlocked; return this; }
    public String getBlockedReason() { return blockedReason; }
    public StudentResponse setBlockedReason(String blockedReason) { this.blockedReason = blockedReason; return this; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public StudentResponse setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public StudentResponse setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
}
