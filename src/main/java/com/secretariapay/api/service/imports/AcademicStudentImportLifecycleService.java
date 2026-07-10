package com.secretariapay.api.service.imports;

import com.secretariapay.api.entity.enums.imports.AcademicStudentImportRowStatus;
import com.secretariapay.api.entity.enums.imports.AcademicStudentImportStatus;
import com.secretariapay.api.entity.imports.AcademicStudentImportBatch;
import com.secretariapay.api.entity.imports.AcademicStudentImportRow;
import com.secretariapay.api.repository.imports.AcademicStudentImportBatchRepository;
import com.secretariapay.api.repository.imports.AcademicStudentImportRowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AcademicStudentImportLifecycleService {

    private final AcademicStudentImportService importService;
    private final AcademicStudentImportBatchRepository batchRepository;
    private final AcademicStudentImportRowRepository rowRepository;

    public AcademicStudentImportLifecycleService(
            AcademicStudentImportService importService,
            AcademicStudentImportBatchRepository batchRepository,
            AcademicStudentImportRowRepository rowRepository
    ) {
        this.importService = importService;
        this.batchRepository = batchRepository;
        this.rowRepository = rowRepository;
    }

    @Transactional
    public AcademicStudentImportBatch cancel(UUID batchId, String reason, String operator) {
        AcademicStudentImportBatch batch = importService.findBatch(batchId);
        List<AcademicStudentImportRow> rows = rowRepository.findByBatchIdOrderByRowNumberAscCreatedAtAsc(batchId);

        boolean hasSyncedRows = rows.stream().anyMatch(row -> row.getStatus() == AcademicStudentImportRowStatus.SYNCED);
        if (hasSyncedRows) {
            throw new IllegalStateException("Não é possível cancelar um lote que já possui estudantes sincronizados.");
        }

        if (batch.getStatus() == AcademicStudentImportStatus.CANCELLED) {
            return batch;
        }

        String detail = reason == null || reason.isBlank() ? "Cancelado manualmente." : reason.trim();
        String actor = operator == null || operator.isBlank() ? "operador não identificado" : operator.trim();
        batch.setStatus(AcademicStudentImportStatus.CANCELLED)
                .setNotes(appendNote(batch.getNotes(), "Cancelado por " + actor + " em " + LocalDateTime.now() + ". Motivo: " + detail));

        return batchRepository.save(batch);
    }

    @Transactional
    public Map<String, Object> reprocess(UUID batchId, String operator) {
        AcademicStudentImportBatch batch = importService.findBatch(batchId);
        if (batch.getStatus() == AcademicStudentImportStatus.CANCELLED) {
            throw new IllegalStateException("Lotes cancelados não podem ser reprocessados.");
        }

        List<AcademicStudentImportRow> rows = rowRepository.findByBatchIdOrderByRowNumberAscCreatedAtAsc(batchId);
        int resetRows = 0;
        int preservedSyncedRows = 0;

        for (AcademicStudentImportRow row : rows) {
            if (row.getStatus() == AcademicStudentImportRowStatus.SYNCED) {
                preservedSyncedRows++;
                continue;
            }
            row.setStatus(AcademicStudentImportRowStatus.PENDING)
                    .setValidationMessage("Linha devolvida para reprocessamento controlado.");
            rowRepository.save(row);
            resetRows++;
        }

        String actor = operator == null || operator.isBlank() ? "operador não identificado" : operator.trim();
        batch.setStatus(AcademicStudentImportStatus.DRAFT)
                .setValidatedAt(null)
                .setCompletedAt(null)
                .setNotes(appendNote(batch.getNotes(), "Reprocessamento solicitado por " + actor + " em " + LocalDateTime.now() + "."));
        batchRepository.save(batch);

        var validation = importService.validateBatch(batchId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("batchId", batchId);
        response.put("importCode", batch.getImportCode());
        response.put("resetRows", resetRows);
        response.put("preservedSyncedRows", preservedSyncedRows);
        response.put("totalRows", validation.getTotalRows());
        response.put("validRows", validation.getValidRows());
        response.put("invalidRows", validation.getInvalidRows());
        response.put("duplicateRows", validation.getDuplicateRows());
        response.put("status", validation.getStatus());
        response.put("message", "Reprocessamento concluído. Linhas já sincronizadas foram preservadas e as demais revalidadas.");
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> history(UUID batchId) {
        AcademicStudentImportBatch batch = importService.findBatch(batchId);
        List<AcademicStudentImportRow> rows = rowRepository.findByBatchIdOrderByRowNumberAscCreatedAtAsc(batchId);

        long pending = rows.stream().filter(row -> row.getStatus() == AcademicStudentImportRowStatus.PENDING).count();
        long valid = rows.stream().filter(row -> row.getStatus() == AcademicStudentImportRowStatus.VALID).count();
        long invalid = rows.stream().filter(row -> row.getStatus() == AcademicStudentImportRowStatus.INVALID).count();
        long duplicate = rows.stream().filter(row -> row.getStatus() == AcademicStudentImportRowStatus.DUPLICATE).count();
        long imported = rows.stream().filter(row -> row.getStatus() == AcademicStudentImportRowStatus.IMPORTED).count();
        long synced = rows.stream().filter(row -> row.getStatus() == AcademicStudentImportRowStatus.SYNCED).count();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("batchId", batch.getId());
        response.put("importCode", batch.getImportCode());
        response.put("status", batch.getStatus().name());
        response.put("fileName", batch.getFileName());
        response.put("sourceName", batch.getSourceName());
        response.put("academicYear", batch.getAcademicYear());
        response.put("semester", batch.getSemester());
        response.put("createdBy", batch.getCreatedBy());
        response.put("createdAt", batch.getCreatedAt());
        response.put("updatedAt", batch.getUpdatedAt());
        response.put("validatedAt", batch.getValidatedAt());
        response.put("completedAt", batch.getCompletedAt());
        response.put("notes", batch.getNotes());
        response.put("totalRows", rows.size());
        response.put("pendingRows", pending);
        response.put("validRows", valid);
        response.put("invalidRows", invalid);
        response.put("duplicateRows", duplicate);
        response.put("importedRows", imported);
        response.put("syncedRows", synced);
        response.put("canCancel", synced == 0 && batch.getStatus() != AcademicStudentImportStatus.CANCELLED);
        response.put("canReprocess", batch.getStatus() != AcademicStudentImportStatus.CANCELLED);
        return response;
    }

    private String appendNote(String current, String addition) {
        if (current == null || current.isBlank()) return addition;
        return current.trim() + "\n" + addition;
    }
}
