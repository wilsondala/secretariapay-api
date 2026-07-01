package com.secretariapay.api.service;

import com.secretariapay.api.dto.report.TripReportResponse;
import com.secretariapay.api.dto.report.TripTicketReportItemResponse;
import com.secretariapay.api.entity.Booking;
import com.secretariapay.api.entity.Passenger;
import com.secretariapay.api.entity.Ticket;
import com.secretariapay.api.entity.TransportCompany;
import com.secretariapay.api.entity.TravelRoute;
import com.secretariapay.api.entity.Trip;
import com.secretariapay.api.entity.enums.BookingStatus;
import com.secretariapay.api.entity.enums.TicketStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TripReportService {

    private final EntityManager entityManager;

    public TripReportService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public TripReportResponse getTripReport(UUID tripId, String currency) {
        Trip trip = findTripOrThrow(tripId);
        String resolvedCurrency = resolveCurrency(currency);

        long totalBookings = countBookings(tripId, resolvedCurrency);

        long paidBookings = countBookingsByStatuses(
                tripId,
                resolvedCurrency,
                List.of(BookingStatus.PAID, BookingStatus.TICKET_ISSUED)
        );

        long pendingBookings = countBookingsByStatuses(
                tripId,
                resolvedCurrency,
                List.of(BookingStatus.PENDING_PAYMENT)
        );

        long cancelledBookings = countBookingsByStatuses(
                tripId,
                resolvedCurrency,
                List.of(BookingStatus.CANCELLED)
        );

        long expiredBookings = countBookingsByStatuses(
                tripId,
                resolvedCurrency,
                List.of(BookingStatus.EXPIRED)
        );

        long issuedTickets = countTickets(tripId);
        long validTickets = countTicketsByStatus(tripId, TicketStatus.VALID);
        long usedTickets = countTicketsByStatus(tripId, TicketStatus.USED);
        long cancelledTickets = countTicketsByStatus(tripId, TicketStatus.CANCELLED);

        BigDecimal totalRevenue = sumRevenue(tripId, resolvedCurrency);
        BigDecimal averageTicketAmount = calculateAverage(totalRevenue, paidBookings);

        Integer totalSeats = trip.getTotalSeats();
        Integer availableSeats = trip.getAvailableSeats();
        Integer occupiedSeats = calculateOccupiedSeats(totalSeats, availableSeats);

        BigDecimal occupancyRatePercentage =
                calculatePercentage(occupiedSeats, totalSeats);

        BigDecimal checkInRatePercentage =
                calculatePercentage(usedTickets, issuedTickets);

        TransportCompany company = trip.getTransportCompany();
        TravelRoute route = trip.getRoute();

        return new TripReportResponse()
                .setTripId(trip.getId())

                .setCompanyId(company.getId())
                .setCompanyName(company.getName())
                .setCompanyTradeName(company.getTradeName())

                .setOriginCity(route.getOriginCity())
                .setOriginState(route.getOriginState())
                .setOriginTerminal(route.getOriginTerminal())

                .setDestinationCity(route.getDestinationCity())
                .setDestinationState(route.getDestinationState())
                .setDestinationTerminal(route.getDestinationTerminal())

                .setDepartureAt(trip.getDepartureAt())
                .setArrivalAt(trip.getArrivalAt())

                .setTotalSeats(totalSeats)
                .setAvailableSeats(availableSeats)
                .setOccupiedSeats(occupiedSeats)

                .setCurrency(resolvedCurrency)

                .setTotalBookings(totalBookings)
                .setPaidBookings(paidBookings)
                .setPendingBookings(pendingBookings)
                .setCancelledBookings(cancelledBookings)
                .setExpiredBookings(expiredBookings)

                .setIssuedTickets(issuedTickets)
                .setValidTickets(validTickets)
                .setUsedTickets(usedTickets)
                .setCancelledTickets(cancelledTickets)

                .setTotalRevenue(totalRevenue)
                .setAverageTicketAmount(averageTicketAmount)
                .setOccupancyRatePercentage(occupancyRatePercentage)
                .setCheckInRatePercentage(checkInRatePercentage)

                .setGeneratedAt(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public TripReportResponse getTripFinancialReport(UUID tripId, String currency) {
        return getTripReport(tripId, currency);
    }

    @Transactional(readOnly = true)
    public List<TripTicketReportItemResponse> findTripTickets(UUID tripId) {
        findTripOrThrow(tripId);

        String jpql = """
                SELECT t
                FROM Ticket t
                WHERE t.booking.trip.id = :tripId
                ORDER BY t.issuedAt DESC
                """;

        return entityManager.createQuery(jpql, Ticket.class)
                .setParameter("tripId", tripId)
                .getResultList()
                .stream()
                .map(this::toTicketItemResponse)
                .toList();
    }

    private Trip findTripOrThrow(UUID tripId) {
        Trip trip = entityManager.find(Trip.class, tripId);

        if (trip == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Viagem não encontrada."
            );
        }

        return trip;
    }

    private long countBookings(UUID tripId, String currency) {
        String jpql = """
                SELECT COUNT(b)
                FROM Booking b
                WHERE b.trip.id = :tripId
                  AND b.currency = :currency
                """;

        return entityManager.createQuery(jpql, Long.class)
                .setParameter("tripId", tripId)
                .setParameter("currency", currency)
                .getSingleResult();
    }

    private long countBookingsByStatuses(
            UUID tripId,
            String currency,
            List<BookingStatus> statuses
    ) {
        String jpql = """
                SELECT COUNT(b)
                FROM Booking b
                WHERE b.trip.id = :tripId
                  AND b.currency = :currency
                  AND b.status IN :statuses
                """;

        return entityManager.createQuery(jpql, Long.class)
                .setParameter("tripId", tripId)
                .setParameter("currency", currency)
                .setParameter("statuses", statuses)
                .getSingleResult();
    }

    private long countTickets(UUID tripId) {
        String jpql = """
                SELECT COUNT(t)
                FROM Ticket t
                WHERE t.booking.trip.id = :tripId
                """;

        return entityManager.createQuery(jpql, Long.class)
                .setParameter("tripId", tripId)
                .getSingleResult();
    }

    private long countTicketsByStatus(UUID tripId, TicketStatus status) {
        String jpql = """
                SELECT COUNT(t)
                FROM Ticket t
                WHERE t.booking.trip.id = :tripId
                  AND t.status = :status
                """;

        return entityManager.createQuery(jpql, Long.class)
                .setParameter("tripId", tripId)
                .setParameter("status", status)
                .getSingleResult();
    }

    private BigDecimal sumRevenue(UUID tripId, String currency) {
        String jpql = """
                SELECT SUM(b.amount)
                FROM Booking b
                WHERE b.trip.id = :tripId
                  AND b.currency = :currency
                  AND b.status IN :statuses
                """;

        TypedQuery<BigDecimal> query = entityManager.createQuery(jpql, BigDecimal.class)
                .setParameter("tripId", tripId)
                .setParameter("currency", currency)
                .setParameter("statuses", List.of(
                        BookingStatus.PAID,
                        BookingStatus.TICKET_ISSUED
                ));

        BigDecimal result = query.getSingleResult();

        if (result == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return result.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAverage(BigDecimal totalRevenue, long paidBookings) {
        if (paidBookings <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return totalRevenue.divide(
                BigDecimal.valueOf(paidBookings),
                2,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal calculatePercentage(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        BigDecimal.valueOf(denominator),
                        2,
                        RoundingMode.HALF_UP
                );
    }

    private Integer calculateOccupiedSeats(Integer totalSeats, Integer availableSeats) {
        if (totalSeats == null || availableSeats == null) {
            return 0;
        }

        int occupied = totalSeats - availableSeats;

        return Math.max(occupied, 0);
    }

    private TripTicketReportItemResponse toTicketItemResponse(Ticket ticket) {
        Booking booking = ticket.getBooking();
        Passenger passenger = booking.getPassenger();

        return new TripTicketReportItemResponse()
                .setTicketId(ticket.getId())
                .setTicketCode(ticket.getTicketCode())
                .setTicketStatus(ticket.getStatus())

                .setBookingId(booking.getId())
                .setBookingCode(booking.getBookingCode())
                .setBookingStatus(booking.getStatus())

                .setPassengerName(passenger.getFullName())
                .setPassengerDocument(passenger.getDocumentNumber())
                .setPassengerWhatsapp(passenger.getWhatsapp())

                .setSeatNumber(booking.getSeatNumber())

                .setAmount(booking.getAmount())
                .setCurrency(booking.getCurrency())

                .setIssuedAt(ticket.getIssuedAt())
                .setUsedAt(ticket.getUsedAt())
                .setCancelledAt(ticket.getCancelledAt());
    }

    private String resolveCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "BRL";
        }

        return currency.trim().toUpperCase();
    }
}
