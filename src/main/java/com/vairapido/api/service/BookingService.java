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
import com.vairapido.api.entity.enums.PassengerFareType;
import com.vairapido.api.entity.enums.TripSegmentType;
import com.vairapido.api.entity.enums.PaymentStatus;
import com.vairapido.api.entity.enums.TripStatus;
import com.vairapido.api.exception.NotFoundException;
import com.vairapido.api.repository.BookingRepository;
import com.vairapido.api.repository.PassengerRepository;
import com.vairapido.api.repository.PaymentRepository;
import com.vairapido.api.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;

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

        PassengerFareType passengerFareType = resolvePassengerFareType(request, passenger);
        Integer passengerAge = calculatePassengerAge(passenger);
        validatePassengerFareType(passengerFareType, passengerAge);

        BigDecimal farePercentage = resolveFarePercentage(passengerFareType);
        BigDecimal amount = calculateFareAmount(trip.getPrice(), farePercentage);
        TripSegmentType tripSegmentType = resolveTripSegmentType(request);

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
                .setPassengerFareType(passengerFareType)
                .setPassengerAge(passengerAge)
                .setFarePercentage(farePercentage)
                .setTripSegmentType(tripSegmentType)
                .setChildGuardianName(request.getChildGuardianName())
                .setChildGuardianPhone(request.getChildGuardianPhone())
                .setMinorGuardianName(request.getMinorGuardianName())
                .setMinorGuardianPhone(request.getMinorGuardianPhone())
                .setMinorPickupResponsibleName(request.getMinorPickupResponsibleName())
                .setMinorPickupResponsiblePhone(request.getMinorPickupResponsiblePhone())
                .setBookingCode(generateBookingCode())
                .setStatus(BookingStatus.PENDING_PAYMENT)
                .setAmount(amount)
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
    public List<BookingResponse> findByCompanyId(UUID companyId) {
        return bookingRepository.findByTrip_TransportCompany_Id(companyId)
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

    private PassengerFareType resolvePassengerFareType(
            BookingRequest request,
            Passenger passenger) {
        if (request.getPassengerFareType() != null) {
            return request.getPassengerFareType();
        }

        Integer age = calculatePassengerAge(passenger);

        if (age == null) {
            return PassengerFareType.ADULT;
        }

        if (age <= 5) {
            return PassengerFareType.INFANT_ON_LAP;
        }

        if (age <= 11) {
            return PassengerFareType.CHILD_WITH_SEAT;
        }

        if (age <= 17) {
            return PassengerFareType.MINOR_UNACCOMPANIED;
        }

        return PassengerFareType.ADULT;
    }

    private Integer calculatePassengerAge(Passenger passenger) {
        if (passenger == null || passenger.getBirthDate() == null) {
            return null;
        }

        return Period.between(passenger.getBirthDate(), LocalDate.now()).getYears();
    }

    private void validatePassengerFareType(
            PassengerFareType passengerFareType,
            Integer passengerAge) {
        if (passengerFareType == null) {
            return;
        }

        if (PassengerFareType.INFANT_ON_LAP.equals(passengerFareType)) {
            throw new IllegalArgumentException(
                    "Criança de colo sem poltrona será liberada no Módulo 79C. Para esta etapa, use criança com poltrona.");
        }

        if (passengerAge != null && passengerAge < 0) {
            throw new IllegalArgumentException("Data de nascimento do passageiro inválida.");
        }
    }

    private BigDecimal resolveFarePercentage(PassengerFareType passengerFareType) {
        if (passengerFareType == null) {
            return BigDecimal.valueOf(100);
        }

        return switch (passengerFareType) {
            case CHILD_WITH_SEAT -> BigDecimal.valueOf(50);
            case INFANT_ON_LAP -> BigDecimal.ZERO;
            case MINOR_UNACCOMPANIED, ADULT -> BigDecimal.valueOf(100);
        };
    }

    private BigDecimal calculateFareAmount(
            BigDecimal baseAmount,
            BigDecimal farePercentage) {
        BigDecimal safeBaseAmount = baseAmount != null ? baseAmount : BigDecimal.ZERO;
        BigDecimal safeFarePercentage = farePercentage != null
                ? farePercentage
                : BigDecimal.valueOf(100);

        return safeBaseAmount
                .multiply(safeFarePercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private TripSegmentType resolveTripSegmentType(BookingRequest request) {
        if (request == null || request.getTripSegmentType() == null) {
            return TripSegmentType.SINGLE;
        }

        return request.getTripSegmentType();
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
                .setPassengerFareType(booking.getPassengerFareType())
                .setPassengerAge(booking.getPassengerAge())
                .setFarePercentage(booking.getFarePercentage())
                .setTripSegmentType(booking.getTripSegmentType())
                .setChildGuardianName(booking.getChildGuardianName())
                .setChildGuardianPhone(booking.getChildGuardianPhone())
                .setMinorGuardianName(booking.getMinorGuardianName())
                .setMinorGuardianPhone(booking.getMinorGuardianPhone())
                .setMinorPickupResponsibleName(booking.getMinorPickupResponsibleName())
                .setMinorPickupResponsiblePhone(booking.getMinorPickupResponsiblePhone())
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