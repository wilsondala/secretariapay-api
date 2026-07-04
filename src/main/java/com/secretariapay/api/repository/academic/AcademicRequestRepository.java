package com.secretariapay.api.repository.academic;
import com.secretariapay.api.entity.academic.AcademicRequest;
import com.secretariapay.api.entity.enums.academic.AcademicRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AcademicRequestRepository extends JpaRepository<AcademicRequest, UUID> {
    List<AcademicRequest> findByStudentIdOrderByCreatedAtDesc(UUID studentId);
    List<AcademicRequest> findByStatusOrderByCreatedAtDesc(AcademicRequestStatus status);
    boolean existsByRequestCode(String requestCode);
}
