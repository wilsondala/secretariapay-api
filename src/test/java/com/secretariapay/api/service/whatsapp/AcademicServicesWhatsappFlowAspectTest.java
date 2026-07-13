package com.secretariapay.api.service.whatsapp;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AcademicServicesWhatsappFlowAspectTest {

    @Test
    void devePriorizarFluxoAcademicoAtivo() throws Throwable {
        AcademicServicesWhatsappFlowService flow = mock(AcademicServicesWhatsappFlowService.class);
        AcademicServicesWhatsappFlowAspect aspect = new AcademicServicesWhatsappFlowAspect(flow);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

        when(joinPoint.getArgs()).thenReturn(new Object[]{"+244 923 000 000", "text", "1"});
        when(flow.handleIfActive("244923000000", "text", "1"))
                .thenReturn(Optional.of("Serviço selecionado."));

        Object result = aspect.routeAcademicServices(joinPoint);

        assertThat(result).isEqualTo(Optional.of("Serviço selecionado."));
        verify(joinPoint, never()).proceed();
    }

    @Test
    void deveTrocarMenuLegadoPeloMenuInstitucional() throws Throwable {
        AcademicServicesWhatsappFlowService flow = mock(AcademicServicesWhatsappFlowService.class);
        AcademicServicesWhatsappFlowAspect aspect = new AcademicServicesWhatsappFlowAspect(flow);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

        when(joinPoint.getArgs()).thenReturn(new Object[]{"244923000000", "text", "menu"});
        when(flow.handleIfActive("244923000000", "text", "menu")).thenReturn(Optional.empty());
        when(flow.isServiceIntent("menu")).thenReturn(false);
        when(flow.buildMainMenu()).thenReturn("NOVO MENU");
        when(joinPoint.proceed()).thenReturn(Optional.of("""
                Como posso ajudar?
                [1] Propinas
                [7] Falar com a DCR
                """));

        Object result = aspect.routeAcademicServices(joinPoint);

        assertThat(result).isEqualTo(Optional.of("NOVO MENU"));
    }

    @Test
    void deveAbrirServicosAoSelecionarOpcaoQuatroDoMenu() throws Throwable {
        AcademicServicesWhatsappFlowService flow = mock(AcademicServicesWhatsappFlowService.class);
        AcademicServicesWhatsappFlowAspect aspect = new AcademicServicesWhatsappFlowAspect(flow);

        ProceedingJoinPoint menuJoinPoint = mock(ProceedingJoinPoint.class);
        when(menuJoinPoint.getArgs()).thenReturn(new Object[]{"244923000000", "text", "menu"});
        when(flow.handleIfActive("244923000000", "text", "menu")).thenReturn(Optional.empty());
        when(flow.isServiceIntent("menu")).thenReturn(false);
        when(flow.buildMainMenu()).thenReturn("NOVO MENU");
        when(menuJoinPoint.proceed()).thenReturn(Optional.of("""
                Como posso ajudar?
                [1] Propinas
                [7] Falar com a DCR
                """));
        aspect.routeAcademicServices(menuJoinPoint);

        ProceedingJoinPoint optionJoinPoint = mock(ProceedingJoinPoint.class);
        when(optionJoinPoint.getArgs()).thenReturn(new Object[]{"244923000000", "interactive", "4"});
        when(flow.handleIfActive("244923000000", "interactive", "4")).thenReturn(Optional.empty());
        when(flow.isServiceIntent("4")).thenReturn(false);
        when(flow.start("244923000000")).thenReturn("LISTA DE SERVIÇOS");

        Object result = aspect.routeAcademicServices(optionJoinPoint);

        assertThat(result).isEqualTo(Optional.of("LISTA DE SERVIÇOS"));
        verify(optionJoinPoint, never()).proceed();
    }
}
