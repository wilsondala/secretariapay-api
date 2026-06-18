package com.vairapido.api.service;

import com.vairapido.api.dto.dashboard.DashboardSummaryResponse;
import com.vairapido.api.entity.enums.BookingStatus;
import com.vairapido.api.entity.enums.PaymentStatus;
import com.vairapido.api.entity.enums.TicketStatus;
import com.vairapido.api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DashboardService {

    private final TransportCompanyRepository transportCompanyRepository;
    private final TravelRouteRepository travelRouteRepository;
    private final TripRepository tripRepository;
    private final PassengerRepository passengerRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;

    public DashboardService(
            TransportCompanyRepository transportCompanyRepository,
            TravelRouteRepository travelRouteRepository,
            TripRepository tripRepository,
            PassengerRepository passengerRepository,
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            TicketRepository ticketRepository
    ) {
        this.transportCompanyRepository = transportCompanyRepository;
        this.travelRouteRepository = travelRouteRepository;
        this.tripRepository = tripRepository;
        this.passengerRepository = passengerRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.ticketRepository = ticketRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        return new DashboardSummaryResponse()
                .setTotalTransportCompanies(transportCompanyRepository.count())
                .setTotalRoutes(travelRouteRepository.count())
                .setTotalTrips(tripRepository.count())
                .setTotalPassengers(passengerRepository.count())
                .setTotalBookings(bookingRepository.count())
                .setTotalPayments(paymentRepository.count())
                .setTotalTickets(ticketRepository.count())

                .setPendingBookings(bookingRepository.countByStatus(BookingStatus.PENDING_PAYMENT))
                .setPaidBookings(bookingRepository.countByStatus(BookingStatus.PAID))
                .setIssuedTicketBookings(bookingRepository.countByStatus(BookingStatus.TICKET_ISSUED))
                .setExpiredBookings(bookingRepository.countByStatus(BookingStatus.EXPIRED))
                .setCancelledBookings(bookingRepository.countByStatus(BookingStatus.CANCELLED))

                .setPendingPayments(paymentRepository.countByStatus(PaymentStatus.PENDING))
                .setPaidPayments(paymentRepository.countByStatus(PaymentStatus.PAID))
                .setExpiredPayments(paymentRepository.countByStatus(PaymentStatus.EXPIRED))
                .setCancelledPayments(paymentRepository.countByStatus(PaymentStatus.CANCELLED))

                .setValidTickets(ticketRepository.countByStatus(TicketStatus.VALID))
                .setUsedTickets(ticketRepository.countByStatus(TicketStatus.USED))
                .setCancelledTickets(ticketRepository.countByStatus(TicketStatus.CANCELLED))

                .setConfirmedRevenue(paymentRepository.sumPaidAmount())
                .setGeneratedAt(LocalDateTime.now());
    }
}