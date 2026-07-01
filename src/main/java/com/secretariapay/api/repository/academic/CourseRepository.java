package com.secretariapay.api.repository.academic;

import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.academic.Institution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    List<Course> findByInstitutionOrderByNameAsc(Institution institution);

    List<Course> findByInstitutionIdOrderByNameAsc(UUID institutionId);

    List<Course> findByInstitutionIdAndActiveTrueOrderByNameAsc(UUID institutionId);
}