package com.secretariapay.api.dto.academic;

import com.secretariapay.api.entity.enums.academic.AcademicShift;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class AcademicClassRequest {

    @NotNull(message = "Curso é obrigatório.")
    private UUID courseId;

    @NotBlank(message = "Nome da turma é obrigatório.")
    @Size(max = 120)
    private String name;

    @NotBlank(message = "Ano académico é obrigatório.")
    @Size(max = 20)
    private String academicYear;

    private Integer yearLevel;
    private AcademicShift shift;
    private Boolean active;

    public UUID getCourseId() { return courseId; }
    public AcademicClassRequest setCourseId(UUID courseId) { this.courseId = courseId; return this; }
    public String getName() { return name; }
    public AcademicClassRequest setName(String name) { this.name = name; return this; }
    public String getAcademicYear() { return academicYear; }
    public AcademicClassRequest setAcademicYear(String academicYear) { this.academicYear = academicYear; return this; }
    public Integer getYearLevel() { return yearLevel; }
    public AcademicClassRequest setYearLevel(Integer yearLevel) { this.yearLevel = yearLevel; return this; }
    public AcademicShift getShift() { return shift; }
    public AcademicClassRequest setShift(AcademicShift shift) { this.shift = shift; return this; }
    public Boolean getActive() { return active; }
    public AcademicClassRequest setActive(Boolean active) { this.active = active; return this; }
}
