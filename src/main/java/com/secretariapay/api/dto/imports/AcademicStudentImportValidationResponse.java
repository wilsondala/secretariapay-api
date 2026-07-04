package com.secretariapay.api.dto.imports;

import java.util.UUID;

public class AcademicStudentImportValidationResponse {

    private UUID batchId;
    private String importCode;
    private Integer totalRows;
    private Integer validRows;
    private Integer invalidRows;
    private Integer duplicateRows;
    private String status;
    private String message;

    public UUID getBatchId() { return batchId; }
    public AcademicStudentImportValidationResponse setBatchId(UUID batchId) { this.batchId = batchId; return this; }
    public String getImportCode() { return importCode; }
    public AcademicStudentImportValidationResponse setImportCode(String importCode) { this.importCode = importCode; return this; }
    public Integer getTotalRows() { return totalRows; }
    public AcademicStudentImportValidationResponse setTotalRows(Integer totalRows) { this.totalRows = totalRows; return this; }
    public Integer getValidRows() { return validRows; }
    public AcademicStudentImportValidationResponse setValidRows(Integer validRows) { this.validRows = validRows; return this; }
    public Integer getInvalidRows() { return invalidRows; }
    public AcademicStudentImportValidationResponse setInvalidRows(Integer invalidRows) { this.invalidRows = invalidRows; return this; }
    public Integer getDuplicateRows() { return duplicateRows; }
    public AcademicStudentImportValidationResponse setDuplicateRows(Integer duplicateRows) { this.duplicateRows = duplicateRows; return this; }
    public String getStatus() { return status; }
    public AcademicStudentImportValidationResponse setStatus(String status) { this.status = status; return this; }
    public String getMessage() { return message; }
    public AcademicStudentImportValidationResponse setMessage(String message) { this.message = message; return this; }
}
