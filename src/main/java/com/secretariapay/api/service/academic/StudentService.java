package com.secretariapay.api.service.academic;

import com.secretariapay.api.dto.academic.StudentRequest;
import com.secretariapay.api.dto.academic.StudentResponse;
import com.secretariapay.api.entity.academic.AcademicClass;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.academic.StudentStatus;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.academic.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class StudentService {

    private final StudentRepository repository;
    private final AcademicClassService academicClassService;

    public StudentService(StudentRepository repository, AcademicClassService academicClassService) {
        this.repository = repository;
        this.academicClassService = academicClassService;
    }

    @Transactional
    public StudentResponse create(StudentRequest request) {
        if (repository.existsByStudentNumber(request.getStudentNumber())) {
            throw new IllegalArgumentException("Já existe estudante cadastrado com este número académico.");
        }
        AcademicClass academicClass = academicClassService.findEntityById(request.getAcademicClassId());
        Student student = applyRequest(new Student(), request, academicClass);
        return toResponse(repository.save(student));
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> findByAcademicClassId(UUID academicClassId) {
        return repository.findByAcademicClassIdOrderByFullNameAsc(academicClassId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public StudentResponse findById(UUID id) {
        return toResponse(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public StudentResponse findByStudentNumber(String studentNumber) {
        Student student = repository.findByStudentNumber(studentNumber)
                .orElseThrow(() -> new NotFoundException("Estudante não encontrado."));
        return toResponse(student);
    }

    @Transactional
    public StudentResponse update(UUID id, StudentRequest request) {
        Student student = findEntityById(id);
        AcademicClass academicClass = academicClassService.findEntityById(request.getAcademicClassId());
        return toResponse(repository.save(applyRequest(student, request, academicClass)));
    }

    public Student findEntityById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Estudante não encontrado."));
    }

    private Student applyRequest(Student student, StudentRequest request, AcademicClass academicClass) {
        student
                .setAcademicClass(academicClass)
                .setStudentNumber(request.getStudentNumber())
                .setFullName(request.getFullName())
                .setDocumentType(request.getDocumentType())
                .setDocumentNumber(request.getDocumentNumber())
                .setEmail(request.getEmail())
                .setPhone(request.getPhone())
                .setWhatsapp(request.getWhatsapp())
                .setBirthDate(request.getBirthDate())
                .setGuardianName(request.getGuardianName())
                .setGuardianPhone(request.getGuardianPhone())
                .setGuardianEmail(request.getGuardianEmail())
                .setBlockedReason(request.getBlockedReason());
        student.setStatus(request.getStatus() == null ? StudentStatus.ACTIVE : request.getStatus());
        if (request.getFinanciallyBlocked() != null) {
            student.setFinanciallyBlocked(request.getFinanciallyBlocked());
        }
        return student;
    }

    private StudentResponse toResponse(Student student) {
        return new StudentResponse()
                .setId(student.getId())
                .setAcademicClassId(student.getAcademicClass().getId())
                .setAcademicClassName(student.getAcademicClass().getName())
                .setCourseName(student.getAcademicClass().getCourse().getName())
                .setStudentNumber(student.getStudentNumber())
                .setFullName(student.getFullName())
                .setDocumentType(student.getDocumentType())
                .setDocumentNumber(student.getDocumentNumber())
                .setEmail(student.getEmail())
                .setPhone(student.getPhone())
                .setWhatsapp(student.getWhatsapp())
                .setBirthDate(student.getBirthDate())
                .setGuardianName(student.getGuardianName())
                .setGuardianPhone(student.getGuardianPhone())
                .setGuardianEmail(student.getGuardianEmail())
                .setStatus(student.getStatus())
                .setFinanciallyBlocked(student.getFinanciallyBlocked())
                .setBlockedReason(student.getBlockedReason())
                .setCreatedAt(student.getCreatedAt())
                .setUpdatedAt(student.getUpdatedAt());
    }
}
