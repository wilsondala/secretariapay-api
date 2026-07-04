package com.secretariapay.api.repository.imports;

import com.secretariapay.api.entity.imports.AcademicStudentImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AcademicStudentImportBatchRepository extends JpaRepository<AcademicStudentImportBatch, UUID> {

    boolean existsByImportCode(String importCode);

    List<AcademicStudentImportBatch> findByInstitutionIdOrderByCreatedAtDesc(UUID institutionId);
}
