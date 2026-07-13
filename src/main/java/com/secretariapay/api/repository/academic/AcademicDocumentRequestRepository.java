package com.secretariapay.api.repository.academic;

import com.secretariapay.api.entity.academic.AcademicDocumentRequest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcademicDocumentRequestRepository extends JpaRepository<AcademicDocumentRequest, UUID> {

    @EntityGraph(attributePaths = {"student", "student.academicClass", "student.academicClass.course", "student.academicClass.course.institution"})
    List<AcademicDocumentRequest> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"student", "student.academicClass", "student.academicClass.course", "student.academicClass.course.institution"})
    Optional<AcademicDocumentRequest> findByDocumentCode(String documentCode);

    @EntityGraph(attributePaths = {"student", "student.academicClass", "student.academicClass.course", "student.academicClass.course.institution"})
    List<AcademicDocumentRequest> findByStudentIdOrderByCreatedAtDesc(UUID studentId);
}
