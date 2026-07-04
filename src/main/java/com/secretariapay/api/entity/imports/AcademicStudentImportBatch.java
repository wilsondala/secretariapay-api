package com.secretariapay.api.entity.imports;

import com.secretariapay.api.entity.enums.imports.AcademicStudentImportStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "academic_student_import_batches")
public class AcademicStudentImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "import_code", nullable = false, unique = true, length = 80)
    private String importCode;

    @Column(name = "source_system", nullable = false, length = 80)
    private String sourceSystem = "WEBSCHOOL_ADMINUT";

    @Column(name = "source_name", length = 180)
    private String sourceName;

    @Column(name = "file_name", length = 180)
    private String fileName;

    @Column(name = "academic_year", length = 20)
    private String academicYear;

    @Column(length = 20)
    private String semester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AcademicStudentImportStatus status = AcademicStudentImportStatus.DRAFT;

    @Column(name = "total_rows", nullable = false)
    private Integer totalRows = 0;

    @Column(name = "valid_rows", nullable = false)
    private Integer validRows = 0;

    @Column(name = "invalid_rows", nullable = false)
    private Integer invalidRows = 0;

    @Column(name = "imported_rows", nullable = false)
    private Integer importedRows = 0;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_by", length = 120)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (importCode == null || importCode.isBlank()) {
            importCode = "WSI-" + System.currentTimeMillis();
        }

        if (sourceSystem == null || sourceSystem.isBlank()) {
            sourceSystem = "WEBSCHOOL_ADMINUT";
        }

        if (status == null) {
            status = AcademicStudentImportStatus.DRAFT;
        }

        if (totalRows == null) totalRows = 0;
        if (validRows == null) validRows = 0;
        if (invalidRows == null) invalidRows = 0;
        if (importedRows == null) importedRows = 0;

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getInstitutionId() { return institutionId; }
    public AcademicStudentImportBatch setInstitutionId(UUID institutionId) { this.institutionId = institutionId; return this; }
    public String getImportCode() { return importCode; }
    public AcademicStudentImportBatch setImportCode(String importCode) { this.importCode = importCode; return this; }
    public String getSourceSystem() { return sourceSystem; }
    public AcademicStudentImportBatch setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; return this; }
    public String getSourceName() { return sourceName; }
    public AcademicStudentImportBatch setSourceName(String sourceName) { this.sourceName = sourceName; return this; }
    public String getFileName() { return fileName; }
    public AcademicStudentImportBatch setFileName(String fileName) { this.fileName = fileName; return this; }
    public String getAcademicYear() { return academicYear; }
    public AcademicStudentImportBatch setAcademicYear(String academicYear) { this.academicYear = academicYear; return this; }
    public String getSemester() { return semester; }
    public AcademicStudentImportBatch setSemester(String semester) { this.semester = semester; return this; }
    public AcademicStudentImportStatus getStatus() { return status; }
    public AcademicStudentImportBatch setStatus(AcademicStudentImportStatus status) { this.status = status; return this; }
    public Integer getTotalRows() { return totalRows; }
    public AcademicStudentImportBatch setTotalRows(Integer totalRows) { this.totalRows = totalRows; return this; }
    public Integer getValidRows() { return validRows; }
    public AcademicStudentImportBatch setValidRows(Integer validRows) { this.validRows = validRows; return this; }
    public Integer getInvalidRows() { return invalidRows; }
    public AcademicStudentImportBatch setInvalidRows(Integer invalidRows) { this.invalidRows = invalidRows; return this; }
    public Integer getImportedRows() { return importedRows; }
    public AcademicStudentImportBatch setImportedRows(Integer importedRows) { this.importedRows = importedRows; return this; }
    public String getNotes() { return notes; }
    public AcademicStudentImportBatch setNotes(String notes) { this.notes = notes; return this; }
    public String getCreatedBy() { return createdBy; }
    public AcademicStudentImportBatch setCreatedBy(String createdBy) { this.createdBy = createdBy; return this; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getValidatedAt() { return validatedAt; }
    public AcademicStudentImportBatch setValidatedAt(LocalDateTime validatedAt) { this.validatedAt = validatedAt; return this; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public AcademicStudentImportBatch setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; return this; }
}
