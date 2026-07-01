package com.secretariapay.api.entity.academic;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(length = 80)
    private String code;

    @Column(length = 120)
    private String faculty;

    @Column(name = "duration_years")
    private Integer durationYears;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (active == null) {
            active = true;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();

        if (active == null) {
            active = true;
        }
    }

    public UUID getId() {
        return id;
    }

    public Institution getInstitution() {
        return institution;
    }

    public Course setInstitution(Institution institution) {
        this.institution = institution;
        return this;
    }

    public String getName() {
        return name;
    }

    public Course setName(String name) {
        this.name = name;
        return this;
    }

    public String getCode() {
        return code;
    }

    public Course setCode(String code) {
        this.code = code;
        return this;
    }

    public String getFaculty() {
        return faculty;
    }

    public Course setFaculty(String faculty) {
        this.faculty = faculty;
        return this;
    }

    public Integer getDurationYears() {
        return durationYears;
    }

    public Course setDurationYears(Integer durationYears) {
        this.durationYears = durationYears;
        return this;
    }

    public Boolean getActive() {
        return active;
    }

    public Course setActive(Boolean active) {
        this.active = active;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
