package com.secretariapay.api.service;

import com.secretariapay.api.dto.report.OperationalReportResponse;
import com.secretariapay.api.entity.enums.TicketAuditAction;
import com.secretariapay.api.entity.enums.TicketStatus;
import com.secretariapay.api.repository.TicketAuditLogRepository;
import com.secretariapay.api.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OperationalReportService {

    private final TicketRepository ticketRepository;
    private final TicketAuditLogRepository ticketAuditLogRepository;

    public OperationalReportService(
            TicketRepository ticketRepository,
            TicketAuditLogRepository ticketAuditLogRepository
    ) {
        this.ticketRepository = ticketRepository;
        this.ticketAuditLogRepository = ticketAuditLogRepository;
    }

    @Transactional(readOnly = true)
    public OperationalReportResponse getGlobalReport(
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        LocalDateTime resolvedStartAt = resolveStartAt(startAt);
        LocalDateTime resolvedEndAt = resolveEndAt(endAt);

        long totalTickets = ticketRepository.countByIssuedAtBetween(
                resolvedStartAt,
                resolvedEndAt
        );

        long validTickets = ticketRepository.countByStatusAndIssuedAtBetween(
                TicketStatus.VALID,
                resolvedStartAt,
                resolvedEndAt
        );

        long usedTickets = ticketRepository.countByStatusAndIssuedAtBetween(
                TicketStatus.USED,
                resolvedStartAt,
                resolvedEndAt
        );

        long cancelledTickets = ticketRepository.countByStatusAndIssuedAtBetween(
                TicketStatus.CANCELLED,
                resolvedStartAt,
                resolvedEndAt
        );

        long publicValidations = ticketAuditLogRepository.countByActionAndCreatedAtBetween(
                TicketAuditAction.PUBLIC_VALIDATION,
                resolvedStartAt,
                resolvedEndAt
        );

        long successfulPublicValidations = ticketAuditLogRepository.countByActionAndSuccessAndCreatedAtBetween(
                TicketAuditAction.PUBLIC_VALIDATION,
                true,
                resolvedStartAt,
                resolvedEndAt
        );

        long failedPublicValidations = ticketAuditLogRepository.countByActionAndSuccessAndCreatedAtBetween(
                TicketAuditAction.PUBLIC_VALIDATION,
                false,
                resolvedStartAt,
                resolvedEndAt
        );

        long boardingAttempts = ticketAuditLogRepository.countByActionAndCreatedAtBetween(
                TicketAuditAction.BOARDING,
                resolvedStartAt,
                resolvedEndAt
        );

        long successfulBoardings = ticketAuditLogRepository.countByActionAndSuccessAndCreatedAtBetween(
                TicketAuditAction.BOARDING,
                true,
                resolvedStartAt,
                resolvedEndAt
        );

        long failedBoardings = ticketAuditLogRepository.countByActionAndSuccessAndCreatedAtBetween(
                TicketAuditAction.BOARDING,
                false,
                resolvedStartAt,
                resolvedEndAt
        );

        return buildResponse(
                null,
                "GLOBAL",
                resolvedStartAt,
                resolvedEndAt,
                totalTickets,
                validTickets,
                usedTickets,
                cancelledTickets,
                publicValidations,
                successfulPublicValidations,
                failedPublicValidations,
                boardingAttempts,
                successfulBoardings,
                failedBoardings
        );
    }

    @Transactional(readOnly = true)
    public OperationalReportResponse getCompanyReport(
            UUID companyId,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        LocalDateTime resolvedStartAt = resolveStartAt(startAt);
        LocalDateTime resolvedEndAt = resolveEndAt(endAt);

        long totalTickets = ticketRepository.countByBooking_Trip_TransportCompany_IdAndIssuedAtBetween(
                companyId,
                resolvedStartAt,
                resolvedEndAt
        );

        long validTickets = ticketRepository.countByStatusAndBooking_Trip_TransportCompany_IdAndIssuedAtBetween(
                TicketStatus.VALID,
                companyId,
                resolvedStartAt,
                resolvedEndAt
        );

        long usedTickets = ticketRepository.countByStatusAndBooking_Trip_TransportCompany_IdAndIssuedAtBetween(
                TicketStatus.USED,
                companyId,
                resolvedStartAt,
                resolvedEndAt
        );

        long cancelledTickets = ticketRepository.countByStatusAndBooking_Trip_TransportCompany_IdAndIssuedAtBetween(
                TicketStatus.CANCELLED,
                companyId,
                resolvedStartAt,
                resolvedEndAt
        );

        long publicValidations = ticketAuditLogRepository
                .countByActionAndTicket_Booking_Trip_TransportCompany_IdAndCreatedAtBetween(
                        TicketAuditAction.PUBLIC_VALIDATION,
                        companyId,
                        resolvedStartAt,
                        resolvedEndAt
                );

        long successfulPublicValidations = ticketAuditLogRepository
                .countByActionAndSuccessAndTicket_Booking_Trip_TransportCompany_IdAndCreatedAtBetween(
                        TicketAuditAction.PUBLIC_VALIDATION,
                        true,
                        companyId,
                        resolvedStartAt,
                        resolvedEndAt
                );

        long failedPublicValidations = ticketAuditLogRepository
                .countByActionAndSuccessAndTicket_Booking_Trip_TransportCompany_IdAndCreatedAtBetween(
                        TicketAuditAction.PUBLIC_VALIDATION,
                        false,
                        companyId,
                        resolvedStartAt,
                        resolvedEndAt
                );

        long boardingAttempts = ticketAuditLogRepository
                .countByActionAndTicket_Booking_Trip_TransportCompany_IdAndCreatedAtBetween(
                        TicketAuditAction.BOARDING,
                        companyId,
                        resolvedStartAt,
                        resolvedEndAt
                );

        long successfulBoardings = ticketAuditLogRepository
                .countByActionAndSuccessAndTicket_Booking_Trip_TransportCompany_IdAndCreatedAtBetween(
                        TicketAuditAction.BOARDING,
                        true,
                        companyId,
                        resolvedStartAt,
                        resolvedEndAt
                );

        long failedBoardings = ticketAuditLogRepository
                .countByActionAndSuccessAndTicket_Booking_Trip_TransportCompany_IdAndCreatedAtBetween(
                        TicketAuditAction.BOARDING,
                        false,
                        companyId,
                        resolvedStartAt,
                        resolvedEndAt
                );

        return buildResponse(
                companyId,
                "COMPANY",
                resolvedStartAt,
                resolvedEndAt,
                totalTickets,
                validTickets,
                usedTickets,
                cancelledTickets,
                publicValidations,
                successfulPublicValidations,
                failedPublicValidations,
                boardingAttempts,
                successfulBoardings,
                failedBoardings
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

    private OperationalReportResponse buildResponse(
            UUID companyId,
            String scope,
            LocalDateTime periodStartAt,
            LocalDateTime periodEndAt,
            long totalTickets,
            long validTickets,
            long usedTickets,
            long cancelledTickets,
            long publicValidations,
            long successfulPublicValidations,
            long failedPublicValidations,
            long boardingAttempts,
            long successfulBoardings,
            long failedBoardings
    ) {
        long suspiciousAttempts = failedPublicValidations + failedBoardings;

        return new OperationalReportResponse()
                .setCompanyId(companyId)
                .setScope(scope)
                .setPeriodStartAt(periodStartAt)
                .setPeriodEndAt(periodEndAt)

                .setTotalTickets(totalTickets)
                .setValidTickets(validTickets)
                .setUsedTickets(usedTickets)
                .setCancelledTickets(cancelledTickets)

                .setPublicValidations(publicValidations)
                .setSuccessfulPublicValidations(successfulPublicValidations)
                .setFailedPublicValidations(failedPublicValidations)

                .setBoardingAttempts(boardingAttempts)
                .setSuccessfulBoardings(successfulBoardings)
                .setFailedBoardings(failedBoardings)

                .setSuspiciousAttempts(suspiciousAttempts)
                .setGeneratedAt(LocalDateTime.now());
    }
}
