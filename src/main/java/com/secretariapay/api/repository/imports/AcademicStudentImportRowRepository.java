package com.secretariapay.api.repository.imports;

import com.secretariapay.api.entity.imports.AcademicStudentImportRow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AcademicStudentImportRowRepository extends JpaRepository<AcademicStudentImportRow, UUID> {

    List<AcademicStudentImportRow> findByBatchIdOrderByRowNumberAscCreatedAtAsc(UUID batchId);

    long countByBatchId(UUID batchId);
}
