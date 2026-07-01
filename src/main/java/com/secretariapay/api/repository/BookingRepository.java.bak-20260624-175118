package com.vairapido.api.repository;

import com.vairapido.api.entity.Booking;
import com.vairapido.api.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByBookingCode(String bookingCode);

    List<Booking> findByTrip_Id(UUID tripId);

    List<Booking> findByPassenger_Id(UUID passengerId);

    List<Booking> findByTrip_TransportCompany_Id(UUID transportCompanyId);

    List<Booking> findByStatusAndExpiresAtBefore(
            BookingStatus status,
            LocalDateTime expiresAt
    );

    long countByStatus(BookingStatus status);

    long countByTrip_TransportCompany_Id(UUID transportCompanyId);

    long countByTrip_TransportCompany_IdAndStatus(
            UUID transportCompanyId,
            BookingStatus status
    );

    @Query("""
            SELECT COUNT(DISTINCT b.passenger.id)
            FROM Booking b
            WHERE b.trip.transportCompany.id = :companyId
            """)
    long countDistinctPassengersByTransportCompanyId(
            @Param("companyId") UUID companyId
    );

    boolean existsByTrip_IdAndSeatNumberAndStatusIn(
            UUID tripId,
            Integer seatNumber,
            Collection<BookingStatus> statuses
    );
}