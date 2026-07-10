package com.secretariapay.api.service.imports;

import com.secretariapay.api.dto.imports.AcademicStudentImportRowUpdateRequest;
import com.secretariapay.api.entity.enums.imports.AcademicStudentImportRowStatus;
import com.secretariapay.api.entity.imports.AcademicStudentImportBatch;
import com.secretariapay.api.entity.imports.AcademicStudentImportRow;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.imports.AcademicStudentImportRowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
public class AcademicStudentImportMaintenanceService {

    private final AcademicStudentImportService importService;
    private final AcademicStudentImportRowRepository rowRepository;

    public AcademicStudentImportMaintenanceService(
            AcademicStudentImportService importService,
            AcademicStudentImportRowRepository rowRepository
    ) {
        this.importService = importService;
        this.rowRepository = rowRepository;
    }

    @Transactional
    public AcademicStudentImportRow updateRow(UUID batchId, UUID rowId, AcademicStudentImportRowUpdateRequest request) {
        AcademicStudentImportBatch batch = importService.findBatch(batchId);
        AcademicStudentImportRow row = rowRepository.findById(rowId)
                .orElseThrow(() -> new NotFoundException("Linha de importação não encontrada."));

        if (!batch.getId().equals(row.getBatchId())) {
            throw new IllegalArgumentException("A linha informada não pertence ao lote selecionado.");
        }

        if (row.getStatus() == AcademicStudentImportRowStatus.SYNCED) {
            throw new IllegalStateException("Linhas já sincronizadas não podem ser alteradas pelo staging.");
        }

        row.setAcademicYear(trim(request.getAcademicYear()))
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
                .setStatus(AcademicStudentImportRowStatus.PENDING)
                .setValidationMessage("Linha alterada. Execute a validação novamente.");

        AcademicStudentImportRow saved = rowRepository.save(row);
        importService.validateBatch(batchId);
        return rowRepository.findById(saved.getId()).orElse(saved);
    }

    @Transactional(readOnly = true)
    public byte[] buildErrorReport(UUID batchId) {
        AcademicStudentImportBatch batch = importService.findBatch(batchId);
        List<AcademicStudentImportRow> rows = importService.findRows(batchId);

        StringBuilder csv = new StringBuilder();
        csv.append("Lote;Linha;Estado;Matrícula;Nome;Curso;Turma;Turno;Telefone;WhatsApp;E-mail;Erro\n");

        rows.stream()
                .filter(row -> row.getStatus() == AcademicStudentImportRowStatus.INVALID
                        || row.getStatus() == AcademicStudentImportRowStatus.DUPLICATE)
                .forEach(row -> csv.append(value(batch.getImportCode())).append(';')
                        .append(row.getRowNumber() == null ? "" : row.getRowNumber()).append(';')
                        .append(value(row.getStatus() == null ? null : row.getStatus().name())).append(';')
                        .append(value(row.getStudentNumber())).append(';')
                        .append(value(row.getFullName())).append(';')
                        .append(value(row.getCourseName())).append(';')
                        .append(value(row.getClassName())).append(';')
                        .append(value(row.getShiftName())).append(';')
                        .append(value(row.getPhone())).append(';')
                        .append(value(row.getWhatsapp())).append(';')
                        .append(value(row.getEmail())).append(';')
                        .append(value(row.getValidationMessage())).append('\n'));

        return ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
    }

    private String value(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private String trim(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
