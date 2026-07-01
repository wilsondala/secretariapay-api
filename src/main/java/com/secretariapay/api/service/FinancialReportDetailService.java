package com.secretariapay.api.service;

import com.secretariapay.api.dto.report.FinancialBookingReportItemResponse;
import com.secretariapay.api.entity.Booking;
import com.secretariapay.api.entity.Passenger;
import com.secretariapay.api.entity.Ticket;
import com.secretariapay.api.entity.TransportCompany;
import com.secretariapay.api.entity.TravelRoute;
import com.secretariapay.api.entity.Trip;
import com.secretariapay.api.entity.enums.BookingStatus;
import com.secretariapay.api.repository.TicketRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FinancialReportDetailService {

    private final EntityManager entityManager;
    private final TicketRepository ticketRepository;

    public FinancialReportDetailService(
            EntityManager entityManager,
            TicketRepository ticketRepository
    ) {
        this.entityManager = entityManager;
        this.ticketRepository = ticketRepository;
    }

    @Transactional(readOnly = true)
    public List<FinancialBookingReportItemResponse> findGlobalBookings(
            LocalDateTime startAt,
            LocalDateTime endAt,
            String currency
    ) {
        LocalDateTime resolvedStartAt = resolveStartAt(startAt);
        LocalDateTime resolvedEndAt = resolveEndAt(endAt);
        String resolvedCurrency = resolveCurrency(currency);

        return findBookings(
                resolvedStartAt,
                resolvedEndAt,
                resolvedCurrency,
                null
        )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FinancialBookingReportItemResponse> findCompanyBookings(
            UUID companyId,
            LocalDateTime startAt,
            LocalDateTime endAt,
            String currency
    ) {
        LocalDateTime resolvedStartAt = resolveStartAt(startAt);
        LocalDateTime resolvedEndAt = resolveEndAt(endAt);
        String resolvedCurrency = resolveCurrency(currency);

        return findBookings(
                resolvedStartAt,
                resolvedEndAt,
                resolvedCurrency,
                companyId
        )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private List<Booking> findBookings(
            LocalDateTime startAt,
            LocalDateTime endAt,
            String currency,
            UUID companyId
    ) {
        String jpql = """
                SELECT b
                FROM Booking b
                WHERE b.currency = :currency
                  AND (
                        (
                            b.status IN :paidStatuses
                            AND b.paidAt BETWEEN :startAt AND :endAt
                        )
                        OR (
                            b.status IN :createdAtStatuses
                            AND b.createdAt BETWEEN :startAt AND :endAt
                        )
                  )
                """;

        if (companyId != null) {
            jpql += " AND b.trip.transportCompany.id = :companyId";
        }

        jpql += " ORDER BY b.createdAt DESC";

        TypedQuery<Booking> query = entityManager.createQuery(jpql, Booking.class)
                .setParameter("currency", currency)
                .setParameter("paidStatuses", List.of(
                        BookingStatus.PAID,
                        BookingStatus.TICKET_ISSUED
                ))
                .setParameter("createdAtStatuses", List.of(
                        BookingStatus.PENDING_PAYMENT,
                        BookingStatus.CANCELLED,
                        BookingStatus.EXPIRED
                ))
                .setParameter("startAt", startAt)
                .setParameter("endAt", endAt);

        if (companyId != null) {
            query.setParameter("companyId", companyId);
        }

        return query.getResultList();
    }

    private FinancialBookingReportItemResponse toResponse(Booking booking) {
        Trip trip = booking.getTrip();
        Passenger passenger = booking.getPassenger();
        TransportCompany company = trip.getTransportCompany();
        TravelRoute route = trip.getRoute();

        Optional<Ticket> optionalTicket =
                ticketRepository.findByBooking_Id(booking.getId());

        FinancialBookingReportItemResponse response =
                new FinancialBookingReportItemResponse()
                        .setBookingId(booking.getId())
                        .setBookingCode(booking.getBookingCode())
                        .setBookingStatus(booking.getStatus())

                        .setCompanyId(company.getId())
                        .setCompanyName(company.getName())
                        .setCompanyTradeName(company.getTradeName())

                        .setPassengerName(passenger.getFullName())
                        .setPassengerDocument(passenger.getDocumentNumber())
                        .setPassengerWhatsapp(passenger.getWhatsapp())

                        .setOriginCity(route.getOriginCity())
                        .setOriginState(route.getOriginState())
                        .setOriginTerminal(route.getOriginTerminal())

                        .setDestinationCity(route.getDestinationCity())
                        .setDestinationState(route.getDestinationState())
                        .setDestinationTerminal(route.getDestinationTerminal())

                        .setDepartureAt(trip.getDepartureAt())
                        .setArrivalAt(trip.getArrivalAt())

                        .setSeatNumber(booking.getSeatNumber())

                        .setAmount(booking.getAmount())
                        .setCurrency(booking.getCurrency())

                        .setExpiresAt(booking.getExpiresAt())
                        .setPaidAt(booking.getPaidAt())
                        .setCancelledAt(booking.getCancelledAt())
                        .setCreatedAt(booking.getCreatedAt())
                        .setUpdatedAt(booking.getUpdatedAt());

        optionalTicket.ifPresent(ticket -> response
                .setTicketId(ticket.getId())
                .setTicketCode(ticket.getTicketCode())
                .setTicketStatus(ticket.getStatus())
        );

        return response;
    }

    private LocalDateTime resolveStartAt(LocalDateTime startAt) {
        if (startAt != null) {
            return startAt;
        }

        return LocalDateTime.of(1970, 1, 1, 0, 0);
    }

    private LocalDateTime resolveEndAt(LocalDateTime endAt) {
        if (endAt != null) {
            return endAt;
        }

        return LocalDateTime.now().plusDays(1);
    }

    private String resolveCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "BRL";
        }

        return currency.trim().toUpperCase();
    }
}
