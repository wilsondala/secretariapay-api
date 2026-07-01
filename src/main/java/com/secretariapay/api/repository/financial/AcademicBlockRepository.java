package com.secretariapay.api.repository.financial;

import com.secretariapay.api.entity.financial.AcademicBlock;
import com.secretariapay.api.entity.enums.financial.AcademicBlockStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AcademicBlockRepository extends JpaRepository<AcademicBlock, UUID> {

    List<AcademicBlock> findByStudentIdOrderByBlockedAtDesc(UUID studentId);

    List<AcademicBlock> findByStatusOrderByBlockedAtDesc(AcademicBlockStatus status);

    List<AcademicBlock> findByStudentIdAndStatusOrderByBlockedAtDesc(UUID studentId, AcademicBlockStatus status);
}