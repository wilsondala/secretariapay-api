package com.secretariapay.api.service.academic;

import com.secretariapay.api.entity.academic.AcademicClass;
import com.secretariapay.api.entity.academic.AcademicDocumentRequest;
import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.academic.Student;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AcademicDocumentPdfServiceTest {

    @Test
    void deveGerarPdfDaDeclaracaoDemo() {
        Course course = new Course().setName("Gestão Financeira");
        AcademicClass academicClass = new AcademicClass()
                .setName("GF-2026")
                .setAcademicYear("2026")
                .setCourse(course);
        Student student = new Student()
                .setStudentNumber("202301404")
                .setFullName("Wilson dos Santos Kahango Dala")
                .setDocumentNumber("001058899UE035")
                .setAcademicClass(academicClass);

        AcademicDocumentRequest request = new AcademicDocumentRequest()
                .setDocumentCode("IMT-DECL-SN-20260713-202301404")
                .setStudent(student)
                .setServiceCode("DECLARATION_WITHOUT_GRADES")
                .setDocumentType("SIMPLE_DECLARATION")
                .setStatus("SIGNED")
                .setPurpose("Comprovação académica")
                .setDeclarationText("O IMETRO declara que o estudante se encontra regularmente matriculado no ano académico de 2026.")
                .setSignatoryName("Zakeu António Zengo")
                .setSignatoryRole("Presidente da Instituição")
                .setSignatureMethod("SECRETARIAPAY_DEMO_ELECTRONIC_SIGNATURE")
                .setDocumentHash("9D6A149E00AA22BB33CC44DD55EE66FF77889900AABBCCDDEEFF001122334455")
                .setSignedAt(LocalDateTime.of(2026, 7, 13, 15, 30))
                .setIssuedAt(LocalDateTime.of(2026, 7, 13, 15, 0))
                .setDemoMode(true)
                .setVersionNumber(1);
        request.prePersist();

        byte[] pdf = new AcademicDocumentPdfService("https://secretariapay-api.paixaoangola.com").generate(request);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        assertThat(pdf.length).isGreaterThan(5_000);
    }
}
