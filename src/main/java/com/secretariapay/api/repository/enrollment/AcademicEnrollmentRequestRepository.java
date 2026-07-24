package com.secretariapay.api.repository.enrollment;

import com.secretariapay.api.entity.enrollment.AcademicEnrollmentRequest;
import com.secretariapay.api.entity.enums.enrollment.EnrollmentRequestStatus;
import com.secretariapay.api.entity.enums.enrollment.EnrollmentRequestType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcademicEnrollmentRequestRepository extends JpaRepository<AcademicEnrollmentRequest, UUID> {

    Optional<AcademicEnrollmentRequest> findByAdmissionApplicationId(UUID admissionApplicationId);

    boolean existsByAdmissionApplicationId(UUID admissionApplicationId);

    boolean existsByStudentIdAndAcademicYearAndRequestTypeAndStatusNot(
            UUID studentId,
            String academicYear,
            EnrollmentRequestType requestType,
            EnrollmentRequestStatus excludedStatus
    );

    boolean existsByRequestCode(String requestCode);

    List<AcademicEnrollmentRequest> findByInstitutionIdOrderByCreatedAtDesc(UUID institutionId);

    List<AcademicEnrollmentRequest> findByInstitutionIdAndRequestTypeOrderByCreatedAtDesc(
            UUID institutionId,
            EnrollmentRequestType requestType
    );
}
