package com.secretariapay.api.dto.academic;

import com.secretariapay.api.entity.enums.academic.StudentStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public class StudentRequest {

    @NotNull(message = "Turma é obrigatória.")
    private UUID academicClassId;

    @NotBlank(message = "Número de estudante é obrigatório.")
    @Size(max = 60)
    private String studentNumber;

    @NotBlank(message = "Nome completo é obrigatório.")
    @Size(max = 180)
    private String fullName;

    @Size(max = 30)
    private String documentType;

    @Size(max = 60)
    private String documentNumber;

    @Email(message = "E-mail inválido.")
    @Size(max = 180)
    private String email;

    @Size(max = 40)
    private String phone;

    @Size(max = 40)
    private String whatsapp;

    private LocalDate birthDate;

    @Size(max = 180)
    private String guardianName;

    @Size(max = 40)
    private String guardianPhone;

    @Email(message = "E-mail do responsável inválido.")
    @Size(max = 180)
    private String guardianEmail;

    private StudentStatus status;
    private Boolean financiallyBlocked;
    private String blockedReason;

    public UUID getAcademicClassId() { return academicClassId; }
    public StudentRequest setAcademicClassId(UUID academicClassId) { this.academicClassId = academicClassId; return this; }
    public String getStudentNumber() { return studentNumber; }
    public StudentRequest setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; return this; }
    public String getFullName() { return fullName; }
    public StudentRequest setFullName(String fullName) { this.fullName = fullName; return this; }
    public String getDocumentType() { return documentType; }
    public StudentRequest setDocumentType(String documentType) { this.documentType = documentType; return this; }
    public String getDocumentNumber() { return documentNumber; }
    public StudentRequest setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; return this; }
    public String getEmail() { return email; }
    public StudentRequest setEmail(String email) { this.email = email; return this; }
    public String getPhone() { return phone; }
    public StudentRequest setPhone(String phone) { this.phone = phone; return this; }
    public String getWhatsapp() { return whatsapp; }
    public StudentRequest setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; return this; }
    public LocalDate getBirthDate() { return birthDate; }
    public StudentRequest setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; return this; }
    public String getGuardianName() { return guardianName; }
    public StudentRequest setGuardianName(String guardianName) { this.guardianName = guardianName; return this; }
    public String getGuardianPhone() { return guardianPhone; }
    public StudentRequest setGuardianPhone(String guardianPhone) { this.guardianPhone = guardianPhone; return this; }
    public String getGuardianEmail() { return guardianEmail; }
    public StudentRequest setGuardianEmail(String guardianEmail) { this.guardianEmail = guardianEmail; return this; }
    public StudentStatus getStatus() { return status; }
    public StudentRequest setStatus(StudentStatus status) { this.status = status; return this; }
    public Boolean getFinanciallyBlocked() { return financiallyBlocked; }
    public StudentRequest setFinanciallyBlocked(Boolean financiallyBlocked) { this.financiallyBlocked = financiallyBlocked; return this; }
    public String getBlockedReason() { return blockedReason; }
    public StudentRequest setBlockedReason(String blockedReason) { this.blockedReason = blockedReason; return this; }
}
