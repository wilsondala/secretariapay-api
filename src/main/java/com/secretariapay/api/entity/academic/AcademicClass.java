package com.secretariapay.api.entity.academic;

import com.secretariapay.api.entity.enums.academic.AcademicShift;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "academic_classes")
public class AcademicClass {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "academic_year", nullable = false, length = 20)
    private String academicYear;

    @Column(name = "year_level")
    private Integer yearLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AcademicShift shift = AcademicShift.NIGHT;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (shift == null) {
            shift = AcademicShift.NIGHT;
        }

        if (active == null) {
            active = true;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();

        if (shift == null) {
            shift = AcademicShift.NIGHT;
        }

        if (active == null) {
            active = true;
        }
    }

    public UUID getId() {
        return id;
    }

    public Course getCourse() {
        return course;
    }

    public AcademicClass setCourse(Course course) {
        this.course = course;
        return this;
    }

    public String getName() {
        return name;
    }

    public AcademicClass setName(String name) {
        this.name = name;
        return this;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public AcademicClass setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
        return this;
    }

    public Integer getYearLevel() {
        return yearLevel;
    }

    public AcademicClass setYearLevel(Integer yearLevel) {
        this.yearLevel = yearLevel;
        return this;
    }

    public AcademicShift getShift() {
        return shift;
    }

    public AcademicClass setShift(AcademicShift shift) {
        this.shift = shift;
        return this;
    }

    public Boolean getActive() {
        return active;
    }

    public AcademicClass setActive(Boolean active) {
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
