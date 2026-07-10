package com.secretariapay.api.service.payment;

import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.enums.financial.ChargeStatus;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.entity.financial.Receipt;
import com.secretariapay.api.repository.financial.ChargeRepository;
import com.secretariapay.api.repository.financial.ReceiptRepository;
import com.secretariapay.api.service.whatsapp.WhatsAppCloudApiClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class InfinitePayReconciliationService {

    private static final String API_BASE_URL = "https://secretariapay-api.paixaoangola.com";

    private final ReceiptRepository receiptRepository;
    private final ChargeRepository chargeRepository;
    private final WhatsAppCloudApiClient whatsAppCloudApiClient;

    public InfinitePayReconciliationService(
            ReceiptRepository receiptRepository,
            ChargeRepository chargeRepository,
            WhatsAppCloudApiClient whatsAppCloudApiClient
    ) {
        this.receiptRepository = receiptRepository;
        this.chargeRepository = chargeRepository;
        this.whatsAppCloudApiClient = whatsAppCloudApiClient;
    }

    @Transactional
    public ReconciliationResult reconcileAfterInfinitePay(String orderNsu, String receiptCodes) {
        List<String> codes = splitReceiptCodes(receiptCodes);
        if (codes.isEmpty()) {
            return new ReconciliationResult(false, "Nenhum bordereau informado para conciliação InfinitePay.");
        }

        List<String> adjusted = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        for (String receiptCode : codes) {
            Optional<Receipt> receiptOptional = receiptRepository.findByReceiptCode(receiptCode);
            if (receiptOptional.isEmpty()) {
                skipped.add(receiptCode + ": recibo não encontrado");
                continue;
            }

            Receipt receipt = receiptOptional.get();
            Charge generatedCharge = receipt.getCharge();
            if (generatedCharge == null || generatedCharge.getStudent() == null) {
                skipped.add(receiptCode + ": cobrança sem estudante");
                continue;
            }

            Optional<Charge> realChargeOptional = findRealOpenCharge(generatedCharge);
            if (realChargeOptional.isEmpty()) {
                skipped.add(receiptCode + ": nenhuma mensalidade real aberta encontrada");
                continue;
            }

            Charge realCharge = realChargeOptional.get();
            if (receiptRepository.findByChargeId(realCharge.getId()).isPresent()) {
                skipped.add(receiptCode + ": mensalidade real já possui bordereau");
                continue;
            }

            LocalDateTime paidAt = generatedCharge.getPaidAt() == null ? LocalDateTime.now() : generatedCharge.getPaidAt();
            realCharge
                    .setStatus(ChargeStatus.PAID)
                    .setPaidAt(paidAt);
            chargeRepository.save(realCharge);

            receipt.setCharge(realCharge);
            receiptRepository.save(receipt);

            generatedCharge
                    .setStatus(ChargeStatus.CANCELLED)
                    .setCancelledAt(LocalDateTime.now())
                    .setDescription(firstNonBlank(generatedCharge.getDescription(), "Cobrança InfinitePay") + " | Corrigida para cobrança real " + realCharge.getChargeCode());
            chargeRepository.save(generatedCharge);

            adjusted.add(receiptCode + " -> " + realCharge.getReferenceMonth() + " (" + money(realCharge.getTotalAmount()) + ")");
            notifyStudent(orderNsu, receipt, realCharge);
        }

        if (adjusted.isEmpty()) {
            return new ReconciliationResult(false, "Conciliação InfinitePay não alterou cobranças. " + String.join("; ", skipped));
        }
        return new ReconciliationResult(true, "Conciliação InfinitePay aplicada: " + String.join("; ", adjusted));
    }

    private Optional<Charge> findRealOpenCharge(Charge generatedCharge) {
        Student student = generatedCharge.getStudent();
        UUID generatedId = generatedCharge.getId();
        List<Charge> openCharges = chargeRepository.findByStudentIdOrderByDueDateDesc(student.getId()).stream()
                .filter(charge -> generatedId == null || !generatedId.equals(charge.getId()))
                .filter(this::isOpen)
                .sorted(Comparator.comparing(Charge::getDueDate))
                .toList();

        if (openCharges.isEmpty()) return Optional.empty();

        String generatedReference = safe(generatedCharge.getReferenceMonth());
        Optional<Charge> sameReference = openCharges.stream()
                .filter(charge -> safe(charge.getReferenceMonth()).equalsIgnoreCase(generatedReference))
                .findFirst();
        return sameReference.or(() -> Optional.of(openCharges.get(0)));
    }

    private boolean isOpen(Charge charge) {
        if (charge == null || charge.getStatus() == null) return false;
        return charge.getStatus() != ChargeStatus.PAID && charge.getStatus() != ChargeStatus.CANCELLED;
    }

    private void notifyStudent(String orderNsu, Receipt receipt, Charge realCharge) {
        Student student = realCharge.getStudent();
        String phone = firstNonBlank(student.getWhatsapp(), student.getPhone(), student.getGuardianPhone());
        if (phone.isBlank()) return;

        String pdfUrl = firstNonBlank(receipt.getPdfUrl(), API_BASE_URL + "/api/v1/public/receipts/" + receipt.getReceiptCode() + "/pdf");
        String caption = ("""
                SecretáriaPay Académico: bordereau corrigido e vinculado à mensalidade real.

                Bordereau: %s
                Estudante: %s
                Matrícula: %s
                Mês pago: %s
                Valor real baixado: %s
                Código InfinitePay: %s
                """).formatted(
                receipt.getReceiptCode(),
                student.getFullName(),
                student.getStudentNumber(),
                realCharge.getReferenceMonth(),
                money(realCharge.getTotalAmount()),
                safe(orderNsu)
        ).trim();

        whatsAppCloudApiClient.sendDocumentByLink(phone, pdfUrl, "bordereau-" + receipt.getReceiptCode() + ".pdf", caption);
        whatsAppCloudApiClient.sendText(phone, ("""
                ✅ Ajuste automático aplicado.

                O pagamento InfinitePay foi vinculado à mensalidade real do sistema.

                Mês pago: %s
                Valor real baixado: %s
                Bordereau: %s

                O lançamento antigo de teste foi cancelado para não duplicar saldo no painel.
                """).formatted(realCharge.getReferenceMonth(), money(realCharge.getTotalAmount()), receipt.getReceiptCode()).trim());
    }

    private List<String> splitReceiptCodes(String receiptCodes) {
        if (receiptCodes == null || receiptCodes.isBlank()) return List.of();
        String[] parts = receiptCodes.split(",");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String code = safe(part);
            if (!code.isBlank()) result.add(code);
        }
        return result;
    }

    private String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isBlank()) return value.trim();
        }
        return "";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String money(BigDecimal value) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        return String.format(Locale.forLanguageTag("pt-AO"), "%,.2f", safeValue)
                .replace(',', '#')
                .replace('.', ',')
                .replace('#', '.') + " Kz";
    }

    public record ReconciliationResult(boolean adjusted, String message) {}
}
