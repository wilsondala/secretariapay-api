package com.secretariapay.api.service.academic;

import com.secretariapay.api.dto.academic.AcademicDocumentDto;
import com.secretariapay.api.entity.academic.AcademicClass;
import com.secretariapay.api.entity.academic.AcademicDocumentRequest;
import com.secretariapay.api.entity.academic.Course;
import com.secretariapay.api.entity.academic.Student;
import com.secretariapay.api.exception.NotFoundException;
import com.secretariapay.api.repository.academic.AcademicDocumentRequestRepository;
import com.secretariapay.api.repository.academic.StudentRepository;
import com.secretariapay.api.service.whatsapp.WhatsAppCloudApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AcademicDocumentServiceTest {

    private AcademicDocumentRequestRepository repository;
    private StudentRepository studentRepository;
    private AcademicDocumentService service;
    private Student student;

    @BeforeEach
    void setUp() {
        repository = mock(AcademicDocumentRequestRepository.class);
        studentRepository = mock(StudentRepository.class);
        AcademicDocumentPdfService pdfService = mock(AcademicDocumentPdfService.class);
        WhatsAppCloudApiClient whatsAppCloudApiClient = mock(WhatsAppCloudApiClient.class);
        service = new AcademicDocumentService(repository, studentRepository, pdfService, whatsAppCloudApiClient,
                "https://secretariapay-api.paixaoangola.com");

        Course course = new Course().setName("Gestão Financeira");
        AcademicClass academicClass = new AcademicClass().setName("GF-2026").setAcademicYear("2026").setCourse(course);
        student = new Student().setStudentNumber("202301404").setFullName("Wilson dos Santos Kahango Dala")
                .setDocumentNumber("001058899UE035").setAcademicClass(academicClass);

        when(repository.save(any(AcademicDocumentRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void deveCriarDeclaracaoDemoEmRascunho() {
        when(studentRepository.findByStudentNumber("202301404")).thenReturn(Optional.of(student));
        AcademicDocumentDto.Response response = service.createDemoDeclaration(
                new AcademicDocumentDto.CreateDemoRequest("202301404", "Comprovação académica", null));
        assertThat(response.status()).isEqualTo("DRAFT");
        assertThat(response.demoMode()).isTrue();
        assertThat(response.studentNumber()).isEqualTo("202301404");
        assertThat(response.signatoryName()).isEqualTo("Zakeu António Zengo");
        assertThat(response.signatoryRole()).isEqualTo("Presidente da Instituição");
        assertThat(response.declarationText()).contains("Wilson dos Santos Kahango Dala", "Gestão Financeira");
    }

    @Test
    void naoDeveAssinarAntesDeEstarPronto() {
        AcademicDocumentRequest document = document("DRAFT");
        when(repository.findById(any(UUID.class))).thenReturn(Optional.of(document));
        assertThatThrownBy(() -> service.signDemo(UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("pronto para assinatura");
    }

    @Test
    void deveAssinarECalcularHash() {
        AcademicDocumentRequest document = document("READY_FOR_SIGNATURE");
        when(repository.findById(any(UUID.class))).thenReturn(Optional.of(document));
        AcademicDocumentDto.Response response = service.signDemo(UUID.randomUUID());
        assertThat(response.status()).isEqualTo("SIGNED");
        assertThat(response.signatureMethod()).isEqualTo("SECRETARIAPAY_DEMO_ELECTRONIC_SIGNATURE");
        assertThat(response.documentHash()).hasSize(64);
        assertThat(response.signedAt()).isNotNull();
    }

    @Test
    void naoDeveEditarDocumentoAssinado() {
        AcademicDocumentRequest document = document("SIGNED");
        when(repository.findById(any(UUID.class))).thenReturn(Optional.of(document));
        assertThatThrownBy(() -> service.update(UUID.randomUUID(),
                new AcademicDocumentDto.UpdateRequest("Nova finalidade", "Novo texto")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("não pode ser alterado");
    }

    @Test
    void naoDeveExporPdfPublicoAntesDaAssinatura() {
        AcademicDocumentRequest document = document("DRAFT");
        when(repository.findByDocumentCode(document.getDocumentCode())).thenReturn(Optional.of(document));
        assertThatThrownBy(() -> service.generatePublicPdf(document.getDocumentCode()))
                .isInstanceOf(NotFoundException.class).hasMessageContaining("assinado não encontrado");
    }

    private AcademicDocumentRequest document(String status) {
        AcademicDocumentRequest document = new AcademicDocumentRequest()
                .setDocumentCode("IMT-DECL-SN-20260713-202301404").setStudent(student)
                .setServiceCode("DECLARATION_WITHOUT_GRADES").setDocumentType("SIMPLE_DECLARATION")
                .setStatus(status).setPurpose("Comprovação académica")
                .setDeclarationText("Declaração de teste para o estudante.")
                .setSignatoryName("Zakeu António Zengo").setSignatoryRole("Presidente da Instituição")
                .setDemoMode(true).setVersionNumber(1);
        document.prePersist();
        return document;
    }
}
