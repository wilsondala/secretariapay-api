package com.secretariapay.api.dto.academic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class CourseRequest {

    @NotNull(message = "Instituição é obrigatória.")
    private UUID institutionId;

    @NotBlank(message = "Nome do curso é obrigatório.")
    @Size(max = 180)
    private String name;

    @Size(max = 80)
    private String code;

    @Size(max = 120)
    private String faculty;

    private Integer durationYears;
    private Boolean active;

    public UUID getInstitutionId() { return institutionId; }
    public CourseRequest setInstitutionId(UUID institutionId) { this.institutionId = institutionId; return this; }
    public String getName() { return name; }
    public CourseRequest setName(String name) { this.name = name; return this; }
    public String getCode() { return code; }
    public CourseRequest setCode(String code) { this.code = code; return this; }
    public String getFaculty() { return faculty; }
    public CourseRequest setFaculty(String faculty) { this.faculty = faculty; return this; }
    public Integer getDurationYears() { return durationYears; }
    public CourseRequest setDurationYears(Integer durationYears) { this.durationYears = durationYears; return this; }
    public Boolean getActive() { return active; }
    public CourseRequest setActive(Boolean active) { this.active = active; return this; }
}
