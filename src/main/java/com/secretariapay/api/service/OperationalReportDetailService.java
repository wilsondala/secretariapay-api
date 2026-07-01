package com.secretariapay.api.service;

import com.secretariapay.api.dto.report.OperationalTicketReportItemResponse;
import com.secretariapay.api.dto.ticketaudit.TicketAuditLogResponse;
import com.secretariapay.api.entity.Booking;
import com.secretariapay.api.entity.Passenger;
import com.secretariapay.api.entity.Ticket;
import com.secretariapay.api.entity.TicketAuditLog;
import com.secretariapay.api.entity.TransportCompany;
import com.secretariapay.api.entity.TravelRoute;
import com.secretariapay.api.entity.Trip;
import com.secretariapay.api.repository.TicketAuditLogRepository;
import com.secretariapay.api.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class OperationalReportDetailService {

    private final TicketRepository ticketRepository;
    private final TicketAuditLogRepository ticketAuditLogRepository;

    public OperationalReportDetailService(
            TicketRepository ticketRepository,
            TicketAuditLogRepository ticketAuditLogRepository
    ) {
        this.ticketRepository = ticketRepository;
        this.ticketAuditLogRepository = ticketAuditLogRepository;
    }

    @Transactional(readOnly = true)
    public List<OperationalTicketReportItemResponse> findGlobalTickets(
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        LocalDateTime resolvedStartAt = resolveStartAt(startAt);
        LocalDateTime resolvedEndAt = resolveEndAt(endAt);

        return ticketRepository.findByIssuedAtBetweenOrderByIssuedAtDesc(
                        resolvedStartAt,
                        resolvedEndAt
                )
                .stream()
                .map(this::toTicketReportItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OperationalTicketReportItemResponse> findCompanyTickets(
            UUID companyId,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        LocalDateTime resolvedStartAt = resolveStartAt(startAt);
        LocalDateTime resolvedEndAt = resolveEndAt(endAt);

        return ticketRepository.findByBooking_Trip_TransportCompany_IdAndIssuedAtBetweenOrderByIssuedAtDesc(
                        companyId,
                        resolvedStartAt,
                        resolvedEndAt
                )
                .stream()
                .map(this::toTicketReportItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketAuditLogResponse> findGlobalAuditLogs(
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        LocalDateTime resolvedStartAt = resolveStartAt(startAt);
        LocalDateTime resolvedEndAt = resolveEndAt(endAt);

        return ticketAuditLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                        resolvedStartAt,
                        resolvedEndAt
                )
                .stream()
                .map(this::toAuditLogResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketAuditLogResponse> findCompanyAuditLogs(
            UUID companyId,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        LocalDateTime resolvedStartAt = resolveStartAt(startAt);
        LocalDateTime resolvedEndAt = resolveEndAt(endAt);

        return ticketAuditLogRepository
                .findByTicket_Booking_Trip_TransportCompany_IdAndCreatedAtBetweenOrderByCreatedAtDesc(
                        companyId,
                        resolvedStartAt,
                        resolvedEndAt
                )
                .stream()
                .map(this::toAuditLogResponse)
                .toList();
    }

    private OperationalTicketReportItemResponse toTicketReportItem(Ticket ticket) {
        Booking booking = ticket.getBooking();
        Trip trip = booking.getTrip();
        Passenger passenger = booking.getPassenger();
        TransportCompany company = trip.getTransportCompany();
        TravelRoute route = trip.getRoute();

        return new OperationalTicketReportItemResponse()
                .setTicketId(ticket.getId())
                .setTicketCode(ticket.getTicketCode())
                .setTicketStatus(ticket.getStatus())

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

                .setIssuedAt(ticket.getIssuedAt())
                .setUsedAt(ticket.getUsedAt())
                .setCancelledAt(ticket.getCancelledAt())
                .setCreatedAt(ticket.getCreatedAt())
                .setUpdatedAt(ticket.getUpdatedAt());
    }

    private TicketAuditLogResponse toAuditLogResponse(TicketAuditLog log) {
        return new TicketAuditLogResponse()
                .setId(log.getId())
                .setTicketCode(log.getTicketCode())
                .setAction(log.getAction())
                .setSuccess(log.getSuccess())
                .setMessage(log.getMessage())
                .setTicketStatus(log.getTicketStatus())
                .setBookingStatus(log.getBookingStatus())
                .setIpAddress(log.getIpAddress())
                .setUserAgent(log.getUserAgent())
                .setCreatedAt(log.getCreatedAt());
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
}
