package com.secretariapay.api.service;

import com.secretariapay.api.dto.payment.PaymentRequest;
import com.secretariapay.api.dto.payment.PaymentResponse;
import com.secretariapay.api.entity.Booking;
import com.secretariapay.api.entity.Passenger;
import com.secretariapay.api.entity.Payment;
import com.secretariapay.api.entity.TransportCompany;
import com.secretariapay.api.entity.TravelRoute;
import com.secretariapay.api.entity.Trip;
import com.secretariapay.api.entity.enums.BookingStatus;
import com.secretariapay.api.entity.enums.PaymentMethod;
import com.secretariapay.api.entity.enums.PaymentStatus;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.BookingRepository;
import com.secretariapay.api.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    public PaymentService(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public PaymentResponse create(PaymentRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new NotFoundException("Reserva não encontrada."));

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new IllegalArgumentException("Só é possível gerar pagamento para reserva pendente.");
        }

        PaymentMethod method = request.getMethod();

        Payment payment = new Payment()
                .setBooking(booking)
                .setPaymentCode(generatePaymentCode())
                .setMethod(method)
                .setStatus(PaymentStatus.PENDING)
                .setAmount(booking.getAmount())
                .setCurrency(booking.getCurrency())
                .setGatewayName(resolveGatewayName(method))
                .setGatewayTransactionId("SIM-" + UUID.randomUUID())
                .setExpiresAt(LocalDateTime.now().plusMinutes(15));

        if (method == PaymentMethod.PIX) {
            payment
                    .setPixCopyPaste(generateFakePixCopyPaste(booking))
                    .setPixQrCodeUrl(generateFakePixQrCodeUrl(booking));
        }

        return toResponse(paymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> findAll() {
        return paymentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse findById(UUID id) {
        return toResponse(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public PaymentResponse findByCode(String paymentCode) {
        Payment payment = paymentRepository.findByPaymentCode(paymentCode)
                .orElseThrow(() -> new NotFoundException("Pagamento não encontrado."));

        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse confirm(UUID id) {
        Payment payment = findEntityById(id);

        if (payment.getStatus() == PaymentStatus.PAID) {
            return toResponse(payment);
        }

        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            throw new IllegalArgumentException("Não é possível confirmar um pagamento cancelado.");
        }

        if (payment.getStatus() == PaymentStatus.EXPIRED) {
            throw new IllegalArgumentException("Não é possível confirmar um pagamento expirado.");
        }

        Booking booking = payment.getBooking();

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new IllegalArgumentException("A reserva não está pendente de pagamento.");
        }

        LocalDateTime now = LocalDateTime.now();

        payment
                .setStatus(PaymentStatus.PAID)
                .setPaidAt(now);

        booking
                .setStatus(BookingStatus.PAID)
                .setPaidAt(now);

        bookingRepository.save(booking);

        return toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResponse cancel(UUID id) {
        Payment payment = findEntityById(id);

        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new IllegalArgumentException("Não é possível cancelar um pagamento já confirmado.");
        }

        payment
                .setStatus(PaymentStatus.CANCELLED)
                .setCancelledAt(LocalDateTime.now());

        return toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResponse expire(UUID id) {
        Payment payment = findEntityById(id);

        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new IllegalArgumentException("Não é possível expirar um pagamento já confirmado.");
        }

        payment.setStatus(PaymentStatus.EXPIRED);

        return toResponse(paymentRepository.save(payment));
    }

    private Payment findEntityById(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pagamento não encontrado."));
    }

    private String generatePaymentCode() {
        String code;

        do {
            code = "PAY" + System.currentTimeMillis();
        } while (paymentRepository.findByPaymentCode(code).isPresent());

        return code;
    }

    private String resolveGatewayName(PaymentMethod method) {
        return switch (method) {
            case PIX -> "PIX_SIMULADO";
            case CREDIT_CARD -> "CARD_SIMULADO";
            case DEBIT_CARD -> "DEBIT_SIMULADO";
            case BANK_TRANSFER -> "BANK_TRANSFER_SIMULADO";
            case MULTICAIXA_EXPRESS -> "MULTICAIXA_EXPRESS_SIMULADO";
            case UNITEL_MONEY -> "UNITEL_MONEY_SIMULADO";
            case AFRIMONEY -> "AFRIMONEY_SIMULADO";
            case CASH -> "CASH";
        };
    }

    private String generateFakePixCopyPaste(Booking booking) {
        return "00020126580014BR.GOV.BCB.PIX0136"
                + booking.getBookingCode()
                + "5204000053039865405"
                + booking.getAmount()
                + "5802BR5910VAIRAPIDO6009SAO PAULO62070503***6304FAKE";
    }

    private String generateFakePixQrCodeUrl(Booking booking) {
        return "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data="
                + booking.getBookingCode();
    }

    private PaymentResponse toResponse(Payment payment) {
        Booking booking = payment.getBooking();
        Trip trip = booking.getTrip();
        Passenger passenger = booking.getPassenger();
        TransportCompany company = trip.getTransportCompany();
        TravelRoute route = trip.getRoute();

        return new PaymentResponse()
                .setId(payment.getId())
                .setPaymentCode(payment.getPaymentCode())

                .setBookingId(booking.getId())
                .setBookingCode(booking.getBookingCode())
                .setBookingStatus(booking.getStatus())

                .setPassengerName(passenger.getFullName())
                .setPassengerDocument(passenger.getDocumentNumber())
                .setPassengerWhatsapp(passenger.getWhatsapp())

                .setCompanyTradeName(company.getTradeName())
                .setOriginCity(route.getOriginCity())
                .setDestinationCity(route.getDestinationCity())
                .setDepartureAt(trip.getDepartureAt())
                .setSeatNumber(booking.getSeatNumber())

                .setMethod(payment.getMethod())
                .setStatus(payment.getStatus())
                .setAmount(payment.getAmount())
                .setCurrency(payment.getCurrency())
                .setPixCopyPaste(payment.getPixCopyPaste())
                .setPixQrCodeUrl(payment.getPixQrCodeUrl())
                .setGatewayName(payment.getGatewayName())
                .setGatewayTransactionId(payment.getGatewayTransactionId())
                .setExpiresAt(payment.getExpiresAt())
                .setPaidAt(payment.getPaidAt())
                .setCancelledAt(payment.getCancelledAt())
                .setCreatedAt(payment.getCreatedAt())
                .setUpdatedAt(payment.getUpdatedAt());
    }
}
