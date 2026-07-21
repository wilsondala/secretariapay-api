package com.secretariapay.api.repository.academic;

import com.secretariapay.api.entity.academic.AcademicServiceOrder;
import com.secretariapay.api.entity.enums.academic.AcademicServiceOrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcademicServiceOrderRepository extends JpaRepository<AcademicServiceOrder, UUID> {

    boolean existsByOrderCode(String orderCode);

    @EntityGraph(attributePaths = {
            "student",
            "student.academicClass",
            "student.academicClass.course",
            "student.academicClass.course.institution",
            "service",
            "charge",
            "documentRequest"
    })
    List<AcademicServiceOrder> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"student", "service", "charge", "documentRequest"})
    Optional<AcademicServiceOrder> findOneById(UUID id);

    @EntityGraph(attributePaths = {"student", "service", "charge", "documentRequest"})
    Optional<AcademicServiceOrder> findByChargeId(UUID chargeId);

    @EntityGraph(attributePaths = {"student", "service", "charge", "documentRequest"})
    List<AcademicServiceOrder> findByStatusOrderByCreatedAtAsc(AcademicServiceOrderStatus status);

    @EntityGraph(attributePaths = {"student", "service", "charge", "documentRequest"})
    List<AcademicServiceOrder> findByStudentIdOrderByCreatedAtDesc(UUID studentId);
}