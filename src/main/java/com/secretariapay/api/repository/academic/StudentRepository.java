package com.secretariapay.api.repository.academic;

import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.academic.StudentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {

    Optional<Student> findByStudentNumber(String studentNumber);

    Optional<Student> findByDocumentNumberIgnoreCase(String documentNumber);

    Optional<Student> findByWhatsapp(String whatsapp);

    Optional<Student> findByPhone(String phone);

    boolean existsByStudentNumber(String studentNumber);

    List<Student> findByAcademicClassIdOrderByFullNameAsc(UUID academicClassId);

    List<Student> findByStatusOrderByFullNameAsc(StudentStatus status);

    List<Student> findByFinanciallyBlockedTrueOrderByFullNameAsc();

    List<Student> findTop5ByFullNameContainingIgnoreCaseOrderByFullNameAsc(String fullName);
}