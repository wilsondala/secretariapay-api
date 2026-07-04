package com.secretariapay.api.service.imports;

import com.secretariapay.api.dto.imports.AcademicStudentImportSyncResponse;
import com.secretariapay.api.dto.imports.AcademicStudentImportValidationResponse;
import com.secretariapay.api.entity.academic.AcademicClass;
import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.academic.Institution;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.academic.AcademicShift;
import com.secretariapay.api.entity.enums.imports.AcademicStudentImportRowStatus;
import com.secretariapay.api.entity.enums.imports.AcademicStudentImportStatus;
import com.secretariapay.api.entity.imports.AcademicStudentImportBatch;
import com.secretariapay.api.entity.imports.AcademicStudentImportRow;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.academic.AcademicClassRepository;
import com.secretariapay.api.repository.academic.CourseRepository;
import com.secretariapay.api.repository.academic.InstitutionRepository;
import com.secretariapay.api.repository.academic.StudentRepository;
import com.secretariapay.api.repository.imports.AcademicStudentImportBatchRepository;
import com.secretariapay.api.repository.imports.AcademicStudentImportRowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AcademicStudentImportService {

    private static final String DEFAULT_ACADEMIC_YEAR = "2025/2026";
    private static final Set<AcademicStudentImportRowStatus> SYNCABLE_STATUSES = Set.of(
            AcademicStudentImportRowStatus.VALID,
            AcademicStudentImportRowStatus.IMPORTED,
            AcademicStudentImportRowStatus.SYNCED
    );

    private final AcademicStudentImportBatchRepository batchRepository;
    private final AcademicStudentImportRowRepository rowRepository;
    private final InstitutionRepository institutionRepository;
    private final CourseRepository courseRepository;
    private final AcademicClassRepository academicClassRepository;
    private final StudentRepository studentRepository;

    public AcademicStudentImportService(
            AcademicStudentImportBatchRepository batchRepository,
            AcademicStudentImportRowRepository rowRepository,
            InstitutionRepository institutionRepository,
            CourseRepository courseRepository,
            AcademicClassRepository academicClassRepository,
            StudentRepository studentRepository
    ) {
        this.batchRepository = batchRepository;
        this.rowRepository = rowRepository;
        this.institutionRepository = institutionRepository;
        this.courseRepository = courseRepository;
        this.academicClassRepository = academicClassRepository;
        this.studentRepository = studentRepository;
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

        Set<String> studentNumbers = new java.util.HashSet<>();
        int valid = 0;
        int invalid = 0;
        int duplicated = 0;
        int importedOrSynced = 0;

        for (AcademicStudentImportRow row : rows) {
            if (row.getStatus() == AcademicStudentImportRowStatus.IMPORTED
                    || row.getStatus() == AcademicStudentImportRowStatus.SYNCED) {
                importedOrSynced++;
                rowRepository.save(row);
                continue;
            }

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
                .setImportedRows(importedOrSynced)
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
                        .setValidationMessage("Linha marcada como importada para staging WebSchool. Sincronize com students/courses/classes pelo endpoint /sync.");
                rowRepository.save(row);
                imported++;
            } else if (row.getStatus() == AcademicStudentImportRowStatus.IMPORTED
                    || row.getStatus() == AcademicStudentImportRowStatus.SYNCED) {
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
                .setMessage("Lote concluído. Linhas válidas/importadas foram contabilizadas como IMPORTED no staging.");
    }

    @Transactional
    public AcademicStudentImportSyncResponse syncBatch(UUID batchId) {
        AcademicStudentImportBatch batch = findBatch(batchId);
        Institution institution = institutionRepository.findById(batch.getInstitutionId())
                .orElseThrow(() -> new NotFoundException("Instituição do lote de importação não encontrada."));

        List<AcademicStudentImportRow> rows = rowRepository.findByBatchIdOrderByRowNumberAscCreatedAtAsc(batchId);

        int processed = 0;
        int synced = 0;
        int skipped = 0;
        int createdCourses = 0;
        int reusedCourses = 0;
        int createdClasses = 0;
        int reusedClasses = 0;
        int createdStudents = 0;
        int updatedStudents = 0;

        for (AcademicStudentImportRow row : rows) {
            if (!SYNCABLE_STATUSES.contains(row.getStatus())) {
                skipped++;
                continue;
            }

            String validationMessage = validateRow(row);

            if (validationMessage != null) {
                row.setStatus(AcademicStudentImportRowStatus.INVALID)
                        .setValidationMessage(validationMessage);
                rowRepository.save(row);
                skipped++;
                continue;
            }

            processed++;

            CourseMatch courseMatch = findOrCreateCourse(institution, row);
            Course course = courseMatch.course();

            if (courseMatch.created()) {
                createdCourses++;
            } else {
                reusedCourses++;
            }

            AcademicClassMatch classMatch = findOrCreateAcademicClass(course, row, batch);
            AcademicClass academicClass = classMatch.academicClass();

            if (classMatch.created()) {
                createdClasses++;
            } else {
                reusedClasses++;
            }

            StudentSyncResult studentResult = findOrCreateOrUpdateStudent(academicClass, row);
            Student student = studentResult.student();

            if (studentResult.created()) {
                createdStudents++;
            } else {
                updatedStudents++;
            }

            row.setMatchedStudentId(student.getId())
                    .setStatus(AcademicStudentImportRowStatus.SYNCED)
                    .setValidationMessage("Linha sincronizada com students/courses/classes reais.");
            rowRepository.save(row);
            synced++;
        }

        batch.setStatus(AcademicStudentImportStatus.COMPLETED)
                .setImportedRows(countImportedOrSynced(rows))
                .setCompletedAt(batch.getCompletedAt() == null ? LocalDateTime.now() : batch.getCompletedAt());
        batchRepository.save(batch);

        return new AcademicStudentImportSyncResponse()
                .setBatchId(batch.getId())
                .setImportCode(batch.getImportCode())
                .setTotalRows(rows.size())
                .setProcessedRows(processed)
                .setSyncedRows(synced)
                .setSkippedRows(skipped)
                .setCreatedCourses(createdCourses)
                .setReusedCourses(reusedCourses)
                .setCreatedClasses(createdClasses)
                .setReusedClasses(reusedClasses)
                .setCreatedStudents(createdStudents)
                .setUpdatedStudents(updatedStudents)
                .setStatus(batch.getStatus().name())
                .setMessage("Sincronização concluída: staging WebSchool ligado ao cadastro académico real.");
    }

    private CourseMatch findOrCreateCourse(Institution institution, AcademicStudentImportRow row) {
        String courseName = trim(row.getCourseName());

        Optional<Course> existing = courseRepository.findFirstByInstitutionIdAndNameIgnoreCase(institution.getId(), courseName);

        if (existing.isPresent()) {
            Course course = existing.get();

            if (!isBlank(row.getDepartmentName()) && isBlank(course.getFaculty())) {
                course.setFaculty(trim(row.getDepartmentName()));
                courseRepository.save(course);
            }

            return new CourseMatch(course, false);
        }

        Course course = new Course()
                .setInstitution(institution)
                .setName(courseName)
                .setCode(buildCode(courseName))
                .setFaculty(trim(row.getDepartmentName()))
                .setActive(true);

        return new CourseMatch(courseRepository.save(course), true);
    }

    private AcademicClassMatch findOrCreateAcademicClass(Course course, AcademicStudentImportRow row, AcademicStudentImportBatch batch) {
        String className = trim(row.getClassName());
        String academicYear = firstNonBlank(row.getAcademicYear(), batch.getAcademicYear(), DEFAULT_ACADEMIC_YEAR);
        AcademicShift shift = parseShift(row.getShiftName());

        Optional<AcademicClass> existing = academicClassRepository
                .findFirstByCourseIdAndNameIgnoreCaseAndAcademicYearAndShift(course.getId(), className, academicYear, shift);

        if (existing.isPresent()) {
            return new AcademicClassMatch(existing.get(), false);
        }

        AcademicClass academicClass = new AcademicClass()
                .setCourse(course)
                .setName(className)
                .setAcademicYear(academicYear)
                .setYearLevel(parseYearLevel(className))
                .setShift(shift)
                .setActive(true);

        return new AcademicClassMatch(academicClassRepository.save(academicClass), true);
    }

    private StudentSyncResult findOrCreateOrUpdateStudent(AcademicClass academicClass, AcademicStudentImportRow row) {
        Optional<Student> existing = studentRepository.findByStudentNumber(trim(row.getStudentNumber()));

        Student student = existing.orElseGet(Student::new);
        boolean created = existing.isEmpty();

        student.setAcademicClass(academicClass)
                .setStudentNumber(trim(row.getStudentNumber()))
                .setFullName(trim(row.getFullName()));

        if (!isBlank(row.getEmail())) {
            student.setEmail(trim(row.getEmail()));
        }

        if (!isBlank(row.getPhone())) {
            student.setPhone(trim(row.getPhone()));
        }

        if (!isBlank(row.getWhatsapp())) {
            student.setWhatsapp(trim(row.getWhatsapp()));
        } else if (!isBlank(row.getPhone()) && isBlank(student.getWhatsapp())) {
            student.setWhatsapp(trim(row.getPhone()));
        }

        if (!isBlank(row.getResponsibleName())) {
            student.setGuardianName(trim(row.getResponsibleName()));
        }

        if (!isBlank(row.getResponsiblePhone())) {
            student.setGuardianPhone(trim(row.getResponsiblePhone()));
        }

        if (!isBlank(row.getResponsibleEmail())) {
            student.setGuardianEmail(trim(row.getResponsibleEmail()));
        }

        return new StudentSyncResult(studentRepository.save(student), created);
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
            } else if (row.getStatus() == AcademicStudentImportRowStatus.IMPORTED
                    || row.getStatus() == AcademicStudentImportRowStatus.SYNCED) {
                imported++;
            }
        }

        batch.setTotalRows(rows.size())
                .setValidRows(valid)
                .setInvalidRows(invalid)
                .setImportedRows(imported);

        batchRepository.save(batch);
    }

    private int countImportedOrSynced(List<AcademicStudentImportRow> rows) {
        int imported = 0;

        for (AcademicStudentImportRow row : rows) {
            if (row.getStatus() == AcademicStudentImportRowStatus.IMPORTED
                    || row.getStatus() == AcademicStudentImportRowStatus.SYNCED) {
                imported++;
            }
        }

        return imported;
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

    private AcademicShift parseShift(String value) {
        if (isBlank(value)) {
            return AcademicShift.NIGHT;
        }

        String normalized = normalize(value);

        if (normalized.contains("MANHA") || normalized.contains("MORNING")) {
            return AcademicShift.MORNING;
        }

        if (normalized.contains("TARDE") || normalized.contains("AFTERNOON")) {
            return AcademicShift.AFTERNOON;
        }

        if (normalized.contains("EVENING")) {
            return AcademicShift.EVENING;
        }

        if (normalized.contains("FIM") || normalized.contains("WEEKEND") || normalized.contains("SABADO") || normalized.contains("SABADO") || normalized.contains("DOMINGO")) {
            return AcademicShift.WEEKEND;
        }

        return AcademicShift.NIGHT;
    }

    private Integer parseYearLevel(String className) {
        if (isBlank(className)) {
            return null;
        }

        Matcher matcher = Pattern.compile("(\\d+)").matcher(className);

        if (!matcher.find()) {
            return null;
        }

        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String buildCode(String value) {
        String normalized = normalize(value)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        if (normalized.isBlank()) {
            return null;
        }

        return normalized.length() <= 50 ? normalized : normalized.substring(0, 50);
    }

    private String generateImportCode() {
        String code;

        do {
            code = "WSI-" + System.currentTimeMillis();
        } while (batchRepository.existsByImportCode(code));

        return code;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return trim(value);
            }
        }

        return null;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return normalized.toUpperCase(Locale.ROOT).trim();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record CourseMatch(Course course, boolean created) {
    }

    private record AcademicClassMatch(AcademicClass academicClass, boolean created) {
    }

    private record StudentSyncResult(Student student, boolean created) {
    }
}
