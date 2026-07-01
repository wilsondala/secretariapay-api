package com.secretariapay.api.controller.academic;

import com.secretariapay.api.dto.academic.CourseRequest;
import com.secretariapay.api.dto.academic.CourseResponse;
import com.secretariapay.api.service.academic.CourseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {

    private final CourseService service;

    public CourseController(CourseService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(@Valid @RequestBody CourseRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<CourseResponse> findAll(@RequestParam(required = false) UUID institutionId) {
        if (institutionId != null) {
            return service.findByInstitutionId(institutionId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public CourseResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public CourseResponse update(@PathVariable UUID id, @Valid @RequestBody CourseRequest request) {
        return service.update(id, request);
    }
}
