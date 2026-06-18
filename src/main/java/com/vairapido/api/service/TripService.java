package com.vairapido.api.service;

import com.vairapido.api.dto.trip.TripRequest;
import com.vairapido.api.dto.trip.TripResponse;
import com.vairapido.api.entity.TransportCompany;
import com.vairapido.api.entity.TravelRoute;
import com.vairapido.api.entity.Trip;
import com.vairapido.api.entity.enums.CompanyStatus;
import com.vairapido.api.entity.enums.RouteStatus;
import com.vairapido.api.entity.enums.TripStatus;
import com.vairapido.api.exception.NotFoundException;
import com.vairapido.api.repository.TransportCompanyRepository;
import com.vairapido.api.repository.TravelRouteRepository;
import com.vairapido.api.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final TransportCompanyRepository companyRepository;
    private final TravelRouteRepository routeRepository;

    public TripService(
            TripRepository tripRepository,
            TransportCompanyRepository companyRepository,
            TravelRouteRepository routeRepository
    ) {
        this.tripRepository = tripRepository;
        this.companyRepository = companyRepository;
        this.routeRepository = routeRepository;
    }

    @Transactional
    public TripResponse create(TripRequest request) {
        validateDates(request.getDepartureAt(), request.getArrivalAt());

        TransportCompany company = companyRepository.findById(request.getTransportCompanyId())
                .orElseThrow(() -> new NotFoundException("Empresa de transporte não encontrada."));

        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new IllegalArgumentException("A empresa de transporte precisa estar ativa para criar uma viagem.");
        }

        TravelRoute route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> new NotFoundException("Rota não encontrada."));

        if (route.getStatus() != RouteStatus.ACTIVE) {
            throw new IllegalArgumentException("A rota precisa estar ativa para criar uma viagem.");
        }

        String currency = request.getCurrency();
        if (currency == null || currency.isBlank()) {
            currency = "BRL";
        }

        Trip trip = new Trip()
                .setTransportCompany(company)
                .setRoute(route)
                .setDepartureAt(request.getDepartureAt())
                .setArrivalAt(request.getArrivalAt())
                .setPrice(request.getPrice())
                .setCurrency(currency.toUpperCase())
                .setTotalSeats(request.getTotalSeats())
                .setAvailableSeats(request.getTotalSeats())
                .setBusPlate(request.getBusPlate())
                .setVehicleDescription(request.getVehicleDescription())
                .setStatus(TripStatus.SCHEDULED);

        return toResponse(tripRepository.save(trip));
    }

    @Transactional(readOnly = true)
    public List<TripResponse> findAll() {
        return tripRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TripResponse findById(UUID id) {
        return toResponse(findEntityById(id));
    }

    @Transactional
    public TripResponse update(UUID id, TripRequest request) {
        validateDates(request.getDepartureAt(), request.getArrivalAt());

        Trip trip = findEntityById(id);

        TransportCompany company = companyRepository.findById(request.getTransportCompanyId())
                .orElseThrow(() -> new NotFoundException("Empresa de transporte não encontrada."));

        TravelRoute route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> new NotFoundException("Rota não encontrada."));

        int oldTotalSeats = trip.getTotalSeats();
        int newTotalSeats = request.getTotalSeats();
        int soldOrReservedSeats = oldTotalSeats - trip.getAvailableSeats();

        if (newTotalSeats < soldOrReservedSeats) {
            throw new IllegalArgumentException("O total de assentos não pode ser menor que a quantidade já reservada/vendida.");
        }

        int newAvailableSeats = newTotalSeats - soldOrReservedSeats;

        String currency = request.getCurrency();
        if (currency == null || currency.isBlank()) {
            currency = "BRL";
        }

        trip
                .setTransportCompany(company)
                .setRoute(route)
                .setDepartureAt(request.getDepartureAt())
                .setArrivalAt(request.getArrivalAt())
                .setPrice(request.getPrice())
                .setCurrency(currency.toUpperCase())
                .setTotalSeats(newTotalSeats)
                .setAvailableSeats(newAvailableSeats)
                .setBusPlate(request.getBusPlate())
                .setVehicleDescription(request.getVehicleDescription());

        return toResponse(tripRepository.save(trip));
    }

    @Transactional
    public TripResponse activate(UUID id) {
        Trip trip = findEntityById(id);
        trip.setStatus(TripStatus.SCHEDULED);

        return toResponse(tripRepository.save(trip));
    }

    @Transactional
    public TripResponse cancel(UUID id) {
        Trip trip = findEntityById(id);
        trip.setStatus(TripStatus.CANCELLED);

        return toResponse(tripRepository.save(trip));
    }

    @Transactional
    public TripResponse complete(UUID id) {
        Trip trip = findEntityById(id);
        trip.setStatus(TripStatus.COMPLETED);

        return toResponse(tripRepository.save(trip));
    }

    @Transactional
    public void delete(UUID id) {
        Trip trip = findEntityById(id);

        if (!trip.getAvailableSeats().equals(trip.getTotalSeats())) {
            throw new IllegalArgumentException("Não é possível excluir uma viagem que já possui reservas ou vendas.");
        }

        tripRepository.delete(trip);
    }

    private void validateDates(LocalDateTime departureAt, LocalDateTime arrivalAt) {
        if (arrivalAt.isBefore(departureAt) || arrivalAt.isEqual(departureAt)) {
            throw new IllegalArgumentException("A data/hora de chegada deve ser posterior à data/hora de saída.");
        }
    }

    private Trip findEntityById(UUID id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Viagem não encontrada."));
    }

    private TripResponse toResponse(Trip trip) {
        TransportCompany company = trip.getTransportCompany();
        TravelRoute route = trip.getRoute();

        return new TripResponse()
                .setId(trip.getId())

                .setTransportCompanyId(company.getId())
                .setTransportCompanyName(company.getName())
                .setTransportCompanyTradeName(company.getTradeName())

                .setRouteId(route.getId())
                .setOriginCity(route.getOriginCity())
                .setOriginState(route.getOriginState())
                .setOriginTerminal(route.getOriginTerminal())
                .setDestinationCity(route.getDestinationCity())
                .setDestinationState(route.getDestinationState())
                .setDestinationTerminal(route.getDestinationTerminal())

                .setDepartureAt(trip.getDepartureAt())
                .setArrivalAt(trip.getArrivalAt())
                .setPrice(trip.getPrice())
                .setCurrency(trip.getCurrency())
                .setTotalSeats(trip.getTotalSeats())
                .setAvailableSeats(trip.getAvailableSeats())
                .setBusPlate(trip.getBusPlate())
                .setVehicleDescription(trip.getVehicleDescription())
                .setStatus(trip.getStatus())
                .setCreatedAt(trip.getCreatedAt())
                .setUpdatedAt(trip.getUpdatedAt());
    }
}