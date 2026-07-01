package com.secretariapay.api.controller.academic;

import com.secretariapay.api.dto.academic.InstitutionRequest;
import com.secretariapay.api.dto.academic.InstitutionResponse;
import com.secretariapay.api.service.academic.InstitutionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/institutions")
public class InstitutionController {

    private final InstitutionService service;

    public InstitutionController(InstitutionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstitutionResponse create(@Valid @RequestBody InstitutionRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<InstitutionResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public InstitutionResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public InstitutionResponse update(@PathVariable UUID id, @Valid @RequestBody InstitutionRequest request) {
        return service.update(id, request);
    }
}
