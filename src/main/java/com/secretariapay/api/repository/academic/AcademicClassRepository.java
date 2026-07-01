package com.secretariapay.api.repository.academic;

import com.secretariapay.api.entity.academic.AcademicClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AcademicClassRepository extends JpaRepository<AcademicClass, UUID> {

    List<AcademicClass> findByCourseIdOrderByNameAsc(UUID courseId);

    List<AcademicClass> findByCourseIdAndActiveTrueOrderByNameAsc(UUID courseId);

    List<AcademicClass> findByAcademicYearOrderByNameAsc(String academicYear);
}