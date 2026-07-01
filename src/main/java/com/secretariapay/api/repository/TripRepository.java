package com.secretariapay.api.repository;

import com.secretariapay.api.entity.Trip;
import com.secretariapay.api.entity.enums.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TripRepository extends JpaRepository<Trip, UUID> {

    List<Trip> findByStatus(TripStatus status);

    List<Trip> findByRoute_Id(UUID routeId);

    List<Trip> findByTransportCompany_Id(UUID transportCompanyId);

    List<Trip> findByDepartureAtBetween(LocalDateTime start, LocalDateTime end);

    long countByTransportCompany_Id(UUID transportCompanyId);

    @Query("""
            SELECT COUNT(DISTINCT t.route.id)
            FROM Trip t
            WHERE t.transportCompany.id = :companyId
            """)
    long countDistinctRoutesByTransportCompanyId(
            @Param("companyId") UUID companyId
    );

    @Query("""
            SELECT t
            FROM Trip t
            JOIN FETCH t.route r
            JOIN FETCH t.transportCompany c
            WHERE t.status = :status
              AND t.departureAt >= :startDateTime
              AND t.departureAt < :endDateTime
              AND t.availableSeats > 0
              AND (
                    LOWER(r.originCity) LIKE LOWER(CONCAT('%', :origin, '%'))
                    OR LOWER(COALESCE(r.originState, '')) LIKE LOWER(CONCAT('%', :origin, '%'))
                  )
              AND (
                    LOWER(r.destinationCity) LIKE LOWER(CONCAT('%', :destination, '%'))
                    OR LOWER(COALESCE(r.destinationState, '')) LIKE LOWER(CONCAT('%', :destination, '%'))
                  )
            ORDER BY t.departureAt ASC
            """)
    List<Trip> searchAvailableTrips(
            @Param("origin") String origin,
            @Param("destination") String destination,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("status") TripStatus status
    );
}
