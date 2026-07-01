package com.secretariapay.api.dto.academic;

import com.secretariapay.api.entity.enums.academic.AcademicShift;

import java.time.LocalDateTime;
import java.util.UUID;

public class AcademicClassResponse {
    private UUID id;
    private UUID courseId;
    private String courseName;
    private String name;
    private String academicYear;
    private Integer yearLevel;
    private AcademicShift shift;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public AcademicClassResponse setId(UUID id) { this.id = id; return this; }
    public UUID getCourseId() { return courseId; }
    public AcademicClassResponse setCourseId(UUID courseId) { this.courseId = courseId; return this; }
    public String getCourseName() { return courseName; }
    public AcademicClassResponse setCourseName(String courseName) { this.courseName = courseName; return this; }
    public String getName() { return name; }
    public AcademicClassResponse setName(String name) { this.name = name; return this; }
    public String getAcademicYear() { return academicYear; }
    public AcademicClassResponse setAcademicYear(String academicYear) { this.academicYear = academicYear; return this; }
    public Integer getYearLevel() { return yearLevel; }
    public AcademicClassResponse setYearLevel(Integer yearLevel) { this.yearLevel = yearLevel; return this; }
    public AcademicShift getShift() { return shift; }
    public AcademicClassResponse setShift(AcademicShift shift) { this.shift = shift; return this; }
    public Boolean getActive() { return active; }
    public AcademicClassResponse setActive(Boolean active) { this.active = active; return this; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public AcademicClassResponse setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public AcademicClassResponse setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
}
