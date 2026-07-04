package com.secretariapay.api.dto.imports;

import java.util.UUID;

public class AcademicStudentImportSyncResponse {

    private UUID batchId;
    private String importCode;
    private Integer totalRows;
    private Integer processedRows;
    private Integer syncedRows;
    private Integer skippedRows;
    private Integer createdCourses;
    private Integer reusedCourses;
    private Integer createdClasses;
    private Integer reusedClasses;
    private Integer createdStudents;
    private Integer updatedStudents;
    private String status;
    private String message;

    public UUID getBatchId() { return batchId; }
    public AcademicStudentImportSyncResponse setBatchId(UUID batchId) { this.batchId = batchId; return this; }

    public String getImportCode() { return importCode; }
    public AcademicStudentImportSyncResponse setImportCode(String importCode) { this.importCode = importCode; return this; }

    public Integer getTotalRows() { return totalRows; }
    public AcademicStudentImportSyncResponse setTotalRows(Integer totalRows) { this.totalRows = totalRows; return this; }

    public Integer getProcessedRows() { return processedRows; }
    public AcademicStudentImportSyncResponse setProcessedRows(Integer processedRows) { this.processedRows = processedRows; return this; }

    public Integer getSyncedRows() { return syncedRows; }
    public AcademicStudentImportSyncResponse setSyncedRows(Integer syncedRows) { this.syncedRows = syncedRows; return this; }

    public Integer getSkippedRows() { return skippedRows; }
    public AcademicStudentImportSyncResponse setSkippedRows(Integer skippedRows) { this.skippedRows = skippedRows; return this; }

    public Integer getCreatedCourses() { return createdCourses; }
    public AcademicStudentImportSyncResponse setCreatedCourses(Integer createdCourses) { this.createdCourses = createdCourses; return this; }

    public Integer getReusedCourses() { return reusedCourses; }
    public AcademicStudentImportSyncResponse setReusedCourses(Integer reusedCourses) { this.reusedCourses = reusedCourses; return this; }

    public Integer getCreatedClasses() { return createdClasses; }
    public AcademicStudentImportSyncResponse setCreatedClasses(Integer createdClasses) { this.createdClasses = createdClasses; return this; }

    public Integer getReusedClasses() { return reusedClasses; }
    public AcademicStudentImportSyncResponse setReusedClasses(Integer reusedClasses) { this.reusedClasses = reusedClasses; return this; }

    public Integer getCreatedStudents() { return createdStudents; }
    public AcademicStudentImportSyncResponse setCreatedStudents(Integer createdStudents) { this.createdStudents = createdStudents; return this; }

    public Integer getUpdatedStudents() { return updatedStudents; }
    public AcademicStudentImportSyncResponse setUpdatedStudents(Integer updatedStudents) { this.updatedStudents = updatedStudents; return this; }

    public String getStatus() { return status; }
    public AcademicStudentImportSyncResponse setStatus(String status) { this.status = status; return this; }

    public String getMessage() { return message; }
    public AcademicStudentImportSyncResponse setMessage(String message) { this.message = message; return this; }
}
