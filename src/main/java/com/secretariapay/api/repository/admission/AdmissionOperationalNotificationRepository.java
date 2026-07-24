package com.secretariapay.api.repository.admission;

import com.secretariapay.api.entity.admission.AdmissionOperationalNotification;
import com.secretariapay.api.entity.enums.admission.AdmissionNotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AdmissionOperationalNotificationRepository
        extends JpaRepository<AdmissionOperationalNotification, UUID> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<AdmissionOperationalNotification>
    findTop20ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            Collection<AdmissionNotificationStatus> statuses,
            LocalDateTime nextAttemptAt
    );
}
