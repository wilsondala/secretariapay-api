package com.secretariapay.api.service.imports;

import com.secretariapay.api.entity.imports.AcademicStudentImportBatch;
import com.secretariapay.api.entity.imports.AcademicStudentImportRow;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AcademicStudentCsvImportService {

    private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;
    private final AcademicStudentImportService importService;

    public AcademicStudentCsvImportService(AcademicStudentImportService importService) {
        this.importService = importService;
    }

    @Transactional
    public AcademicStudentImportBatch upload(
            MultipartFile file,
            UUID institutionId,
            String academicYear,
            String semester,
            String sourceName,
            String createdBy
    ) {
        validateFile(file);
        String extension = extension(file.getOriginalFilename());
        String formatLabel = extension.equals("csv") ? "CSV" : "Excel";

        AcademicStudentImportBatch batch = new AcademicStudentImportBatch()
                .setInstitutionId(institutionId)
                .setSourceSystem("WEBSCHOOL_ADMINUT")
                .setSourceName(isBlank(sourceName) ? "WebSchool IMETRO" : sourceName.trim())
                .setFileName(file.getOriginalFilename())
                .setAcademicYear(trimToNull(academicYear))
                .setSemester(trimToNull(semester))
                .setCreatedBy(trimToNull(createdBy))
                .setNotes("Lote criado por upload " + formatLabel + " no painel SecretáriaPay.");

        AcademicStudentImportBatch savedBatch = importService.createBatch(batch);
        List<List<String>> records = readRecords(file, extension);
        if (records.size() < 2) {
            throw new IllegalArgumentException("O ficheiro deve conter cabeçalho e pelo menos uma linha de dados.");
        }

        Map<String, Integer> header = buildHeaderMap(records.get(0));
        requireAny(header, "numeroestudante", "nrestudante", "matricula", "studentnumber");
        requireAny(header, "nome", "nomecompleto", "fullname", "estudante");
        requireAny(header, "curso", "course", "coursename");

        int rowNumber = 1;
        for (int i = 1; i < records.size(); i++) {
            List<String> values = records.get(i);
            if (isEmptyRecord(values)) continue;

            AcademicStudentImportRow row = new AcademicStudentImportRow()
                    .setRowNumber(rowNumber++)
                    .setAcademicYear(firstNonBlank(value(values, header, "anolectivo", "anoletivo", "academicyear"), academicYear))
                    .setSemesterNumber(parseSemester(value(values, header, "semestre", "semesternumber"), semester))
                    .setStudentNumber(value(values, header, "numeroestudante", "nrestudante", "matricula", "studentnumber"))
                    .setFullName(value(values, header, "nome", "nomecompleto", "fullname", "estudante"))
                    .setCourseName(value(values, header, "curso", "course", "coursename"))
                    .setClassName(value(values, header, "turma", "classe", "class", "classname"))
                    .setShiftName(value(values, header, "turno", "shift", "shiftname"))
                    .setDepartmentName(value(values, header, "departamento", "department", "departmentname"))
                    .setEmail(value(values, header, "email", "correio", "correioelectronico"))
                    .setPhone(value(values, header, "telefone", "phone", "contacto"))
                    .setWhatsapp(value(values, header, "whatsapp", "telefonewhatsapp"))
                    .setResponsibleName(value(values, header, "responsavel", "nomeresponsavel", "responsiblename"))
                    .setResponsiblePhone(value(values, header, "telefoneresponsavel", "responsiblephone"))
                    .setResponsibleEmail(value(values, header, "emailresponsavel", "responsibleemail"));

            importService.addRow(savedBatch.getId(), row);
        }

        importService.validateBatch(savedBatch.getId());
        return importService.findBatch(savedBatch.getId());
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Selecione um ficheiro CSV ou Excel para importar.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("O ficheiro excede o limite de 10 MB.");
        }
        String extension = extension(file.getOriginalFilename());
        if (!extension.equals("csv") && !extension.equals("xlsx") && !extension.equals("xls")) {
            throw new IllegalArgumentException("Formato inválido. São aceites ficheiros .csv, .xlsx e .xls.");
        }
    }

    private List<List<String>> readRecords(MultipartFile file, String extension) {
        return extension.equals("csv") ? readCsvRecords(file) : readExcelRecords(file);
    }

    private List<List<String>> readExcelRecords(MultipartFile file) {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) return List.of();
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            List<List<String>> records = new ArrayList<>();

            int firstRow = sheet.getFirstRowNum();
            int lastRow = sheet.getLastRowNum();
            int columnCount = 0;
            Row headerRow = sheet.getRow(firstRow);
            if (headerRow != null) columnCount = Math.max(0, headerRow.getLastCellNum());

            for (int rowIndex = firstRow; rowIndex <= lastRow; rowIndex++) {
                Row excelRow = sheet.getRow(rowIndex);
                List<String> values = new ArrayList<>();
                for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                    Cell cell = excelRow == null ? null : excelRow.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String value = cell == null ? "" : formatter.formatCellValue(cell, evaluator).trim();
                    values.add(value);
                }
                records.add(values);
            }
            return records;
        } catch (IOException | RuntimeException e) {
            throw new IllegalArgumentException("Não foi possível ler o ficheiro Excel. Confirme se o documento não está corrompido ou protegido.", e);
        }
    }

    private List<List<String>> readCsvRecords(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            List<String> lines = reader.lines().toList();
            if (lines.isEmpty()) return List.of();
            char delimiter = detectDelimiter(lines.get(0));
            List<List<String>> records = new ArrayList<>();
            StringBuilder pending = new StringBuilder();
            for (String line : lines) {
                if (!pending.isEmpty()) pending.append('\n');
                pending.append(line);
                if (hasBalancedQuotes(pending.toString())) {
                    records.add(parseCsvLine(removeBom(pending.toString()), delimiter));
                    pending.setLength(0);
                }
            }
            if (!pending.isEmpty()) {
                throw new IllegalArgumentException("CSV inválido: aspas não fechadas.");
            }
            return records;
        } catch (IOException e) {
            throw new IllegalArgumentException("Não foi possível ler o ficheiro CSV.", e);
        }
    }

    private char detectDelimiter(String header) {
        long semicolons = header.chars().filter(ch -> ch == ';').count();
        long commas = header.chars().filter(ch -> ch == ',').count();
        long tabs = header.chars().filter(ch -> ch == '\t').count();
        if (tabs > semicolons && tabs > commas) return '\t';
        return semicolons >= commas ? ';' : ',';
    }

    private boolean hasBalancedQuotes(String text) {
        int quotes = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '"') {
                if (i + 1 < text.length() && text.charAt(i + 1) == '"') i++;
                else quotes++;
            }
        }
        return quotes % 2 == 0;
    }

    private List<String> parseCsvLine(String line, char delimiter) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == delimiter && !quoted) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    private Map<String, Integer> buildHeaderMap(List<String> columns) {
        Map<String, Integer> result = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) result.put(normalize(columns.get(i)), i);
        return result;
    }

    private void requireAny(Map<String, Integer> header, String... aliases) {
        for (String alias : aliases) if (header.containsKey(normalize(alias))) return;
        throw new IllegalArgumentException("Ficheiro inválido: falta uma coluna obrigatória. Aceites: " + String.join(", ", aliases));
    }

    private String value(List<String> values, Map<String, Integer> header, String... aliases) {
        for (String alias : aliases) {
            Integer index = header.get(normalize(alias));
            if (index != null && index < values.size()) return trimToNull(values.get(index));
        }
        return null;
    }

    private Integer parseSemester(String raw, String fallback) {
        String source = firstNonBlank(raw, fallback);
        if (isBlank(source)) return null;
        String digits = source.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return null;
        try { return Integer.parseInt(digits.substring(0, 1)); }
        catch (NumberFormatException ignored) { return null; }
    }

    private String extension(String fileName) {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String normalize(String value) {
        if (value == null) return "";
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]", "")
                .toLowerCase(Locale.ROOT);
    }

    private boolean isEmptyRecord(List<String> values) {
        return values.stream().allMatch(this::isBlank);
    }

    private String removeBom(String value) {
        return value != null && value.startsWith("\uFEFF") ? value.substring(1) : value;
    }

    private String firstNonBlank(String first, String second) {
        return !isBlank(first) ? first.trim() : trimToNull(second);
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
