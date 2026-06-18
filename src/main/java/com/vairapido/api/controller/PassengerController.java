package com.vairapido.api.controller;

import com.vairapido.api.dto.passenger.PassengerRequest;
import com.vairapido.api.dto.passenger.PassengerResponse;
import com.vairapido.api.service.PassengerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/passengers")
public class PassengerController {

    private final PassengerService service;

    public PassengerController(PassengerService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PassengerResponse create(@Valid @RequestBody PassengerRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<PassengerResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public PassengerResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public PassengerResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody PassengerRequest request
    ) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}