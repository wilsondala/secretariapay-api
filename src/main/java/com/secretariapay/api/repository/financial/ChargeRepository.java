package com.secretariapay.api.repository.financial;

import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import com.secretariapay.api.entity.financial.Charge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
                              and (
                                    (year(paid.dueDate) = year(c.dueDate)
                                     and month(paid.dueDate) = month(c.dueDate))
                                    or lower(coalesce(paid.referenceMonth, '')) = lower(coalesce(c.referenceMonth, ''))
                                  )
                              and (
                                    lower(coalesce(paid.chargeCode, '')) like '%propina%'
                                    or lower(coalesce(paid.description, '')) like '%propina%'
                                  )
                        )
                    )
                  )
            order by c.dueDate desc
            """)
    List<Charge> findByStudentIdOrderByDueDateDesc(@Param("studentId") UUID studentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c from Charge c
            where c.student.id = :studentId
              and c.dueDate between :periodStart and :periodEnd
              and c.status in (
                    com.secretariapay.api.entity.enums.financial.ChargeStatus.PENDING,
                    com.secretariapay.api.entity.enums.financial.ChargeStatus.PAID,
                    com.secretariapay.api.entity.enums.financial.ChargeStatus.OVERDUE,
                    com.secretariapay.api.entity.enums.financial.ChargeStatus.PARTIALLY_PAID
                  )
              and (
                    lower(coalesce(c.chargeCode, '')) like '%propina%'
                    or lower(coalesce(c.description, '')) like '%propina%'
                  )
            order by
              case
                when c.status = com.secretariapay.api.entity.enums.financial.ChargeStatus.PAID then 0
                when c.status = com.secretariapay.api.entity.enums.financial.ChargeStatus.PARTIALLY_PAID then 1
                when c.status = com.secretariapay.api.entity.enums.financial.ChargeStatus.OVERDUE then 2
                else 3
              end,
              c.createdAt asc
            """)
    List<Charge> findActiveTuitionByStudentAndPeriodForUpdate(
            @Param("studentId") UUID studentId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd
    );

    @Query("""
            select case when count(c) > 0 then true else false end
            from Charge c
            where c.student.id = :studentId
              and c.dueDate between :periodStart and :periodEnd
              and c.status in (
                    com.secretariapay.api.entity.enums.financial.ChargeStatus.PENDING,
                    com.secretariapay.api.entity.enums.financial.ChargeStatus.PAID,
                    com.secretariapay.api.entity.enums.financial.ChargeStatus.OVERDUE,
                    com.secretariapay.api.entity.enums.financial.ChargeStatus.PARTIALLY_PAID
                  )
              and (
                    lower(coalesce(c.chargeCode, '')) like '%propina%'
                    or lower(coalesce(c.description, '')) like '%propina%'
                  )
            """)
    boolean existsActiveTuitionByStudentAndPeriod(
            @Param("studentId") UUID studentId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd
    );

    @EntityGraph(attributePaths = "student")
    List<Charge> findByStatusOrderByDueDateAsc(ChargeStatus status);

    @EntityGraph(attributePaths = "student")
    List<Charge> findByDueDateBeforeAndStatusOrderByDueDateAsc(LocalDate dueDate, ChargeStatus status);

    @EntityGraph(attributePaths = "student")
    List<Charge> findByDueDateBetweenOrderByDueDateAsc(LocalDate startDate, LocalDate endDate);
}
