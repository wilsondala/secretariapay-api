package com.vairapido.api.repository;

import com.vairapido.api.entity.Trip;
import com.vairapido.api.entity.enums.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TripRepository extends JpaRepository<Trip, UUID> {

    List<Trip> findByStatus(TripStatus status);

    List<Trip> findByRoute_Id(UUID routeId);

    List<Trip> findByTransportCompany_Id(UUID transportCompanyId);

    List<Trip> findByDepartureAtBetween(LocalDateTime start, LocalDateTime end);
}