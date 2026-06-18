package com.vairapido.api.controller;

import com.vairapido.api.dto.transportcompany.TransportCompanyRequest;
import com.vairapido.api.dto.transportcompany.TransportCompanyResponse;
import com.vairapido.api.service.TransportCompanyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transport-companies")
public class TransportCompanyController {

    private final TransportCompanyService service;

    public TransportCompanyController(TransportCompanyService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransportCompanyResponse create(@Valid @RequestBody TransportCompanyRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<TransportCompanyResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public TransportCompanyResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public TransportCompanyResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody TransportCompanyRequest request
    ) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/activate")
    public TransportCompanyResponse activate(@PathVariable UUID id) {
        return service.activate(id);
    }

    @PatchMapping("/{id}/deactivate")
    public TransportCompanyResponse deactivate(@PathVariable UUID id) {
        return service.deactivate(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}