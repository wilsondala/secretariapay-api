package com.vairapido.api.service;

import com.vairapido.api.dto.route.RouteRequest;
import com.vairapido.api.dto.route.RouteResponse;
import com.vairapido.api.entity.TravelRoute;
import com.vairapido.api.entity.enums.RouteStatus;
import com.vairapido.api.exception.NotFoundException;
import com.vairapido.api.repository.TravelRouteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TravelRouteService {

    private final TravelRouteRepository repository;

    public TravelRouteService(TravelRouteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public RouteResponse create(RouteRequest request) {
        if (isDuplicate(request)) {
            throw new IllegalArgumentException("Já existe uma rota cadastrada com esta origem e destino.");
        }

        TravelRoute route = new TravelRoute()
                .setOriginCity(request.getOriginCity())
                .setOriginState(request.getOriginState())
                .setOriginTerminal(request.getOriginTerminal())
                .setDestinationCity(request.getDestinationCity())
                .setDestinationState(request.getDestinationState())
                .setDestinationTerminal(request.getDestinationTerminal())
                .setDistanceKm(request.getDistanceKm())
                .setEstimatedDurationMinutes(request.getEstimatedDurationMinutes())
                .setStatus(RouteStatus.ACTIVE);

        TravelRoute savedRoute = repository.save(route);
        return toResponse(savedRoute);
    }

    @Transactional(readOnly = true)
    public List<RouteResponse> findAll() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RouteResponse findById(UUID id) {
        TravelRoute route = findEntityById(id);
        return toResponse(route);
    }

    @Transactional
    public RouteResponse update(UUID id, RouteRequest request) {
        TravelRoute route = findEntityById(id);

        route
                .setOriginCity(request.getOriginCity())
                .setOriginState(request.getOriginState())
                .setOriginTerminal(request.getOriginTerminal())
                .setDestinationCity(request.getDestinationCity())
                .setDestinationState(request.getDestinationState())
                .setDestinationTerminal(request.getDestinationTerminal())
                .setDistanceKm(request.getDistanceKm())
                .setEstimatedDurationMinutes(request.getEstimatedDurationMinutes());

        TravelRoute updatedRoute = repository.save(route);
        return toResponse(updatedRoute);
    }

    @Transactional
    public RouteResponse activate(UUID id) {
        TravelRoute route = findEntityById(id);
        route.setStatus(RouteStatus.ACTIVE);

        return toResponse(repository.save(route));
    }

    @Transactional
    public RouteResponse deactivate(UUID id) {
        TravelRoute route = findEntityById(id);
        route.setStatus(RouteStatus.INACTIVE);

        return toResponse(repository.save(route));
    }

    @Transactional
    public void delete(UUID id) {
        TravelRoute route = findEntityById(id);
        repository.delete(route);
    }

    private boolean isDuplicate(RouteRequest request) {
        return repository.existsByOriginCityIgnoreCaseAndDestinationCityIgnoreCaseAndOriginTerminalIgnoreCaseAndDestinationTerminalIgnoreCase(
                request.getOriginCity(),
                request.getDestinationCity(),
                normalizeNullable(request.getOriginTerminal()),
                normalizeNullable(request.getDestinationTerminal())
        );
    }

    private String normalizeNullable(String value) {
        return value == null ? "" : value;
    }

    private TravelRoute findEntityById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Rota não encontrada."));
    }

    private RouteResponse toResponse(TravelRoute route) {
        return new RouteResponse()
                .setId(route.getId())
                .setOriginCity(route.getOriginCity())
                .setOriginState(route.getOriginState())
                .setOriginTerminal(route.getOriginTerminal())
                .setDestinationCity(route.getDestinationCity())
                .setDestinationState(route.getDestinationState())
                .setDestinationTerminal(route.getDestinationTerminal())
                .setDistanceKm(route.getDistanceKm())
                .setEstimatedDurationMinutes(route.getEstimatedDurationMinutes())
                .setStatus(route.getStatus())
                .setCreatedAt(route.getCreatedAt())
                .setUpdatedAt(route.getUpdatedAt());
    }
}