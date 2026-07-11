package com.secretariapay.api.service.financial;

import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.entity.financial.Charge;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.financial.ChargeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;

@Service
public class OfficialDocumentFilenameService {

    private final ChargeRepository chargeRepository;

    public OfficialDocumentFilenameService(ChargeRepository chargeRepository) {
        this.chargeRepository = chargeRepository;
    }

    @Transactional(readOnly = true)
    public String paymentGuideFilenameByChargeCode(String chargeCode) {
        Charge charge = chargeRepository.findByChargeCode(chargeCode)
                .orElseThrow(() -> new NotFoundException("Cobrança não encontrada."));

        Student student = charge.getStudent();
        String studentNumber = student != null ? student.getStudentNumber() : null;
        String reference = firstNotBlank(charge.getReferenceMonth(), "Sem_Referencia");
        String guideCode = firstNotBlank(charge.getChargeCode(), "Sem_Codigo");

        return "Guia_Pagamento_Academico_"
                + filenamePart(studentNumber, "Sem_Matricula") + "_"
                + filenamePart(reference, "Sem_Referencia") + "_"
                + filenamePart(guideCode, "Sem_Codigo")
                + ".pdf";
    }

    public String receiptFilename(String studentNumber, String receiptCode) {
        return "Comprovativo_Pagamentos_"
                + filenamePart(studentNumber, "Sem_Matricula") + "_"
                + filenamePart(receiptCode, "Sem_Codigo")
                + ".pdf";
    }

    private String filenamePart(String value, String fallback) {
        String clean = firstNotBlank(value, fallback);
        clean = Normalizer.normalize(clean, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^A-Za-z0-9._-]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");
        return clean.isBlank() ? fallback : clean;
    }

    private String firstNotBlank(String value, String fallback) {
        return value == null || value.trim().isBlank() ? fallback : value.trim();
    }
}
