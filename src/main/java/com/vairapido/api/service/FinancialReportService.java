package com.vairapido.api.service;

import com.vairapido.api.dto.report.FinancialReportResponse;
import com.vairapido.api.entity.enums.BookingStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class FinancialReportService {

    private final EntityManager entityManager;

    public FinancialReportService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public FinancialReportResponse getGlobalReport(
            LocalDateTime startAt,
            LocalDateTime endAt,
            String currency
    ) {
        LocalDateTime resolvedStartAt = resolveStartAt(startAt);
        LocalDateTime resolvedEndAt = resolveEndAt(endAt);
        String resolvedCurrency = resolveCurrency(currency);

        long paidBookings = countPaidBookings(
                resolvedStartAt,
                resolvedEndAt,
                resolvedCurrency,
                null
        );

        long pendingBookings = countBookingsByStatus(
                BookingStatus.PENDING_PAYMENT,
                resolvedStartAt,
                resolvedEndAt,
                resolvedCurrency,
                null
        );

        long cancelledBookings = countBookingsByStatus(
                BookingStatus.CANCELLED,
                resolvedStartAt,
                resolvedEndAt,
                resolvedCurrency,
                null
        );

        long issuedTickets = countIssuedTickets(
                resolvedStartAt,
                resolvedEndAt,
                null
        );

        BigDecimal totalRevenue = sumPaidBookingsAmount(
                resolvedStartAt,
                resolvedEndAt,
                resolvedCurrency,
                null
        );

        BigDecimal averageTicketAmount = calculateAverage(
                totalRevenue,
                paidBookings
        );

        return buildResponse(
                null,
                "GLOBAL",
                resolvedCurrency,
                resolvedStartAt,
                resolvedEndAt,
                paidBookings,
                pendingBookings,
                cancelledBookings,
                issuedTickets,
                totalRevenue,
                averageTicketAmount
        );
    }

    @Transactional(readOnly = true)
    public FinancialReportResponse getCompanyReport(
            UUID companyId,
            LocalDateTime startAt,
            LocalDateTime endAt,
            String currency
    ) {
        LocalDateTime resolvedStartAt = resolveStartAt(startAt);
        LocalDateTime resolvedEndAt = resolveEndAt(endAt);
        String resolvedCurrency = resolveCurrency(currency);

        long paidBookings = countPaidBookings(
                resolvedStartAt,
                resolvedEndAt,
                resolvedCurrency,
                companyId
        );

        long pendingBookings = countBookingsByStatus(
                BookingStatus.PENDING_PAYMENT,
                resolvedStartAt,
                resolvedEndAt,
                resolvedCurrency,
                companyId
        );

        long cancelledBookings = countBookingsByStatus(
                BookingStatus.CANCELLED,
                resolvedStartAt,
                resolvedEndAt,
                resolvedCurrency,
                companyId
        );

        long issuedTickets = countIssuedTickets(
                resolvedStartAt,
                resolvedEndAt,
                companyId
        );

        BigDecimal totalRevenue = sumPaidBookingsAmount(
                resolvedStartAt,
                resolvedEndAt,
                resolvedCurrency,
                companyId
        );

        BigDecimal averageTicketAmount = calculateAverage(
                totalRevenue,
                paidBookings
        );

        return buildResponse(
                companyId,
                "COMPANY",
                resolvedCurrency,
                resolvedStartAt,
                resolvedEndAt,
                paidBookings,
                pendingBookings,
                cancelledBookings,
                issuedTickets,
                totalRevenue,
                averageTicketAmount
        );
    }

    private long countPaidBookings(
            LocalDateTime startAt,
            LocalDateTime endAt,
            String currency,
            UUID companyId
    ) {
        String jpql = """
                SELECT COUNT(b)
                FROM Booking b
                WHERE b.status IN :statuses
                  AND b.currency = :currency
                  AND b.paidAt BETWEEN :startAt AND :endAt
                """;

        if (companyId != null) {
            jpql += " AND b.trip.transportCompany.id = :companyId";
        }

        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class)
                .setParameter("statuses", List.of(
                        BookingStatus.PAID,
                        BookingStatus.TICKET_ISSUED
                ))
                .setParameter("currency", currency)
                .setParameter("startAt", startAt)
                .setParameter("endAt", endAt);

        if (companyId != null) {
            query.setParameter("companyId", companyId);
        }

        return query.getSingleResult();
    }

    private long countBookingsByStatus(
            BookingStatus status,
            LocalDateTime startAt,
            LocalDateTime endAt,
            String currency,
            UUID companyId
    ) {
        String jpql = """
                SELECT COUNT(b)
                FROM Booking b
                WHERE b.status = :status
                  AND b.currency = :currency
                  AND b.createdAt BETWEEN :startAt AND :endAt
                """;

        if (companyId != null) {
            jpql += " AND b.trip.transportCompany.id = :companyId";
        }

        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class)
                .setParameter("status", status)
                .setParameter("currency", currency)
                .setParameter("startAt", startAt)
                .setParameter("endAt", endAt);

        if (companyId != null) {
            query.setParameter("companyId", companyId);
        }

        return query.getSingleResult();
    }

    private long countIssuedTickets(
            LocalDateTime startAt,
            LocalDateTime endAt,
            UUID companyId
    ) {
        String jpql = """
                SELECT COUNT(t)
                FROM Ticket t
                WHERE t.issuedAt BETWEEN :startAt AND :endAt
                """;

        if (companyId != null) {
            jpql += " AND t.booking.trip.transportCompany.id = :companyId";
        }

        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class)
                .setParameter("startAt", startAt)
                .setParameter("endAt", endAt);

        if (companyId != null) {
            query.setParameter("companyId", companyId);
        }

        return query.getSingleResult();
    }

    private BigDecimal sumPaidBookingsAmount(
            LocalDateTime startAt,
            LocalDateTime endAt,
            String currency,
            UUID companyId
    ) {
        String jpql = """
                SELECT SUM(b.amount)
                FROM Booking b
                WHERE b.status IN :statuses
                  AND b.currency = :currency
                  AND b.paidAt BETWEEN :startAt AND :endAt
                """;

        if (companyId != null) {
            jpql += " AND b.trip.transportCompany.id = :companyId";
        }

        TypedQuery<BigDecimal> query = entityManager.createQuery(jpql, BigDecimal.class)
                .setParameter("statuses", List.of(
                        BookingStatus.PAID,
                        BookingStatus.TICKET_ISSUED
                ))
                .setParameter("currency", currency)
                .setParameter("startAt", startAt)
                .setParameter("endAt", endAt);

        if (companyId != null) {
            query.setParameter("companyId", companyId);
        }

        BigDecimal result = query.getSingleResult();

        if (result == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return result.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAverage(
            BigDecimal totalRevenue,
            long paidBookings
    ) {
        if (paidBookings <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return totalRevenue.divide(
                BigDecimal.valueOf(paidBookings),
                2,
                RoundingMode.HALF_UP
        );
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

    private FinancialReportResponse buildResponse(
            UUID companyId,
            String scope,
            String currency,
            LocalDateTime periodStartAt,
            LocalDateTime periodEndAt,
            long paidBookings,
            long pendingBookings,
            long cancelledBookings,
            long issuedTickets,
            BigDecimal totalRevenue,
            BigDecimal averageTicketAmount
    ) {
        return new FinancialReportResponse()
                .setCompanyId(companyId)
                .setScope(scope)
                .setCurrency(currency)
                .setPeriodStartAt(periodStartAt)
                .setPeriodEndAt(periodEndAt)
                .setPaidBookings(paidBookings)
                .setPendingBookings(pendingBookings)
                .setCancelledBookings(cancelledBookings)
                .setIssuedTickets(issuedTickets)
                .setTotalRevenue(totalRevenue)
                .setAverageTicketAmount(averageTicketAmount)
                .setGeneratedAt(LocalDateTime.now());
    }
}