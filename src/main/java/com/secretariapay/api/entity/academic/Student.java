package com.secretariapay.api.entity.academic;

import com.secretariapay.api.entity.enums.academic.StudentStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_class_id", nullable = false)
    private AcademicClass academicClass;

    @Column(name = "student_number", nullable = false, unique = true, length = 60)
    private String studentNumber;

    @Column(name = "full_name", nullable = false, length = 180)
    private String fullName;

    @Column(name = "document_type", length = 30)
    private String documentType;

    @Column(name = "document_number", length = 60)
    private String documentNumber;

    @Column(length = 180)
    private String email;

    @Column(length = 40)
    private String phone;

    @Column(length = 40)
    private String whatsapp;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "guardian_name", length = 180)
    private String guardianName;

    @Column(name = "guardian_phone", length = 40)
    private String guardianPhone;

    @Column(name = "guardian_email", length = 180)
    private String guardianEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StudentStatus status = StudentStatus.ACTIVE;

    @Column(name = "financially_blocked", nullable = false)
    private Boolean financiallyBlocked = false;

    @Column(name = "blocked_reason", columnDefinition = "TEXT")
    private String blockedReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (status == null) {
            status = StudentStatus.ACTIVE;
        }

        if (financiallyBlocked == null) {
            financiallyBlocked = false;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();

        if (status == null) {
            status = StudentStatus.ACTIVE;
        }

        if (financiallyBlocked == null) {
            financiallyBlocked = false;
        }
    }

    public UUID getId() {
        return id;
    }

    public AcademicClass getAcademicClass() {
        return academicClass;
    }

    public Student setAcademicClass(AcademicClass academicClass) {
        this.academicClass = academicClass;
        return this;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public Student setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
        return this;
    }

    public String getFullName() {
        return fullName;
    }

    public Student setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public String getDocumentType() {
        return documentType;
    }

    public Student setDocumentType(String documentType) {
        this.documentType = documentType;
        return this;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public Student setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public Student setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public Student setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public Student setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
        return this;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public Student setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
        return this;
    }

    public String getGuardianName() {
        return guardianName;
    }

    public Student setGuardianName(String guardianName) {
        this.guardianName = guardianName;
        return this;
    }

    public String getGuardianPhone() {
        return guardianPhone;
    }

    public Student setGuardianPhone(String guardianPhone) {
        this.guardianPhone = guardianPhone;
        return this;
    }

    public String getGuardianEmail() {
        return guardianEmail;
    }

    public Student setGuardianEmail(String guardianEmail) {
        this.guardianEmail = guardianEmail;
        return this;
    }

    public StudentStatus getStatus() {
        return status;
    }

    public Student setStatus(StudentStatus status) {
        this.status = status;
        return this;
    }

    public Boolean getFinanciallyBlocked() {
        return financiallyBlocked;
    }

    public Student setFinanciallyBlocked(Boolean financiallyBlocked) {
        this.financiallyBlocked = financiallyBlocked;
        return this;
    }

    public String getBlockedReason() {
        return blockedReason;
    }

    public Student setBlockedReason(String blockedReason) {
        this.blockedReason = blockedReason;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}