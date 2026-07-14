package com.secretariapay.api.service.whatsapp;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AcademicServicesWhatsappFlowAspectTest {

    private AcademicServicesWhatsappFlowService servicesFlow;
    private StudentFinancialStatementWhatsappService statementFlow;
    private AcademicServicesWhatsappFlowAspect aspect;

    @BeforeEach
    void setUp() {
        servicesFlow = mock(AcademicServicesWhatsappFlowService.class);
        statementFlow = mock(StudentFinancialStatementWhatsappService.class);
        aspect = new AcademicServicesWhatsappFlowAspect(servicesFlow, statementFlow);
    }

    @Test
    void devePriorizarSituacaoFinanceiraAtiva() throws Throwable {
        ProceedingJoinPoint joinPoint = join("244923000000", "text", "202301404");
        when(statementFlow.handleIfActive("244923000000", "text", "202301404"))
                .thenReturn(Optional.of("RESUMO SEPARADO"));

        Object result = aspect.routeAcademicServices(joinPoint);

        assertThat(result).isEqualTo(Optional.of("RESUMO SEPARADO"));
        verify(joinPoint, never()).proceed();
    }

    @Test
    void devePriorizarFluxoAcademicoAtivo() throws Throwable {
        ProceedingJoinPoint joinPoint = join("+244 923 000 000", "text", "1");
        when(statementFlow.handleIfActive("244923000000", "text", "1")).thenReturn(Optional.empty());
        when(servicesFlow.handleIfActive("244923000000", "text", "1"))
                .thenReturn(Optional.of("Serviço selecionado."));

        Object result = aspect.routeAcademicServices(joinPoint);

        assertThat(result).isEqualTo(Optional.of("Serviço selecionado."));
        verify(joinPoint, never()).proceed();
    }

    @Test
    void deveTrocarMenuLegadoPeloMenuInstitucionalSeparado() throws Throwable {
        ProceedingJoinPoint joinPoint = join("244923000000", "text", "menu");
        inactiveFlows("244923000000", "text", "menu");
        when(servicesFlow.isServiceIntent("menu")).thenReturn(false);
        when(joinPoint.proceed()).thenReturn(Optional.of("Como posso ajudar?\n[1] Propinas\n[7] Falar com a DCR"));

        Object result = aspect.routeAcademicServices(joinPoint);

        assertThat(result.toString())
                .contains("[2] Situação financeira")
                .contains("[3] Recibos e documentos")
                .contains("[4] Serviços académicos");
    }

    @Test
    void deveAbrirResumoSeparadoNaOpcaoDois() throws Throwable {
        registerMainMenuState();
        ProceedingJoinPoint option = join("244923000000", "interactive", "2");
        inactiveFlows("244923000000", "interactive", "2");
        when(servicesFlow.isServiceIntent("2")).thenReturn(false);
        when(statementFlow.startSummary("244923000000")).thenReturn("RESUMO SEPARADO");

        Object result = aspect.routeAcademicServices(option);

        assertThat(result).isEqualTo(Optional.of("RESUMO SEPARADO"));
        verify(option, never()).proceed();
    }

    @Test
    void deveAbrirDocumentosSeparadosNaOpcaoTres() throws Throwable {
        registerMainMenuState();
        ProceedingJoinPoint option = join("244923000000", "interactive", "3");
        inactiveFlows("244923000000", "interactive", "3");
        when(servicesFlow.isServiceIntent("3")).thenReturn(false);
        when(statementFlow.startDocuments("244923000000")).thenReturn("DOCUMENTOS SEPARADOS");

        Object result = aspect.routeAcademicServices(option);

        assertThat(result).isEqualTo(Optional.of("DOCUMENTOS SEPARADOS"));
        verify(option, never()).proceed();
    }

    @Test
    void deveAbrirServicosAoSelecionarOpcaoQuatro() throws Throwable {
        registerMainMenuState();
        ProceedingJoinPoint option = join("244923000000", "interactive", "4");
        inactiveFlows("244923000000", "interactive", "4");
        when(servicesFlow.isServiceIntent("4")).thenReturn(false);
        when(servicesFlow.start("244923000000")).thenReturn("LISTA DE SERVIÇOS");

        Object result = aspect.routeAcademicServices(option);

        assertThat(result).isEqualTo(Optional.of("LISTA DE SERVIÇOS"));
        verify(option, never()).proceed();
    }

    @Test
    void deveEncaminharServicosDiretosParaCatalogoOficial() throws Throwable {
        for (String intent : new String[]{"quero pagar matrícula", "preciso de declaração", "solicitar certificado", "pagar recurso"}) {
            ProceedingJoinPoint joinPoint = join("244923000000", "text", intent);
            inactiveFlows("244923000000", "text", intent);
            when(servicesFlow.isServiceIntent(intent)).thenReturn(false);
            when(servicesFlow.start("244923000000")).thenReturn("CATÁLOGO OFICIAL IMETRO");

            Object result = aspect.routeAcademicServices(joinPoint);

            assertThat(result).isEqualTo(Optional.of("CATÁLOGO OFICIAL IMETRO"));
            verify(joinPoint, never()).proceed();
        }
    }

    private void registerMainMenuState() throws Throwable {
        ProceedingJoinPoint menu = join("244923000000", "text", "menu");
        inactiveFlows("244923000000", "text", "menu");
        when(servicesFlow.isServiceIntent("menu")).thenReturn(false);
        when(menu.proceed()).thenReturn(Optional.of("Como posso ajudar?\n[1] Propinas\n[7] Falar com a DCR"));
        aspect.routeAcademicServices(menu);
    }

    private void inactiveFlows(String phone, String type, String message) {
        when(statementFlow.handleIfActive(phone.replaceAll("[^0-9]", ""), type, message)).thenReturn(Optional.empty());
        when(servicesFlow.handleIfActive(phone.replaceAll("[^0-9]", ""), type, message)).thenReturn(Optional.empty());
    }

    private ProceedingJoinPoint join(String phone, String type, String message) {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{phone, type, message});
        return joinPoint;
    }
}
