package com.secretariapay.api.service.financial;

import com.secretariapay.api.dto.financial.ReceiptResponse;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import com.secretariapay.api.entity.enums.financial.ReceiptStatus;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.entity.financial.Receipt;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.financial.ChargeRepository;
import com.secretariapay.api.repository.financial.ReceiptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final ChargeRepository chargeRepository;

    public ReceiptService(
            ReceiptRepository receiptRepository,
            ChargeRepository chargeRepository
    ) {
        this.receiptRepository = receiptRepository;
        this.chargeRepository = chargeRepository;
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

        String receiptCode = generateReceiptCode();
        String validationUrl = "https://secretariapay-api.paixaoangola.com/api/v1/public/receipts/validate/" + receiptCode;
        String qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=" + validationUrl;

        Receipt receipt = new Receipt()
                .setCharge(charge)
                .setReceiptCode(receiptCode)
                .setValidationUrl(validationUrl)
                .setQrCodeUrl(qrCodeUrl)
                .setStatus(ReceiptStatus.VALID);

        return toResponse(receiptRepository.save(receipt));
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

        return new ReceiptResponse()
                .setId(receipt.getId())
                .setChargeId(charge != null ? charge.getId() : null)
                .setChargeCode(charge != null ? charge.getChargeCode() : null)
                .setStudentName(charge != null && charge.getStudent() != null ? charge.getStudent().getFullName() : null)
                .setReceiptCode(receipt.getReceiptCode())
                .setPdfUrl(receipt.getPdfUrl())
                .setQrCodeUrl(receipt.getQrCodeUrl())
                .setValidationUrl(receipt.getValidationUrl())
                .setStatus(receipt.getStatus())
                .setIssuedAt(receipt.getIssuedAt())
                .setCancelledAt(receipt.getCancelledAt())
                .setCreatedAt(receipt.getCreatedAt())
                .setUpdatedAt(receipt.getUpdatedAt());
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
}
