package com.vairapido.api.controller;

import com.vairapido.api.dto.trip.TripRequest;
import com.vairapido.api.dto.trip.TripResponse;
import com.vairapido.api.service.TripService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN') or @companyAccessService.canAccessCompany(#p0.transportCompanyId)")
    public TripResponse create(@Valid @RequestBody TripRequest request) {
        return service.create(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public List<TripResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/company/{companyId}")
    @PreAuthorize("@companyAccessService.canAccessCompany(#p0)")
    public List<TripResponse> findByCompanyId(@PathVariable UUID companyId) {
        return service.findByCompanyId(companyId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@companyAccessService.canAccessTrip(#p0)")
    public TripResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN') or (@companyAccessService.canAccessTrip(#p0) and @companyAccessService.canAccessCompany(#p1.transportCompanyId))")
    public TripResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody TripRequest request
    ) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("@companyAccessService.canAccessTrip(#p0)")
    public TripResponse activate(@PathVariable UUID id) {
        return service.activate(id);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("@companyAccessService.canAccessTrip(#p0)")
    public TripResponse cancel(@PathVariable UUID id) {
        return service.cancel(id);
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("@companyAccessService.canAccessTrip(#p0)")
    public TripResponse complete(@PathVariable UUID id) {
        return service.complete(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@companyAccessService.canAccessTrip(#p0)")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}