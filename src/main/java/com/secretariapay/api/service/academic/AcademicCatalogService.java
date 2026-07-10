package com.secretariapay.api.service.academic;

import com.secretariapay.api.entity.academic.AcademicClass;
import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.academic.Institution;
import com.secretariapay.api.entity.enums.academic.AcademicShift;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.academic.AcademicClassRepository;
import com.secretariapay.api.repository.academic.CourseRepository;
import com.secretariapay.api.repository.academic.InstitutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AcademicCatalogService {

    private final InstitutionRepository institutionRepository;
    private final CourseRepository courseRepository;
    private final AcademicClassRepository classRepository;

    public AcademicCatalogService(InstitutionRepository institutionRepository, CourseRepository courseRepository, AcademicClassRepository classRepository) {
        this.institutionRepository = institutionRepository;
        this.courseRepository = courseRepository;
        this.classRepository = classRepository;
    }

    @Transactional(readOnly = true)
    public List<CourseView> listCourses(UUID institutionId) {
        return courseRepository.findByInstitutionIdOrderByNameAsc(institutionId).stream().map(this::toCourseView).toList();
    }

    @Transactional
    public CourseView createCourse(CourseRequest request) {
        Institution institution = institutionRepository.findById(request.institutionId())
                .orElseThrow(() -> new NotFoundException("Instituição não encontrada."));
        validateCourse(request, null);
        Course saved = courseRepository.save(new Course()
                .setInstitution(institution)
                .setName(request.name().trim())
                .setCode(trim(request.code()))
                .setFaculty(trim(request.faculty()))
                .setDurationYears(request.durationYears())
                .setActive(request.active() == null || request.active()));
        return toCourseView(saved);
    }

    @Transactional
    public CourseView updateCourse(UUID id, CourseRequest request) {
        Course course = courseRepository.findById(id).orElseThrow(() -> new NotFoundException("Curso não encontrado."));
        validateCourse(request, id);
        course.setName(request.name().trim())
                .setCode(trim(request.code()))
                .setFaculty(trim(request.faculty()))
                .setDurationYears(request.durationYears())
                .setActive(request.active() == null ? course.getActive() : request.active());
        return toCourseView(courseRepository.save(course));
    }

    @Transactional(readOnly = true)
    public List<ClassView> listClasses(UUID institutionId) {
        return courseRepository.findByInstitutionIdOrderByNameAsc(institutionId).stream()
                .flatMap(course -> classRepository.findByCourseIdOrderByNameAsc(course.getId()).stream())
                .map(this::toClassView).toList();
    }

    @Transactional
    public ClassView createClass(ClassRequest request) {
        Course course = courseRepository.findById(request.courseId()).orElseThrow(() -> new NotFoundException("Curso não encontrado."));
        validateClass(request, null);
        AcademicClass saved = classRepository.save(new AcademicClass()
                .setCourse(course)
                .setName(request.name().trim())
                .setAcademicYear(request.academicYear().trim())
                .setYearLevel(request.yearLevel())
                .setShift(request.shift() == null ? AcademicShift.NIGHT : request.shift())
                .setActive(request.active() == null || request.active()));
        return toClassView(saved);
    }

    @Transactional
    public ClassView updateClass(UUID id, ClassRequest request) {
        AcademicClass academicClass = classRepository.findById(id).orElseThrow(() -> new NotFoundException("Turma não encontrada."));
        Course course = courseRepository.findById(request.courseId()).orElseThrow(() -> new NotFoundException("Curso não encontrado."));
        validateClass(request, id);
        academicClass.setCourse(course)
                .setName(request.name().trim())
                .setAcademicYear(request.academicYear().trim())
                .setYearLevel(request.yearLevel())
                .setShift(request.shift() == null ? academicClass.getShift() : request.shift())
                .setActive(request.active() == null ? academicClass.getActive() : request.active());
        return toClassView(classRepository.save(academicClass));
    }

    private void validateCourse(CourseRequest request, UUID currentId) {
        if (request.institutionId() == null) throw new IllegalArgumentException("Instituição é obrigatória.");
        if (request.name() == null || request.name().isBlank()) throw new IllegalArgumentException("Nome do curso é obrigatório.");
        courseRepository.findFirstByInstitutionIdAndNameIgnoreCase(request.institutionId(), request.name().trim())
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> { throw new IllegalArgumentException("Já existe um curso com este nome na instituição."); });
        if (request.code() != null && !request.code().isBlank()) {
            courseRepository.findFirstByInstitutionIdAndCodeIgnoreCase(request.institutionId(), request.code().trim())
                    .filter(existing -> !existing.getId().equals(currentId))
                    .ifPresent(existing -> { throw new IllegalArgumentException("Já existe um curso com este código na instituição."); });
        }
    }

    private void validateClass(ClassRequest request, UUID currentId) {
        if (request.courseId() == null) throw new IllegalArgumentException("Curso é obrigatório.");
        if (request.name() == null || request.name().isBlank()) throw new IllegalArgumentException("Nome da turma é obrigatório.");
        if (request.academicYear() == null || request.academicYear().isBlank()) throw new IllegalArgumentException("Ano letivo é obrigatório.");
        AcademicShift shift = request.shift() == null ? AcademicShift.NIGHT : request.shift();
        classRepository.findFirstByCourseIdAndNameIgnoreCaseAndAcademicYearAndShift(request.courseId(), request.name().trim(), request.academicYear().trim(), shift)
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> { throw new IllegalArgumentException("Já existe uma turma igual para este curso, ano letivo e turno."); });
    }

    private CourseView toCourseView(Course course) {
        return new CourseView(course.getId(), course.getInstitution().getId(), course.getName(), course.getCode(), course.getFaculty(), course.getDurationYears(), course.getActive(), course.getCreatedAt(), course.getUpdatedAt());
    }

    private ClassView toClassView(AcademicClass academicClass) {
        Course course = academicClass.getCourse();
        return new ClassView(academicClass.getId(), course.getId(), course.getName(), academicClass.getName(), academicClass.getAcademicYear(), academicClass.getYearLevel(), academicClass.getShift(), academicClass.getActive(), academicClass.getCreatedAt(), academicClass.getUpdatedAt());
    }

    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public record CourseRequest(UUID institutionId, String name, String code, String faculty, Integer durationYears, Boolean active) {}
    public record ClassRequest(UUID courseId, String name, String academicYear, Integer yearLevel, AcademicShift shift, Boolean active) {}
    public record CourseView(UUID id, UUID institutionId, String name, String code, String faculty, Integer durationYears, Boolean active, java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {}
    public record ClassView(UUID id, UUID courseId, String courseName, String name, String academicYear, Integer yearLevel, AcademicShift shift, Boolean active, java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {}
}
