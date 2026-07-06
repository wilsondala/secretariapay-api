package com.secretariapay.api.service.financial;

import com.secretariapay.api.dto.financial.PaymentProofRequest;
import com.secretariapay.api.dto.financial.PaymentProofResponse;
import com.secretariapay.api.dto.financial.PaymentProofReviewRequest;
import com.secretariapay.api.entity.User;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import com.secretariapay.api.entity.enums.financial.PaymentProofStatus;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.entity.financial.PaymentProof;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.UserRepository;
import com.secretariapay.api.repository.financial.ChargeRepository;
import com.secretariapay.api.repository.financial.PaymentProofRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentProofService {

    private final PaymentProofRepository paymentProofRepository;
    private final ChargeRepository chargeRepository;
    private final UserRepository userRepository;

    public PaymentProofService(
            PaymentProofRepository paymentProofRepository,
            ChargeRepository chargeRepository,
            UserRepository userRepository
    ) {
        this.paymentProofRepository = paymentProofRepository;
        this.chargeRepository = chargeRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PaymentProofResponse create(PaymentProofRequest request) {
        Charge charge = chargeRepository.findById(request.getChargeId())
                .orElseThrow(() -> new NotFoundException("Cobrança não encontrada."));

        PaymentProof proof = new PaymentProof()
                .setCharge(charge)
                .setFileUrl(request.getFileUrl())
                .setFileName(request.getFileName())
                .setMimeType(request.getMimeType())
                .setSubmittedByPhone(request.getSubmittedByPhone())
                .setStatus(PaymentProofStatus.PENDING_REVIEW);

        return toResponse(paymentProofRepository.save(proof));
    }

    @Transactional(readOnly = true)
    public List<PaymentProofResponse> findAll() {
        return paymentProofRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentProofResponse findById(UUID id) {
        return toResponse(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<PaymentProofResponse> findByStatus(PaymentProofStatus status) {
        return paymentProofRepository.findByStatusOrderBySubmittedAtAsc(status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentProofResponse> findByCharge(UUID chargeId) {
        return paymentProofRepository.findByChargeIdOrderBySubmittedAtDesc(chargeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PaymentProofResponse approve(UUID id, PaymentProofReviewRequest request) {
        PaymentProof proof = findEntityById(id);
        User reviewer = resolveReviewer(request);
        LocalDateTime now = LocalDateTime.now();

        proof
                .setStatus(PaymentProofStatus.APPROVED)
                .setReviewedBy(reviewer)
                .setReviewNote(resolveReviewNote(request, "Aprovado pela DCR no painel."))
                .setReviewedAt(now);

        Charge charge = proof.getCharge();
        charge
                .setStatus(ChargeStatus.PAID)
                .setPaidAt(now);

        chargeRepository.save(charge);

        return toResponse(paymentProofRepository.save(proof));
    }

    @Transactional
    public PaymentProofResponse reject(UUID id, PaymentProofReviewRequest request) {
        PaymentProof proof = findEntityById(id);
        User reviewer = resolveReviewer(request);

        proof
                .setStatus(PaymentProofStatus.REJECTED)
                .setReviewedBy(reviewer)
                .setReviewNote(resolveReviewNote(request, "Comprovativo rejeitado pela DCR."))
                .setReviewedAt(LocalDateTime.now());

        return toResponse(paymentProofRepository.save(proof));
    }

    public PaymentProofResponse toResponse(PaymentProof proof) {
        Charge charge = proof.getCharge();
        User reviewer = proof.getReviewedBy();

        return new PaymentProofResponse()
                .setId(proof.getId())
                .setChargeId(charge != null ? charge.getId() : null)
                .setChargeCode(charge != null ? charge.getChargeCode() : null)
                .setStudentName(charge != null && charge.getStudent() != null ? charge.getStudent().getFullName() : null)
                .setFileUrl(proof.getFileUrl())
                .setFileName(proof.getFileName())
                .setMimeType(proof.getMimeType())
                .setSubmittedByPhone(proof.getSubmittedByPhone())
                .setSubmittedAt(proof.getSubmittedAt())
                .setStatus(proof.getStatus())
                .setReviewedByUserId(reviewer != null ? reviewer.getId() : null)
                .setReviewedByName(reviewer != null ? reviewer.getFullName() : null)
                .setReviewNote(proof.getReviewNote())
                .setReviewedAt(proof.getReviewedAt())
                .setCreatedAt(proof.getCreatedAt())
                .setUpdatedAt(proof.getUpdatedAt());
    }

    private PaymentProof findEntityById(UUID id) {
        return paymentProofRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Comprovativo não encontrado."));
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utilizador não encontrado."));
    }

    private User resolveReviewer(PaymentProofReviewRequest request) {
        UUID reviewedByUserId = request != null ? request.getReviewedByUserId() : null;

        if (reviewedByUserId != null) {
            return findUser(reviewedByUserId);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();

            if (username != null && !username.isBlank() && !"anonymousUser".equalsIgnoreCase(username)) {
                return userRepository.findByEmailIgnoreCase(username)
                        .orElseThrow(() -> new IllegalArgumentException("Utilizador autenticado não encontrado para validar o comprovativo."));
            }
        }

        throw new IllegalArgumentException("Não foi possível identificar o utilizador responsável pela validação do comprovativo.");
    }

    private String resolveReviewNote(PaymentProofReviewRequest request, String fallback) {
        String reviewNote = request != null ? request.getReviewNote() : null;

        if (reviewNote == null || reviewNote.isBlank()) {
            return fallback;
        }

        return reviewNote.trim();
    }
}
