package com.vairapido.api.controller;

import com.vairapido.api.dto.route.RouteRequest;
import com.vairapido.api.dto.route.RouteResponse;
import com.vairapido.api.service.TravelRouteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/routes")
public class TravelRouteController {

    private final TravelRouteService service;

    public TravelRouteController(TravelRouteService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RouteResponse create(@Valid @RequestBody RouteRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<RouteResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public RouteResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public RouteResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody RouteRequest request
    ) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/activate")
    public RouteResponse activate(@PathVariable UUID id) {
        return service.activate(id);
    }

    @PatchMapping("/{id}/deactivate")
    public RouteResponse deactivate(@PathVariable UUID id) {
        return service.deactivate(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}