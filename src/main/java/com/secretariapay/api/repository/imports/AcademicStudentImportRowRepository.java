package com.secretariapay.api.repository.imports;

import com.secretariapay.api.entity.enums.imports.AcademicStudentImportRowStatus;
import com.secretariapay.api.entity.imports.AcademicStudentImportRow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AcademicStudentImportRowRepository extends JpaRepository<AcademicStudentImportRow, UUID> {

    List<AcademicStudentImportRow> findByBatchIdOrderByRowNumberAscCreatedAtAsc(UUID batchId);

    List<AcademicStudentImportRow> findByBatchIdAndStatusInOrderByRowNumberAscCreatedAtAsc(
            UUID batchId,
            Collection<AcademicStudentImportRowStatus> statuses
    );

    long countByBatchId(UUID batchId);
}
