package com.secretariapay.api.repository.whatsapp;

import com.secretariapay.api.entity.enums.whatsapp.SecretariaPayMessageStatus;
import com.secretariapay.api.entity.whatsapp.SecretariaPayMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SecretariaPayMessageRepository extends JpaRepository<SecretariaPayMessage, UUID> {

    List<SecretariaPayMessage> findByInstitutionIdOrderByCreatedAtDesc(UUID institutionId);

    List<SecretariaPayMessage> findByStudentIdOrderByCreatedAtDesc(UUID studentId);

    List<SecretariaPayMessage> findByChargeIdOrderByCreatedAtDesc(UUID chargeId);

    List<SecretariaPayMessage> findTop20ByStatusOrderByCreatedAtAsc(SecretariaPayMessageStatus status);
}
