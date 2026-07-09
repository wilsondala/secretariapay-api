package com.secretariapay.api.repository.financial;

import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChargeRepository extends JpaRepository<Charge, UUID> {

    @EntityGraph(attributePaths = "student")
    Optional<Charge> findByChargeCode(String chargeCode);

    boolean existsByChargeCode(String chargeCode);

    @EntityGraph(attributePaths = "student")
    List<Charge> findByStudentIdOrderByDueDateDesc(UUID studentId);

    @EntityGraph(attributePaths = "student")
    List<Charge> findByStatusOrderByDueDateAsc(ChargeStatus status);

    @EntityGraph(attributePaths = "student")
    List<Charge> findByDueDateBeforeAndStatusOrderByDueDateAsc(LocalDate dueDate, ChargeStatus status);

    @EntityGraph(attributePaths = "student")
    List<Charge> findByDueDateBetweenOrderByDueDateAsc(LocalDate startDate, LocalDate endDate);
}
