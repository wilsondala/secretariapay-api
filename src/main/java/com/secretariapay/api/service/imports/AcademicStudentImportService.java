package com.secretariapay.api.service.imports;

import com.secretariapay.api.dto.imports.AcademicStudentImportValidationResponse;
import com.secretariapay.api.entity.enums.imports.AcademicStudentImportRowStatus;
import com.secretariapay.api.entity.enums.imports.AcademicStudentImportStatus;
import com.secretariapay.api.entity.imports.AcademicStudentImportBatch;
import com.secretariapay.api.entity.imports.AcademicStudentImportRow;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.imports.AcademicStudentImportBatchRepository;
import com.secretariapay.api.repository.imports.AcademicStudentImportRowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AcademicStudentImportService {

    private final AcademicStudentImportBatchRepository batchRepository;
    private final AcademicStudentImportRowRepository rowRepository;

    public AcademicStudentImportService(
            AcademicStudentImportBatchRepository batchRepository,
            AcademicStudentImportRowRepository rowRepository
    ) {
        this.batchRepository = batchRepository;
        this.rowRepository = rowRepository;
    }

    @Transactional
    public AcademicStudentImportBatch createBatch(AcademicStudentImportBatch request) {
        if (request.getImportCode() == null || request.getImportCode().isBlank()) {
            request.setImportCode(generateImportCode());
        }

        if (request.getStatus() == null) {
            request.setStatus(AcademicStudentImportStatus.DRAFT);
        }

        return batchRepository.save(request);
    }

    @Transactional(readOnly = true)
    public List<AcademicStudentImportBatch> findAll() {
        return batchRepository.findAll()
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AcademicStudentImportBatch> findByInstitution(UUID institutionId) {
        return batchRepository.findByInstitutionIdOrderByCreatedAtDesc(institutionId);
    }

    @Transactional(readOnly = true)
    public AcademicStudentImportBatch findBatch(UUID id) {
        return batchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Importação de alunos não encontrada."));
    }

    @Transactional
    public AcademicStudentImportRow addRow(UUID batchId, AcademicStudentImportRow request) {
        AcademicStudentImportBatch batch = findBatch(batchId);

        AcademicStudentImportRow row = new AcademicStudentImportRow()
                .setBatchId(batch.getId())
                .setInstitutionId(batch.getInstitutionId())
                .setRowNumber(request.getRowNumber())
                .setAcademicYear(firstNonBlank(request.getAcademicYear(), batch.getAcademicYear()))
                .setSemesterNumber(request.getSemesterNumber())
                .setStudentNumber(trim(request.getStudentNumber()))
                .setFullName(trim(request.getFullName()))
                .setCourseName(trim(request.getCourseName()))
                .setClassName(trim(request.getClassName()))
                .setShiftName(trim(request.getShiftName()))
                .setDepartmentName(trim(request.getDepartmentName()))
                .setEmail(trim(request.getEmail()))
                .setPhone(trim(request.getPhone()))
                .setWhatsapp(trim(request.getWhatsapp()))
                .setResponsibleName(trim(request.getResponsibleName()))
                .setResponsiblePhone(trim(request.getResponsiblePhone()))
                .setResponsibleEmail(trim(request.getResponsibleEmail()))
                .setSourceAction(trim(request.getSourceAction()))
                .setStatus(AcademicStudentImportRowStatus.PENDING);

        AcademicStudentImportRow saved = rowRepository.save(row);
        refreshBatchCounters(batch.getId());

        return saved;
    }

    @Transactional(readOnly = true)
    public List<AcademicStudentImportRow> findRows(UUID batchId) {
        findBatch(batchId);
        return rowRepository.findByBatchIdOrderByRowNumberAscCreatedAtAsc(batchId);
    }

    @Transactional
    public AcademicStudentImportValidationResponse validateBatch(UUID batchId) {
        AcademicStudentImportBatch batch = findBatch(batchId);
        List<AcademicStudentImportRow> rows = rowRepository.findByBatchIdOrderByRowNumberAscCreatedAtAsc(batchId);

        Set<String> studentNumbers = new HashSet<>();
        int valid = 0;
        int invalid = 0;
        int duplicated = 0;

        for (AcademicStudentImportRow row : rows) {
            String validationMessage = validateRow(row);

            if (validationMessage != null) {
                row.setStatus(AcademicStudentImportRowStatus.INVALID)
                        .setValidationMessage(validationMessage);
                invalid++;
            } else if (row.getStudentNumber() != null && !studentNumbers.add(row.getStudentNumber())) {
                row.setStatus(AcademicStudentImportRowStatus.DUPLICATE)
                        .setValidationMessage("Número de estudante duplicado dentro do mesmo lote.");
                duplicated++;
            } else {
                row.setStatus(AcademicStudentImportRowStatus.VALID)
                        .setValidationMessage("Linha validada com sucesso.");
                valid++;
            }

            rowRepository.save(row);
        }

        batch.setStatus(AcademicStudentImportStatus.VALIDATED)
                .setTotalRows(rows.size())
                .setValidRows(valid)
                .setInvalidRows(invalid + duplicated)
                .setValidatedAt(LocalDateTime.now());

        batchRepository.save(batch);

        return new AcademicStudentImportValidationResponse()
                .setBatchId(batch.getId())
                .setImportCode(batch.getImportCode())
                .setTotalRows(rows.size())
                .setValidRows(valid)
                .setInvalidRows(invalid)
                .setDuplicateRows(duplicated)
                .setStatus(batch.getStatus().name())
                .setMessage("Validação concluída. As linhas válidas ficam prontas para importação/sincronização controlada.");
    }

    @Transactional
    public AcademicStudentImportValidationResponse completeBatch(UUID batchId) {
        AcademicStudentImportBatch batch = findBatch(batchId);
        List<AcademicStudentImportRow> rows = rowRepository.findByBatchIdOrderByRowNumberAscCreatedAtAsc(batchId);

        int imported = 0;

        for (AcademicStudentImportRow row : rows) {
            if (row.getStatus() == AcademicStudentImportRowStatus.VALID) {
                row.setStatus(AcademicStudentImportRowStatus.IMPORTED)
                        .setValidationMessage("Linha marcada como importada para staging WebSchool. Sincronização com cadastro final será feita na próxima fase.");
                rowRepository.save(row);
                imported++;
            }
        }

        batch.setStatus(AcademicStudentImportStatus.COMPLETED)
                .setImportedRows(imported)
                .setCompletedAt(LocalDateTime.now());

        batchRepository.save(batch);

        return new AcademicStudentImportValidationResponse()
                .setBatchId(batch.getId())
                .setImportCode(batch.getImportCode())
                .setTotalRows(rows.size())
                .setValidRows(batch.getValidRows())
                .setInvalidRows(batch.getInvalidRows())
                .setDuplicateRows(0)
                .setStatus(batch.getStatus().name())
                .setMessage("Lote concluído. Linhas válidas foram marcadas como IMPORTED no staging.");
    }

    private void refreshBatchCounters(UUID batchId) {
        AcademicStudentImportBatch batch = findBatch(batchId);
        List<AcademicStudentImportRow> rows = rowRepository.findByBatchIdOrderByRowNumberAscCreatedAtAsc(batchId);

        int valid = 0;
        int invalid = 0;
        int imported = 0;

        for (AcademicStudentImportRow row : rows) {
            if (row.getStatus() == AcademicStudentImportRowStatus.VALID) {
                valid++;
            } else if (row.getStatus() == AcademicStudentImportRowStatus.INVALID || row.getStatus() == AcademicStudentImportRowStatus.DUPLICATE) {
                invalid++;
            } else if (row.getStatus() == AcademicStudentImportRowStatus.IMPORTED) {
                imported++;
            }
        }

        batch.setTotalRows(rows.size())
                .setValidRows(valid)
                .setInvalidRows(invalid)
                .setImportedRows(imported);

        batchRepository.save(batch);
    }

    private String validateRow(AcademicStudentImportRow row) {
        if (isBlank(row.getStudentNumber())) {
            return "Número do estudante é obrigatório.";
        }

        if (isBlank(row.getFullName())) {
            return "Nome do estudante é obrigatório.";
        }

        if (isBlank(row.getCourseName())) {
            return "Curso é obrigatório.";
        }

        if (isBlank(row.getClassName())) {
            return "Turma é obrigatória.";
        }

        return null;
    }

    private String generateImportCode() {
        String code;

        do {
            code = "WSI-" + System.currentTimeMillis();
        } while (batchRepository.existsByImportCode(code));

        return code;
    }

    private String firstNonBlank(String first, String fallback) {
        return !isBlank(first) ? trim(first) : trim(fallback);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
