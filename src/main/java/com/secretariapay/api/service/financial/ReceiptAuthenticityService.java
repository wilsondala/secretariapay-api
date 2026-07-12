package com.secretariapay.api.service.financial;

import com.secretariapay.api.entity.financial.Receipt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class ReceiptAuthenticityService {

    private final String secret;

    public ReceiptAuthenticityService(@Value("${secretariapay.documents.signing-secret:secretariapay-academico}") String secret) {
        this.secret = secret;
    }

    public String hash(Receipt receipt) {
        String chargeId = receipt.getCharge() == null || receipt.getCharge().getId() == null ? "" : receipt.getCharge().getId().toString();
        String issuedAt = receipt.getIssuedAt() == null ? "" : receipt.getIssuedAt().toString();
        return sha256(receipt.getReceiptCode() + "|" + chargeId + "|" + issuedAt + "|" + secret);
    }

    public boolean matches(Receipt receipt, String candidate) {
        if (candidate == null || candidate.isBlank()) return false;
        return MessageDigest.isEqual(hash(receipt).getBytes(StandardCharsets.UTF_8), candidate.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    public String shortHash(Receipt receipt) {
        return hash(receipt).substring(0, 16).toUpperCase();
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível assinar digitalmente o comprovativo.", exception);
        }
    }
}
