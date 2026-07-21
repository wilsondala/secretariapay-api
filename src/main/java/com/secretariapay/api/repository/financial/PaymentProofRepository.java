package com.secretariapay.api.repository.financial;

import com.secretariapay.api.entity.financial.PaymentProof;
import com.secretariapay.api.entity.enums.financial.PaymentProofStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentProofRepository extends JpaRepository<PaymentProof, UUID> {

    boolean existsByFileUrl(String fileUrl);

    List<PaymentProof> findByChargeIdOrderBySubmittedAtDesc(UUID chargeId);

    List<PaymentProof> findByStatusOrderBySubmittedAtAsc(PaymentProofStatus status);
}
