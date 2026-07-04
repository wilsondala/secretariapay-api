package com.secretariapay.api.repository.restriction;
import com.secretariapay.api.entity.restriction.AcademicRestriction;
import com.secretariapay.api.entity.enums.restriction.AcademicRestrictionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AcademicRestrictionRepository extends JpaRepository<AcademicRestriction, UUID> {
    List<AcademicRestriction> findByStudentIdOrderByCreatedAtDesc(UUID studentId);
    List<AcademicRestriction> findByStudentIdAndStatusOrderByCreatedAtDesc(UUID studentId, AcademicRestrictionStatus status);
    boolean existsByRestrictionCode(String restrictionCode);
}
