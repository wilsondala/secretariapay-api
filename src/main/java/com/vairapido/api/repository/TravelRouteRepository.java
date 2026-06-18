package com.vairapido.api.repository;

import com.vairapido.api.entity.TravelRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TravelRouteRepository extends JpaRepository<TravelRoute, UUID> {

    List<TravelRoute> findByOriginCityContainingIgnoreCase(String originCity);

    List<TravelRoute> findByDestinationCityContainingIgnoreCase(String destinationCity);

    boolean existsByOriginCityIgnoreCaseAndDestinationCityIgnoreCaseAndOriginTerminalIgnoreCaseAndDestinationTerminalIgnoreCase(
            String originCity,
            String destinationCity,
            String originTerminal,
            String destinationTerminal
    );
}