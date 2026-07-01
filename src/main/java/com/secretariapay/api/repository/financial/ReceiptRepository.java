package com.secretariapay.api.repository.financial;

import com.secretariapay.api.entity.financial.Receipt;
import com.secretariapay.api.entity.enums.financial.ReceiptStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {

    Optional<Receipt> findByReceiptCode(String receiptCode);

    Optional<Receipt> findByChargeId(UUID chargeId);

    boolean existsByReceiptCode(String receiptCode);

    List<Receipt> findByStatusOrderByIssuedAtDesc(ReceiptStatus status);
}