package com.secretariapay.api.service;

import com.secretariapay.api.dto.dashboard.DashboardSummaryResponse;
import com.secretariapay.api.entity.TransportCompany;
import com.secretariapay.api.entity.enums.BookingStatus;
import com.secretariapay.api.entity.enums.PaymentStatus;
import com.secretariapay.api.entity.enums.TicketStatus;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.BookingRepository;
import com.secretariapay.api.repository.PassengerRepository;
import com.secretariapay.api.repository.PaymentRepository;
import com.secretariapay.api.repository.TicketRepository;
import com.secretariapay.api.repository.TransportCompanyRepository;
import com.secretariapay.api.repository.TravelRouteRepository;
import com.secretariapay.api.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

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

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getCompanySummary(UUID companyId) {
        TransportCompany company = transportCompanyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Empresa de transporte não encontrada."));

        return new DashboardSummaryResponse()
                .setTotalTransportCompanies(1)
                .setTotalRoutes(tripRepository.countDistinctRoutesByTransportCompanyId(company.getId()))
                .setTotalTrips(tripRepository.countByTransportCompany_Id(company.getId()))
                .setTotalPassengers(bookingRepository.countDistinctPassengersByTransportCompanyId(company.getId()))
                .setTotalBookings(bookingRepository.countByTrip_TransportCompany_Id(company.getId()))
                .setTotalPayments(paymentRepository.countByBooking_Trip_TransportCompany_Id(company.getId()))
                .setTotalTickets(ticketRepository.countByBooking_Trip_TransportCompany_Id(company.getId()))

                .setPendingBookings(bookingRepository.countByTrip_TransportCompany_IdAndStatus(
                        company.getId(),
                        BookingStatus.PENDING_PAYMENT
                ))
                .setPaidBookings(bookingRepository.countByTrip_TransportCompany_IdAndStatus(
                        company.getId(),
                        BookingStatus.PAID
                ))
                .setIssuedTicketBookings(bookingRepository.countByTrip_TransportCompany_IdAndStatus(
                        company.getId(),
                        BookingStatus.TICKET_ISSUED
                ))
                .setExpiredBookings(bookingRepository.countByTrip_TransportCompany_IdAndStatus(
                        company.getId(),
                        BookingStatus.EXPIRED
                ))
                .setCancelledBookings(bookingRepository.countByTrip_TransportCompany_IdAndStatus(
                        company.getId(),
                        BookingStatus.CANCELLED
                ))

                .setPendingPayments(paymentRepository.countByBooking_Trip_TransportCompany_IdAndStatus(
                        company.getId(),
                        PaymentStatus.PENDING
                ))
                .setPaidPayments(paymentRepository.countByBooking_Trip_TransportCompany_IdAndStatus(
                        company.getId(),
                        PaymentStatus.PAID
                ))
                .setExpiredPayments(paymentRepository.countByBooking_Trip_TransportCompany_IdAndStatus(
                        company.getId(),
                        PaymentStatus.EXPIRED
                ))
                .setCancelledPayments(paymentRepository.countByBooking_Trip_TransportCompany_IdAndStatus(
                        company.getId(),
                        PaymentStatus.CANCELLED
                ))

                .setValidTickets(ticketRepository.countByStatusAndBooking_Trip_TransportCompany_Id(
                        TicketStatus.VALID,
                        company.getId()
                ))
                .setUsedTickets(ticketRepository.countByStatusAndBooking_Trip_TransportCompany_Id(
                        TicketStatus.USED,
                        company.getId()
                ))
                .setCancelledTickets(ticketRepository.countByStatusAndBooking_Trip_TransportCompany_Id(
                        TicketStatus.CANCELLED,
                        company.getId()
                ))

                .setConfirmedRevenue(paymentRepository.sumPaidAmountByCompany(company.getId()))
                .setGeneratedAt(LocalDateTime.now());
    }
}
