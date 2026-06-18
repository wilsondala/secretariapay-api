package com.vairapido.api.repository;

import com.vairapido.api.entity.Payment;
import com.vairapido.api.entity.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByPaymentCode(String paymentCode);

    List<Payment> findByBooking_Id(UUID bookingId);

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByBooking_IdAndStatus(UUID bookingId, PaymentStatus status);

    long countByStatus(PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'PAID'")
    BigDecimal sumPaidAmount();
}