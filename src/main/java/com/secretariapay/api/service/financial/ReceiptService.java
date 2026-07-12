package com.secretariapay.api.service.financial;

import com.secretariapay.api.dto.financial.ReceiptResponse;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import com.secretariapay.api.entity.enums.financial.ReceiptStatus;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.entity.financial.Receipt;
import com.secretariapay.api.entity.operations.PaymentTransaction;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.financial.ChargeRepository;
import com.secretariapay.api.repository.financial.ReceiptRepository;
import com.secretariapay.api.repository.operations.PaymentTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ReceiptService {

    private static final String API_BASE_URL = "https://secretariapay-api.paixaoangola.com";

    private final ReceiptRepository receiptRepository;
    private final ChargeRepository chargeRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ReceiptAuthenticityService authenticityService;

    public ReceiptService(
            ReceiptRepository receiptRepository,
            ChargeRepository chargeRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            ReceiptAuthenticityService authenticityService
    ) {
        this.receiptRepository = receiptRepository;
        this.chargeRepository = chargeRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.authenticityService = authenticityService;
    }

    @Transactional
    public ReceiptResponse issueForCharge(UUID chargeId) {
        Charge charge = chargeRepository.findById(chargeId)
                .orElseThrow(() -> new NotFoundException("Cobrança não encontrada."));

        if (charge.getStatus() != ChargeStatus.PAID) {
            throw new IllegalArgumentException("Só é possível emitir recibo para cobrança paga.");
        }

        receiptRepository.findByChargeId(chargeId)
                .ifPresent(receipt -> {
                    throw new IllegalArgumentException("Já existe recibo emitido para esta cobrança.");
                });

        return issueNewReceipt(charge);
    }

    @Transactional
    public ReceiptResponse issueOrFindForCharge(UUID chargeId) {
        Charge charge = chargeRepository.findById(chargeId)
                .orElseThrow(() -> new NotFoundException("Cobrança não encontrada."));

        if (charge.getStatus() != ChargeStatus.PAID) {
            throw new IllegalArgumentException("Só é possível emitir recibo para cobrança paga.");
        }

        return receiptRepository.findByChargeId(chargeId)
                .map(this::toResponse)
                .orElseGet(() -> issueNewReceipt(charge));
    }

    @Transactional(readOnly = true)
    public List<ReceiptResponse> findAll() {
        return receiptRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReceiptResponse findById(UUID id) {
        return toResponse(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public ReceiptResponse findByCode(String receiptCode) {
        Receipt receipt = receiptRepository.findByReceiptCode(receiptCode)
                .orElseThrow(() -> new NotFoundException("Recibo não encontrado."));

        return toResponse(receipt);
    }

    @Transactional(readOnly = true)
    public ReceiptResponse validate(String receiptCode, String hash) {
        Receipt receipt = receiptRepository.findByReceiptCode(receiptCode)
                .orElseThrow(() -> new NotFoundException("Comprovativo não encontrado."));
        if (!authenticityService.matches(receipt, hash)) {
            throw new IllegalArgumentException("HASH de autenticação inválido.");
        }
        return toResponse(receipt);
    }

    @Transactional
    public ReceiptResponse cancel(UUID id) {
        Receipt receipt = findEntityById(id);

        receipt
                .setStatus(ReceiptStatus.CANCELLED)
                .setCancelledAt(LocalDateTime.now());

        return toResponse(receiptRepository.save(receipt));
    }

    public ReceiptResponse toResponse(Receipt receipt) {
        Charge charge = receipt.getCharge();
        PaymentTransaction transaction = charge != null && charge.getId() != null
                ? paymentTransactionRepository.findFirstByChargeIdOrderByCreatedAtDesc(charge.getId()).orElse(null)
                : null;

        String pdfUrl = receipt.getPdfUrl();
        if (receipt.getReceiptCode() != null && !receipt.getReceiptCode().isBlank()) {
            pdfUrl = buildPdfUrl(receipt.getReceiptCode());
        }

        String validationUrl = receipt.getReceiptCode() == null ? receipt.getValidationUrl()
                : buildValidationUrl(receipt.getReceiptCode()) + "/authentic?hash=" + authenticityService.hash(receipt);

        return new ReceiptResponse()
                .setId(receipt.getId())
                .setChargeId(charge != null ? charge.getId() : null)
                .setChargeCode(charge != null ? charge.getChargeCode() : null)
                .setStudentName(charge != null && charge.getStudent() != null ? charge.getStudent().getFullName() : null)
                .setStudentNumber(charge != null && charge.getStudent() != null ? charge.getStudent().getStudentNumber() : null)
                .setReferenceMonth(charge != null ? charge.getReferenceMonth() : null)
                .setDueDate(charge != null ? charge.getDueDate() : null)
                .setAmount(charge != null ? safeMoney(charge.getTotalAmount()) : BigDecimal.ZERO)
                .setBaseAmount(charge != null ? safeMoney(charge.getAmount()) : BigDecimal.ZERO)
                .setFineAmount(charge != null ? safeMoney(charge.getFineAmount()) : BigDecimal.ZERO)
                .setInterestAmount(charge != null ? safeMoney(charge.getInterestAmount()) : BigDecimal.ZERO)
                .setDiscountAmount(charge != null ? safeMoney(charge.getDiscountAmount()) : BigDecimal.ZERO)
                .setCurrency(charge != null && charge.getCurrency() != null ? charge.getCurrency() : "AOA")
                .setPaymentMethod(resolvePaymentMethod(transaction, charge))
                .setPaidAt(charge != null ? charge.getPaidAt() : null)
                .setReceiptCode(receipt.getReceiptCode())
                .setPdfUrl(pdfUrl)
                .setQrCodeUrl(receipt.getQrCodeUrl())
                .setValidationUrl(validationUrl)
                .setAuthenticityHash(authenticityService.hash(receipt))
                .setDigitalSignature("SECRETARIAPAY-SHA256-" + authenticityService.shortHash(receipt))
                .setStatus(receipt.getStatus())
                .setIssuedAt(receipt.getIssuedAt())
                .setCancelledAt(receipt.getCancelledAt())
                .setCreatedAt(receipt.getCreatedAt())
                .setUpdatedAt(receipt.getUpdatedAt());
    }

    private ReceiptResponse issueNewReceipt(Charge charge) {
        String receiptCode = generateReceiptCode();
        Receipt receipt = new Receipt()
                .setCharge(charge)
                .setReceiptCode(receiptCode)
                .setStatus(ReceiptStatus.VALID);

        Receipt savedReceipt = receiptRepository.save(receipt);
        String validationUrl = buildValidationUrl(savedReceipt.getReceiptCode()) + "/authentic?hash=" + authenticityService.hash(savedReceipt);
        savedReceipt.setValidationUrl(validationUrl);
        savedReceipt.setQrCodeUrl(validationUrl);
        savedReceipt.setPdfUrl(buildPdfUrl(savedReceipt.getReceiptCode()));

        return toResponse(receiptRepository.save(savedReceipt));
    }

    private Receipt findEntityById(UUID id) {
        return receiptRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Recibo não encontrado."));
    }

    private String generateReceiptCode() {
        String code;
        do {
            code = "RCT" + System.currentTimeMillis();
        } while (receiptRepository.existsByReceiptCode(code));
        return code;
    }

    private String buildPdfUrl(String receiptCode) {
        return API_BASE_URL + "/api/v1/public/receipts/" + receiptCode + "/pdf";
    }

    private String buildValidationUrl(String receiptCode) {
        return API_BASE_URL + "/api/v1/public/receipts/validate/" + receiptCode;
    }

    private BigDecimal safeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String resolvePaymentMethod(PaymentTransaction transaction, Charge charge) {
        if (transaction != null && transaction.getPaymentMethod() != null && !transaction.getPaymentMethod().isBlank()) {
            if ("INFINITEPAY_LINK".equalsIgnoreCase(transaction.getPaymentMethod())) {
                return "Pix/link InfinitePay";
            }
            return transaction.getPaymentMethod();
        }
        if (transaction != null && transaction.getProvider() != null && !transaction.getProvider().isBlank()) {
            return transaction.getProvider();
        }
        if (charge != null && charge.getDescription() != null && charge.getDescription().toLowerCase().contains("infinitepay")) {
            return "Pix/link InfinitePay";
        }
        return "Pagamento confirmado";
    }
}
