package com.secretariapay.api.repository.enrollment;

import com.secretariapay.api.entity.enrollment.AcademicEnrollmentInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AcademicEnrollmentInvoiceRepository extends JpaRepository<AcademicEnrollmentInvoice, UUID> {

    Optional<AcademicEnrollmentInvoice> findByEnrollmentRequestId(UUID enrollmentRequestId);

    boolean existsByInvoiceCode(String invoiceCode);
}
