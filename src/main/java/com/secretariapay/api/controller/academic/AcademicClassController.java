package com.secretariapay.api.controller.academic;

import com.secretariapay.api.dto.academic.AcademicClassRequest;
import com.secretariapay.api.dto.academic.AcademicClassResponse;
import com.secretariapay.api.service.academic.AcademicClassService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/academic-classes")
public class AcademicClassController {

    private final AcademicClassService service;

    public AcademicClassController(AcademicClassService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AcademicClassResponse create(@Valid @RequestBody AcademicClassRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<AcademicClassResponse> findAll(@RequestParam(required = false) UUID courseId) {
        if (courseId != null) {
            return service.findByCourseId(courseId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public AcademicClassResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public AcademicClassResponse update(@PathVariable UUID id, @Valid @RequestBody AcademicClassRequest request) {
        return service.update(id, request);
    }
}
