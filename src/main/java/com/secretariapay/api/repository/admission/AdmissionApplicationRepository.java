package com.secretariapay.api.repository.admission;

import com.secretariapay.api.entity.admission.AdmissionApplication;
import com.secretariapay.api.entity.enums.admission.AdmissionApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdmissionApplicationRepository extends JpaRepository<AdmissionApplication, UUID> {
    List<AdmissionApplication> findByInstitutionIdOrderByCreatedAtDesc(UUID institutionId);
    List<AdmissionApplication> findByInstitutionIdAndStatusOrderByCreatedAtDesc(UUID institutionId, AdmissionApplicationStatus status);
    List<AdmissionApplication> findByInstitutionIdAndDesiredCourseIdAndDesiredShiftIgnoreCaseOrderByCreatedAtDesc(UUID institutionId, UUID courseId, String desiredShift);
    Optional<AdmissionApplication> findByApplicationCodeIgnoreCase(String applicationCode);
    boolean existsByApplicationCode(String applicationCode);
    boolean existsByInstitutionIdAndAcademicYearAndDocumentNumberIgnoreCaseAndStatusNotIn(
            UUID institutionId,
            String academicYear,
            String documentNumber,
            List<AdmissionApplicationStatus> statuses
    );
}
