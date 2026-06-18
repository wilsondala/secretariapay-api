package com.vairapido.api.service;

import com.vairapido.api.dto.booking.BookingExpirationResponse;
import com.vairapido.api.dto.booking.BookingRequest;
import com.vairapido.api.dto.booking.BookingResponse;
import com.vairapido.api.entity.Booking;
import com.vairapido.api.entity.Passenger;
import com.vairapido.api.entity.Payment;
import com.vairapido.api.entity.TransportCompany;
import com.vairapido.api.entity.TravelRoute;
import com.vairapido.api.entity.Trip;
import com.vairapido.api.entity.enums.BookingStatus;
import com.vairapido.api.entity.enums.PaymentStatus;
import com.vairapido.api.entity.enums.TripStatus;
import com.vairapido.api.exception.NotFoundException;
import com.vairapido.api.repository.BookingRepository;
import com.vairapido.api.repository.PassengerRepository;
import com.vairapido.api.repository.PaymentRepository;
import com.vairapido.api.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private static final List<BookingStatus> ACTIVE_SEAT_STATUSES = List.of(
            BookingStatus.PENDING_PAYMENT,
            BookingStatus.PAID,
            BookingStatus.TICKET_ISSUED
    );

    private final BookingRepository bookingRepository;
    private final TripRepository tripRepository;
    private final PassengerRepository passengerRepository;
    private final PaymentRepository paymentRepository;

    public BookingService(
            BookingRepository bookingRepository,
            TripRepository tripRepository,
            PassengerRepository passengerRepository,
            PaymentRepository paymentRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.tripRepository = tripRepository;
        this.passengerRepository = passengerRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public BookingResponse create(BookingRequest request) {
        Trip trip = tripRepository.findById(request.getTripId())
                .orElseThrow(() -> new NotFoundException("Viagem não encontrada."));

        Passenger passenger = passengerRepository.findById(request.getPassengerId())
                .orElseThrow(() -> new NotFoundException("Passageiro não encontrado."));

        validateTripForBooking(trip);
        validateSeatNumber(trip, request.getSeatNumber());

        boolean seatAlreadyTaken = bookingRepository.existsByTrip_IdAndSeatNumberAndStatusIn(
                trip.getId(),
                request.getSeatNumber(),
                ACTIVE_SEAT_STATUSES
        );

        if (seatAlreadyTaken) {
            throw new IllegalArgumentException("Esta poltrona já está reservada ou vendida para esta viagem.");
        }

        trip.decreaseAvailableSeats();

        Booking booking = new Booking()
                .setTrip(trip)
                .setPassenger(passenger)
                .setSeatNumber(request.getSeatNumber())
                .setBookingCode(generateBookingCode())
                .setStatus(BookingStatus.PENDING_PAYMENT)
                .setAmount(trip.getPrice())
                .setCurrency(trip.getCurrency())
                .setExpiresAt(LocalDateTime.now().plusMinutes(15));

        Booking savedBooking = bookingRepository.save(booking);
        tripRepository.save(trip);

        return toResponse(savedBooking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> findAll() {
        return bookingRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse findById(UUID id) {
        return toResponse(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public BookingResponse findByCode(String bookingCode) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new NotFoundException("Reserva não encontrada."));

        return toResponse(booking);
    }

    @Transactional
    public BookingResponse confirmPayment(UUID id) {
        Booking booking = findEntityById(id);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Não é possível confirmar pagamento de uma reserva cancelada.");
        }

        if (booking.getStatus() == BookingStatus.EXPIRED) {
            throw new IllegalArgumentException("Não é possível confirmar pagamento de uma reserva expirada.");
        }

        booking
                .setStatus(BookingStatus.PAID)
                .setPaidAt(LocalDateTime.now());

        return toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse issueTicket(UUID id) {
        Booking booking = findEntityById(id);

        if (booking.getStatus() != BookingStatus.PAID) {
            throw new IllegalArgumentException("A reserva precisa estar paga para emitir bilhete.");
        }

        booking.setStatus(BookingStatus.TICKET_ISSUED);

        return toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse cancel(UUID id) {
        Booking booking = findEntityById(id);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return toResponse(booking);
        }

        if (booking.getStatus() == BookingStatus.TICKET_ISSUED) {
            throw new IllegalArgumentException("Não é possível cancelar uma reserva com bilhete já emitido.");
        }

        booking
                .setStatus(BookingStatus.CANCELLED)
                .setCancelledAt(LocalDateTime.now());

        Trip trip = booking.getTrip();
        trip.increaseAvailableSeats();

        tripRepository.save(trip);

        return toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingExpirationResponse expireOverdueBookings() {
        LocalDateTime now = LocalDateTime.now();

        List<Booking> overdueBookings = bookingRepository.findByStatusAndExpiresAtBefore(
                BookingStatus.PENDING_PAYMENT,
                now
        );

        int expiredCount = 0;

        for (Booking booking : overdueBookings) {
            booking.setStatus(BookingStatus.EXPIRED);

            Trip trip = booking.getTrip();
            trip.increaseAvailableSeats();

            List<Payment> pendingPayments = paymentRepository.findByBooking_IdAndStatus(
                    booking.getId(),
                    PaymentStatus.PENDING
            );

            for (Payment payment : pendingPayments) {
                payment
                        .setStatus(PaymentStatus.EXPIRED);
            }

            tripRepository.save(trip);
            paymentRepository.saveAll(pendingPayments);
            bookingRepository.save(booking);

            expiredCount++;
        }

        return new BookingExpirationResponse()
                .setExpiredBookings(expiredCount)
                .setProcessedAt(now)
                .setMessage(expiredCount == 1
                        ? "1 reserva vencida foi expirada e o assento foi liberado."
                        : expiredCount + " reservas vencidas foram expiradas e os assentos foram liberados.");
    }

    private void validateTripForBooking(Trip trip) {
        if (trip.getStatus() != TripStatus.SCHEDULED) {
            throw new IllegalArgumentException("A viagem não está disponível para reserva.");
        }

        if (trip.getDepartureAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Não é possível reservar uma viagem que já saiu.");
        }

        if (trip.getAvailableSeats() == null || trip.getAvailableSeats() <= 0) {
            throw new IllegalArgumentException("Não há assentos disponíveis para esta viagem.");
        }
    }

    private void validateSeatNumber(Trip trip, Integer seatNumber) {
        if (seatNumber == null || seatNumber < 1) {
            throw new IllegalArgumentException("Número da poltrona inválido.");
        }

        if (seatNumber > trip.getTotalSeats()) {
            throw new IllegalArgumentException("A poltrona informada é maior que o total de assentos da viagem.");
        }
    }

    private Booking findEntityById(UUID id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reserva não encontrada."));
    }

    private String generateBookingCode() {
        String code;

        do {
            code = "VR" + System.currentTimeMillis();
        } while (bookingRepository.findByBookingCode(code).isPresent());

        return code;
    }

    private BookingResponse toResponse(Booking booking) {
        Trip trip = booking.getTrip();
        Passenger passenger = booking.getPassenger();
        TransportCompany company = trip.getTransportCompany();
        TravelRoute route = trip.getRoute();

        return new BookingResponse()
                .setId(booking.getId())
                .setBookingCode(booking.getBookingCode())

                .setTripId(trip.getId())
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

                .setPassengerId(passenger.getId())
                .setPassengerName(passenger.getFullName())
                .setPassengerDocument(passenger.getDocumentNumber())
                .setPassengerEmail(passenger.getEmail())
                .setPassengerWhatsapp(passenger.getWhatsapp())

                .setSeatNumber(booking.getSeatNumber())
                .setStatus(booking.getStatus())
                .setAmount(booking.getAmount())
                .setCurrency(booking.getCurrency())
                .setExpiresAt(booking.getExpiresAt())
                .setPaidAt(booking.getPaidAt())
                .setCancelledAt(booking.getCancelledAt())
                .setCreatedAt(booking.getCreatedAt())
                .setUpdatedAt(booking.getUpdatedAt());
    }
}