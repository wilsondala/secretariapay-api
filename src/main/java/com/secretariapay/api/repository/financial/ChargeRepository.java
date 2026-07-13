package com.secretariapay.api.repository.financial;

import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChargeRepository extends JpaRepository<Charge, UUID> {

    @EntityGraph(attributePaths = "student")
    Optional<Charge> findByChargeCode(String chargeCode);

    boolean existsByChargeCode(String chargeCode);

    boolean existsByStudentIdAndReferenceMonthIgnoreCase(UUID studentId, String referenceMonth);

    @Query("""
            select c from Charge c
            left join fetch c.student s
            where s.id = :studentId
              and (
                    c.status = com.secretariapay.api.entity.enums.financial.ChargeStatus.PAID
                    or not (
                        (lower(coalesce(c.chargeCode, '')) like '%imt-propina%'
                         or lower(coalesce(c.description, '')) like '%propina%')
                        and exists (
                            select 1 from Charge paid
                            where paid.student.id = s.id
                              and paid.status = com.secretariapay.api.entity.enums.financial.ChargeStatus.PAID
                              and lower(coalesce(paid.referenceMonth, '')) = lower(coalesce(c.referenceMonth, ''))
                              and (
                                    lower(coalesce(paid.chargeCode, '')) like '%imt-propina%'
                                    or lower(coalesce(paid.description, '')) like '%propina%'
                                  )
                        )
                    )
                  )
            order by c.dueDate desc
            """)
    List<Charge> findByStudentIdOrderByDueDateDesc(@Param("studentId") UUID studentId);

    @EntityGraph(attributePaths = "student")
    List<Charge> findByStatusOrderByDueDateAsc(ChargeStatus status);

    @EntityGraph(attributePaths = "student")
    List<Charge> findByDueDateBeforeAndStatusOrderByDueDateAsc(LocalDate dueDate, ChargeStatus status);

    @EntityGraph(attributePaths = "student")
    List<Charge> findByDueDateBetweenOrderByDueDateAsc(LocalDate startDate, LocalDate endDate);
}
