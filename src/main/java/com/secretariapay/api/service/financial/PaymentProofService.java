package com.secretariapay.api.service.financial;

import com.secretariapay.api.dto.financial.PaymentProofRequest;
import com.secretariapay.api.dto.financial.PaymentProofResponse;
import com.secretariapay.api.dto.financial.PaymentProofReviewRequest;
import com.secretariapay.api.dto.financial.ReceiptResponse;
import com.secretariapay.api.dto.whatsapp.WhatsAppCloudSendResult;
import com.secretariapay.api.entity.User;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import com.secretariapay.api.entity.enums.financial.PaymentProofStatus;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.entity.financial.PaymentProof;
import com.secretariapay.api.entity.financial.Receipt;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.UserRepository;
import com.secretariapay.api.repository.financial.ChargeRepository;
import com.secretariapay.api.repository.financial.PaymentProofRepository;
import com.secretariapay.api.repository.financial.ReceiptRepository;
import com.secretariapay.api.service.whatsapp.WhatsAppCloudApiClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentProofService {

    private final PaymentProofRepository paymentProofRepository;
    private final ChargeRepository chargeRepository;
    private final UserRepository userRepository;
    private final ReceiptRepository receiptRepository;
    private final ReceiptService receiptService;
    private final WhatsAppCloudApiClient whatsAppCloudApiClient;
    private final ChargeClassificationService classificationService;

    public PaymentProofService(
            PaymentProofRepository paymentProofRepository,
            ChargeRepository chargeRepository,
            UserRepository userRepository,
            ReceiptRepository receiptRepository,
            ReceiptService receiptService,
            WhatsAppCloudApiClient whatsAppCloudApiClient,
            ChargeClassificationService classificationService
    ) {
        this.paymentProofRepository = paymentProofRepository;
        this.chargeRepository = chargeRepository;
        this.userRepository = userRepository;
        this.receiptRepository = receiptRepository;
        this.receiptService = receiptService;
        this.whatsAppCloudApiClient = whatsAppCloudApiClient;
        this.classificationService = classificationService;
    }

    @Transactional
    public PaymentProofResponse create(PaymentProofRequest request) {
        Charge charge = chargeRepository.findById(request.getChargeId())
                .orElseThrow(() -> new NotFoundException("Cobrança não encontrada."));
        classificationService.classify(charge);
        chargeRepository.save(charge);

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
        return paymentProofRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PaymentProofResponse findById(UUID id) {
        return toResponse(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<PaymentProofResponse> findByStatus(PaymentProofStatus status) {
        return paymentProofRepository.findByStatusOrderBySubmittedAtAsc(status).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentProofResponse> findByCharge(UUID chargeId) {
        return paymentProofRepository.findByChargeIdOrderBySubmittedAtDesc(chargeId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public PaymentProofResponse approve(UUID id, PaymentProofReviewRequest request) {
        PaymentProof proof = findEntityById(id);
        User reviewer = resolveReviewer(request);
        LocalDateTime now = LocalDateTime.now();

        proof.setStatus(PaymentProofStatus.APPROVED)
                .setReviewedBy(reviewer)
                .setReviewNote(resolveReviewNote(request, "Aprovado pela DCR no painel."))
                .setReviewedAt(now);

        Charge charge = proof.getCharge();
        classificationService.classify(charge);
        charge.setStatus(ChargeStatus.PAID).setPaidAt(now);
        chargeRepository.save(charge);

        ReceiptResponse receipt = receiptService.issueOrFindForCharge(charge.getId());
        sendReceiptDocument(charge, receipt);
        return enrichWithReceipt(toResponse(paymentProofRepository.save(proof)), receipt);
    }

    @Transactional
    public PaymentProofResponse reject(UUID id, PaymentProofReviewRequest request) {
        PaymentProof proof = findEntityById(id);
        User reviewer = resolveReviewer(request);
        proof.setStatus(PaymentProofStatus.REJECTED)
                .setReviewedBy(reviewer)
                .setReviewNote(resolveReviewNote(request, "Comprovativo rejeitado pela DCR."))
                .setReviewedAt(LocalDateTime.now());
        return toResponse(paymentProofRepository.save(proof));
    }

    public PaymentProofResponse toResponse(PaymentProof proof) {
        Charge charge = proof.getCharge();
        User reviewer = proof.getReviewedBy();
        PaymentProofResponse response = new PaymentProofResponse()
                .setId(proof.getId())
                .setChargeId(charge != null ? charge.getId() : null)
                .setChargeCode(charge != null ? charge.getChargeCode() : null)
                .setChargeCategory(charge != null ? classificationService.resolveCategory(charge) : null)
                .setServiceCode(charge != null ? classificationService.resolveServiceCode(charge) : null)
                .setChargeDescription(charge != null ? charge.getDescription() : null)
                .setReferenceMonth(charge != null ? charge.getReferenceMonth() : null)
                .setAmount(charge != null ? charge.getTotalAmount() : BigDecimal.ZERO)
                .setCurrency(charge != null ? charge.getCurrency() : "AOA")
                .setStudentName(charge != null && charge.getStudent() != null ? charge.getStudent().getFullName() : null)
                .setStudentNumber(charge != null && charge.getStudent() != null ? charge.getStudent().getStudentNumber() : null)
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

        if (charge != null && charge.getId() != null) {
            Optional<Receipt> receipt = receiptRepository.findByChargeId(charge.getId());
            receipt.ifPresent(value -> enrichWithReceipt(response, receiptService.toResponse(value)));
        }
        return response;
    }

    private PaymentProofResponse enrichWithReceipt(PaymentProofResponse response, ReceiptResponse receipt) {
        if (response == null || receipt == null) return response;
        return response.setReceiptCode(receipt.getReceiptCode())
                .setReceiptPdfUrl(receipt.getPdfUrl())
                .setReceiptValidationUrl(receipt.getValidationUrl());
    }

    private void sendReceiptDocument(Charge charge, ReceiptResponse receipt) {
        if (charge == null || receipt == null) return;
        Student student = charge.getStudent();
        if (student == null) return;

        String recipientPhone = firstNonBlank(student.getWhatsapp(), student.getPhone(), proofSafePhoneFallback(charge));
        if (recipientPhone.isBlank()) return;
        String pdfUrl = firstNonBlank(receipt.getPdfUrl(), receipt.getValidationUrl());
        if (pdfUrl.isBlank()) return;

        String categoryLabel = classificationService.isTuition(charge) ? "Propina" : "Serviço académico";
        String reference = classificationService.isTuition(charge)
                ? firstNonBlank(charge.getReferenceMonth(), charge.getDescription())
                : firstNonBlank(charge.getDescription(), charge.getReferenceMonth());
        String fileName = "recibo-secretariapay-" + firstNonBlank(receipt.getReceiptCode(), charge.getChargeCode(), "pagamento") + ".pdf";
        String caption = """
                ✅ Pagamento confirmado pela DCR / IMETRO.

                Estudante: %s
                Matrícula: %s
                Categoria: %s
                Referência: %s
                Valor: %s
                Recibo: %s

                O recibo institucional segue em PDF.
                Validação pública:
                %s

                SecretáriaPay Académico
                """.formatted(
                safe(student.getFullName()),
                firstNonBlank(student.getStudentNumber(), "-"),
                categoryLabel,
                reference,
                formatMoney(charge.getTotalAmount(), charge.getCurrency()),
                firstNonBlank(receipt.getReceiptCode(), "-"),
                firstNonBlank(receipt.getValidationUrl(), pdfUrl)
        ).trim();

        WhatsAppCloudSendResult result = whatsAppCloudApiClient.sendDocumentByLink(recipientPhone, pdfUrl, fileName, caption);
        if (result == null || !result.isSuccess()) {
            whatsAppCloudApiClient.sendText(recipientPhone, caption + "\n\nLink do recibo PDF:\n" + pdfUrl);
        }
    }

    private String proofSafePhoneFallback(Charge charge) {
        if (charge == null || charge.getId() == null) return "";
        return paymentProofRepository.findByChargeIdOrderBySubmittedAtDesc(charge.getId()).stream()
                .map(PaymentProof::getSubmittedByPhone)
                .filter(phone -> phone != null && !phone.isBlank())
                .findFirst()
                .orElse("");
    }

    private PaymentProof findEntityById(UUID id) {
        return paymentProofRepository.findById(id).orElseThrow(() -> new NotFoundException("Comprovativo não encontrado."));
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Utilizador não encontrado."));
    }

    private User resolveReviewer(PaymentProofReviewRequest request) {
        UUID reviewedByUserId = request != null ? request.getReviewedByUserId() : null;
        if (reviewedByUserId != null) return findUser(reviewedByUserId);

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
        return reviewNote == null || reviewNote.isBlank() ? fallback : reviewNote.trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) if (value != null && !value.isBlank()) return value.trim();
        return "";
    }

    private String safe(String value) { return value == null ? "" : value; }

    private String formatMoney(BigDecimal amount, String currency) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-AO"));
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);
        return formatter.format(safeAmount) + " " + firstNonBlank(currency, "Kz");
    }
}
