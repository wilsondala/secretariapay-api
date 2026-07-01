package com.secretariapay.api.service.financial;

import com.secretariapay.api.dto.financial.AcademicBlockReleaseRequest;
import com.secretariapay.api.dto.financial.AcademicBlockRequest;
import com.secretariapay.api.dto.financial.AcademicBlockResponse;
import com.secretariapay.api.entity.User;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.academic.StudentStatus;
import com.secretariapay.api.entity.enums.financial.AcademicBlockStatus;
import com.secretariapay.api.entity.financial.AcademicBlock;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.UserRepository;
import com.secretariapay.api.repository.academic.StudentRepository;
import com.secretariapay.api.repository.financial.AcademicBlockRepository;
import com.secretariapay.api.repository.financial.ChargeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AcademicBlockService {

    private final AcademicBlockRepository academicBlockRepository;
    private final StudentRepository studentRepository;
    private final ChargeRepository chargeRepository;
    private final UserRepository userRepository;

    public AcademicBlockService(
            AcademicBlockRepository academicBlockRepository,
            StudentRepository studentRepository,
            ChargeRepository chargeRepository,
            UserRepository userRepository
    ) {
        this.academicBlockRepository = academicBlockRepository;
        this.studentRepository = studentRepository;
        this.chargeRepository = chargeRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AcademicBlockResponse create(AcademicBlockRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new NotFoundException("Estudante não encontrado."));

        Charge charge = request.getChargeId() == null
                ? null
                : chargeRepository.findById(request.getChargeId())
                .orElseThrow(() -> new NotFoundException("Cobrança não encontrada."));

        User blockedBy = request.getBlockedByUserId() == null
                ? null
                : userRepository.findById(request.getBlockedByUserId())
                .orElseThrow(() -> new NotFoundException("Utilizador não encontrado."));

        AcademicBlock block = new AcademicBlock()
                .setStudent(student)
                .setCharge(charge)
                .setBlockedService(request.getBlockedService())
                .setReason(request.getReason())
                .setBlockedBy(blockedBy)
                .setStatus(AcademicBlockStatus.ACTIVE);

        student
                .setFinanciallyBlocked(true)
                .setStatus(StudentStatus.BLOCKED)
                .setBlockedReason(request.getReason());

        studentRepository.save(student);

        return toResponse(academicBlockRepository.save(block));
    }

    @Transactional(readOnly = true)
    public List<AcademicBlockResponse> findAll() {
        return academicBlockRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AcademicBlockResponse findById(UUID id) {
        return toResponse(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<AcademicBlockResponse> findByStudent(UUID studentId) {
        return academicBlockRepository.findByStudentIdOrderByBlockedAtDesc(studentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AcademicBlockResponse> findByStatus(AcademicBlockStatus status) {
        return academicBlockRepository.findByStatusOrderByBlockedAtDesc(status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AcademicBlockResponse release(UUID id, AcademicBlockReleaseRequest request) {
        AcademicBlock block = findEntityById(id);

        if (block.getStatus() != AcademicBlockStatus.ACTIVE) {
            return toResponse(block);
        }

        User releasedBy = request.getReleasedByUserId() == null
                ? null
                : userRepository.findById(request.getReleasedByUserId())
                .orElseThrow(() -> new NotFoundException("Utilizador não encontrado."));

        block
                .setStatus(AcademicBlockStatus.RELEASED)
                .setReleasedBy(releasedBy)
                .setReleaseNote(request.getReleaseNote())
                .setReleasedAt(LocalDateTime.now());

        Student student = block.getStudent();
        List<AcademicBlock> activeBlocks = academicBlockRepository
                .findByStudentIdAndStatusOrderByBlockedAtDesc(student.getId(), AcademicBlockStatus.ACTIVE);

        if (activeBlocks.size() <= 1) {
            student
                    .setFinanciallyBlocked(false)
                    .setStatus(StudentStatus.ACTIVE)
                    .setBlockedReason(null);

            studentRepository.save(student);
        }

        return toResponse(academicBlockRepository.save(block));
    }

    public AcademicBlockResponse toResponse(AcademicBlock block) {
        Student student = block.getStudent();
        Charge charge = block.getCharge();
        User blockedBy = block.getBlockedBy();
        User releasedBy = block.getReleasedBy();

        return new AcademicBlockResponse()
                .setId(block.getId())
                .setStudentId(student != null ? student.getId() : null)
                .setStudentName(student != null ? student.getFullName() : null)
                .setStudentNumber(student != null ? student.getStudentNumber() : null)
                .setChargeId(charge != null ? charge.getId() : null)
                .setChargeCode(charge != null ? charge.getChargeCode() : null)
                .setBlockedService(block.getBlockedService())
                .setReason(block.getReason())
                .setStatus(block.getStatus())
                .setBlockedByUserId(blockedBy != null ? blockedBy.getId() : null)
                .setBlockedByName(blockedBy != null ? blockedBy.getFullName() : null)
                .setBlockedAt(block.getBlockedAt())
                .setReleasedByUserId(releasedBy != null ? releasedBy.getId() : null)
                .setReleasedByName(releasedBy != null ? releasedBy.getFullName() : null)
                .setReleasedAt(block.getReleasedAt())
                .setReleaseNote(block.getReleaseNote())
                .setCreatedAt(block.getCreatedAt())
                .setUpdatedAt(block.getUpdatedAt());
    }

    private AcademicBlock findEntityById(UUID id) {
        return academicBlockRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Bloqueio académico não encontrado."));
    }
}
