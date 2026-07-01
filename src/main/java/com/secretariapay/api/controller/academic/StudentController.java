package com.secretariapay.api.controller.academic;

import com.secretariapay.api.dto.academic.StudentRequest;
import com.secretariapay.api.dto.academic.StudentResponse;
import com.secretariapay.api.service.academic.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentService service;

    public StudentController(StudentService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse create(@Valid @RequestBody StudentRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<StudentResponse> findAll(@RequestParam(required = false) UUID academicClassId) {
        if (academicClassId != null) {
            return service.findByAcademicClassId(academicClassId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public StudentResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @GetMapping("/number/{studentNumber}")
    public StudentResponse findByStudentNumber(@PathVariable String studentNumber) {
        return service.findByStudentNumber(studentNumber);
    }

    @PutMapping("/{id}")
    public StudentResponse update(@PathVariable UUID id, @Valid @RequestBody StudentRequest request) {
        return service.update(id, request);
    }
}
