package com.secretariapay.api.entity.financial;

import com.secretariapay.api.entity.User;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.financial.AcademicBlockStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "academic_blocks")
public class AcademicBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charge_id")
    private Charge charge;

    @Column(name = "blocked_service", nullable = false, length = 120)
    private String blockedService;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AcademicBlockStatus status = AcademicBlockStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_by_user_id")
    private User blockedBy;

    @Column(name = "blocked_at", nullable = false)
    private LocalDateTime blockedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "released_by_user_id")
    private User releasedBy;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "release_note", columnDefinition = "TEXT")
    private String releaseNote;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (blockedAt == null) {
            blockedAt = now;
        }

        if (status == null) {
            status = AcademicBlockStatus.ACTIVE;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();

        if (status == null) {
            status = AcademicBlockStatus.ACTIVE;
        }
    }

    public UUID getId() {
        return id;
    }

    public Student getStudent() {
        return student;
    }

    public AcademicBlock setStudent(Student student) {
        this.student = student;
        return this;
    }

    public Charge getCharge() {
        return charge;
    }

    public AcademicBlock setCharge(Charge charge) {
        this.charge = charge;
        return this;
    }

    public String getBlockedService() {
        return blockedService;
    }

    public AcademicBlock setBlockedService(String blockedService) {
        this.blockedService = blockedService;
        return this;
    }

    public String getReason() {
        return reason;
    }

    public AcademicBlock setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public AcademicBlockStatus getStatus() {
        return status;
    }

    public AcademicBlock setStatus(AcademicBlockStatus status) {
        this.status = status;
        return this;
    }

    public User getBlockedBy() {
        return blockedBy;
    }

    public AcademicBlock setBlockedBy(User blockedBy) {
        this.blockedBy = blockedBy;
        return this;
    }

    public LocalDateTime getBlockedAt() {
        return blockedAt;
    }

    public AcademicBlock setBlockedAt(LocalDateTime blockedAt) {
        this.blockedAt = blockedAt;
        return this;
    }

    public User getReleasedBy() {
        return releasedBy;
    }

    public AcademicBlock setReleasedBy(User releasedBy) {
        this.releasedBy = releasedBy;
        return this;
    }

    public LocalDateTime getReleasedAt() {
        return releasedAt;
    }

    public AcademicBlock setReleasedAt(LocalDateTime releasedAt) {
        this.releasedAt = releasedAt;
        return this;
    }

    public String getReleaseNote() {
        return releaseNote;
    }

    public AcademicBlock setReleaseNote(String releaseNote) {
        this.releaseNote = releaseNote;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}