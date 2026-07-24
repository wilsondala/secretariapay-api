package com.secretariapay.api.controller.academic;

import com.secretariapay.api.service.academic.AcademicCatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/academic-catalog")
public class AcademicCatalogController {

    private static final String AUTHORITIES = "hasAnyAuthority('ADMIN_GLOBAL','ROLE_ADMIN_GLOBAL','ADMIN_INSTITUTION','ROLE_ADMIN_INSTITUTION','ADMIN_IMETRO','ROLE_ADMIN_IMETRO','DIRECAO','ROLE_DIRECAO','SECRETARIA','ROLE_SECRETARIA','TIC','ROLE_TIC')";
    private final AcademicCatalogService service;

    public AcademicCatalogController(AcademicCatalogService service) {
        this.service = service;
    }

    @GetMapping("/institutions/{institutionId}/courses")
    @PreAuthorize(AUTHORITIES)
    public List<AcademicCatalogService.CourseView> listCourses(@PathVariable UUID institutionId) {
        return service.listCourses(institutionId);
    }

    @PostMapping("/courses")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(AUTHORITIES)
    public AcademicCatalogService.CourseView createCourse(@RequestBody AcademicCatalogService.CourseRequest request) {
        return service.createCourse(request);
    }

    @PutMapping("/courses/{id}")
    @PreAuthorize(AUTHORITIES)
    public AcademicCatalogService.CourseView updateCourse(@PathVariable UUID id, @RequestBody AcademicCatalogService.CourseRequest request) {
        return service.updateCourse(id, request);
    }

    @GetMapping("/institutions/{institutionId}/classes")
    @PreAuthorize(AUTHORITIES)
    public List<AcademicCatalogService.ClassView> listClasses(@PathVariable UUID institutionId) {
        return service.listClasses(institutionId);
    }

    @PostMapping("/classes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(AUTHORITIES)
    public AcademicCatalogService.ClassView createClass(@RequestBody AcademicCatalogService.ClassRequest request) {
        return service.createClass(request);
    }

    @PutMapping("/classes/{id}")
    @PreAuthorize(AUTHORITIES)
    public AcademicCatalogService.ClassView updateClass(@PathVariable UUID id, @RequestBody AcademicCatalogService.ClassRequest request) {
        return service.updateClass(id, request);
    }
}
