package com.secretariapay.api.service.academic;

import com.secretariapay.api.dto.academic.AcademicClassRequest;
import com.secretariapay.api.dto.academic.AcademicClassResponse;
import com.secretariapay.api.entity.academic.AcademicClass;
import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.academic.AcademicClassRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AcademicClassService {

    private final AcademicClassRepository repository;
    private final CourseService courseService;

    public AcademicClassService(AcademicClassRepository repository, CourseService courseService) {
        this.repository = repository;
        this.courseService = courseService;
    }

    @Transactional
    public AcademicClassResponse create(AcademicClassRequest request) {
        Course course = courseService.findEntityById(request.getCourseId());
        AcademicClass academicClass = applyRequest(new AcademicClass(), request, course);
        return toResponse(repository.save(academicClass));
    }

    @Transactional(readOnly = true)
    public List<AcademicClassResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AcademicClassResponse> findByCourseId(UUID courseId) {
        return repository.findByCourseIdOrderByNameAsc(courseId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AcademicClassResponse findById(UUID id) {
        return toResponse(findEntityById(id));
    }

    @Transactional
    public AcademicClassResponse update(UUID id, AcademicClassRequest request) {
        AcademicClass academicClass = findEntityById(id);
        Course course = courseService.findEntityById(request.getCourseId());
        return toResponse(repository.save(applyRequest(academicClass, request, course)));
    }

    public AcademicClass findEntityById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Turma não encontrada."));
    }

    private AcademicClass applyRequest(AcademicClass academicClass, AcademicClassRequest request, Course course) {
        academicClass
                .setCourse(course)
                .setName(request.getName())
                .setAcademicYear(request.getAcademicYear())
                .setYearLevel(request.getYearLevel());
        if (request.getShift() != null) {
            academicClass.setShift(request.getShift());
        }
        if (request.getActive() != null) {
            academicClass.setActive(request.getActive());
        }
        return academicClass;
    }

    private AcademicClassResponse toResponse(AcademicClass academicClass) {
        return new AcademicClassResponse()
                .setId(academicClass.getId())
                .setCourseId(academicClass.getCourse().getId())
                .setCourseName(academicClass.getCourse().getName())
                .setName(academicClass.getName())
                .setAcademicYear(academicClass.getAcademicYear())
                .setYearLevel(academicClass.getYearLevel())
                .setShift(academicClass.getShift())
                .setActive(academicClass.getActive())
                .setCreatedAt(academicClass.getCreatedAt())
                .setUpdatedAt(academicClass.getUpdatedAt());
    }
}
