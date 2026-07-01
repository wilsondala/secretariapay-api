package com.secretariapay.api.dto.academic;

import java.time.LocalDateTime;
import java.util.UUID;

public class CourseResponse {
    private UUID id;
    private UUID institutionId;
    private String institutionName;
    private String name;
    private String code;
    private String faculty;
    private Integer durationYears;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public CourseResponse setId(UUID id) { this.id = id; return this; }
    public UUID getInstitutionId() { return institutionId; }
    public CourseResponse setInstitutionId(UUID institutionId) { this.institutionId = institutionId; return this; }
    public String getInstitutionName() { return institutionName; }
    public CourseResponse setInstitutionName(String institutionName) { this.institutionName = institutionName; return this; }
    public String getName() { return name; }
    public CourseResponse setName(String name) { this.name = name; return this; }
    public String getCode() { return code; }
    public CourseResponse setCode(String code) { this.code = code; return this; }
    public String getFaculty() { return faculty; }
    public CourseResponse setFaculty(String faculty) { this.faculty = faculty; return this; }
    public Integer getDurationYears() { return durationYears; }
    public CourseResponse setDurationYears(Integer durationYears) { this.durationYears = durationYears; return this; }
    public Boolean getActive() { return active; }
    public CourseResponse setActive(Boolean active) { this.active = active; return this; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public CourseResponse setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public CourseResponse setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
}
