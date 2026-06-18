package com.vairapido.api.controller;

import com.vairapido.api.dto.trip.TripRequest;
import com.vairapido.api.dto.trip.TripResponse;
import com.vairapido.api.service.TripService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips")
public class TripController {

    private final TripService service;

    public TripController(TripService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TripResponse create(@Valid @RequestBody TripRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<TripResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public TripResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public TripResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody TripRequest request
    ) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/activate")
    public TripResponse activate(@PathVariable UUID id) {
        return service.activate(id);
    }

    @PatchMapping("/{id}/cancel")
    public TripResponse cancel(@PathVariable UUID id) {
        return service.cancel(id);
    }

    @PatchMapping("/{id}/complete")
    public TripResponse complete(@PathVariable UUID id) {
        return service.complete(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}