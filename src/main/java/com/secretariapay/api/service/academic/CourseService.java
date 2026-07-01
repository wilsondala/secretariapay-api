package com.secretariapay.api.service.academic;

import com.secretariapay.api.dto.academic.CourseRequest;
import com.secretariapay.api.dto.academic.CourseResponse;
import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.academic.Institution;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.academic.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CourseService {

    private final CourseRepository repository;
    private final InstitutionService institutionService;

    public CourseService(CourseRepository repository, InstitutionService institutionService) {
        this.repository = repository;
        this.institutionService = institutionService;
    }

    @Transactional
    public CourseResponse create(CourseRequest request) {
        Institution institution = institutionService.findEntityById(request.getInstitutionId());
        Course course = applyRequest(new Course(), request, institution);
        return toResponse(repository.save(course));
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> findByInstitutionId(UUID institutionId) {
        return repository.findByInstitutionIdOrderByNameAsc(institutionId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse findById(UUID id) {
        return toResponse(findEntityById(id));
    }

    @Transactional
    public CourseResponse update(UUID id, CourseRequest request) {
        Course course = findEntityById(id);
        Institution institution = institutionService.findEntityById(request.getInstitutionId());
        return toResponse(repository.save(applyRequest(course, request, institution)));
    }

    public Course findEntityById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Curso não encontrado."));
    }

    private Course applyRequest(Course course, CourseRequest request, Institution institution) {
        course
                .setInstitution(institution)
                .setName(request.getName())
                .setCode(request.getCode())
                .setFaculty(request.getFaculty())
                .setDurationYears(request.getDurationYears());
        if (request.getActive() != null) {
            course.setActive(request.getActive());
        }
        return course;
    }

    private CourseResponse toResponse(Course course) {
        return new CourseResponse()
                .setId(course.getId())
                .setInstitutionId(course.getInstitution().getId())
                .setInstitutionName(course.getInstitution().getName())
                .setName(course.getName())
                .setCode(course.getCode())
                .setFaculty(course.getFaculty())
                .setDurationYears(course.getDurationYears())
                .setActive(course.getActive())
                .setCreatedAt(course.getCreatedAt())
                .setUpdatedAt(course.getUpdatedAt());
    }
}
