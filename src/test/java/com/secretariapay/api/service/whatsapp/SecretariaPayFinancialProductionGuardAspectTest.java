package com.secretariapay.api.service.whatsapp;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecretariaPayFinancialProductionGuardAspectTest {

    @Test
    void deveBloquearInfinitePayNoAmbienteInstitucional() throws Throwable {
        SecretariaPayFinancialProductionGuardAspect aspect = new SecretariaPayFinancialProductionGuardAspect(false);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"244923000000", "interactive", "8"});

        Object result = aspect.protectMainFinancialFlow(joinPoint);

        assertThat(result).isInstanceOf(Optional.class);
        assertThat(((Optional<?>) result).orElseThrow().toString())
                .contains("não está disponível no ambiente institucional")
                .contains("Multicaixa Express")
                .doesNotContain("InfinitePay");
        verify(joinPoint, never()).proceed();
    }

    @Test
    void deveRemoverInformacoesDeTesteDaRespostaFinanceira() {
        SecretariaPayFinancialProductionGuardAspect aspect = new SecretariaPayFinancialProductionGuardAspect(false);

        String result = aspect.sanitizeFinancialText("""
                Integração: AppyPay Sandbox
                [1] Multicaixa Express
                [8] Teste real Brasil - InfinitePay
                Pix/link InfinitePay
                """);

        assertThat(result)
                .contains("Integração: AppyPay")
                .contains("[1] Multicaixa Express")
                .doesNotContain("Sandbox")
                .doesNotContain("InfinitePay")
                .doesNotContain("Teste real Brasil");
    }

    @Test
    void devePermitirPagamentoDeTesteSomenteQuandoAtivadoExplicitamente() throws Throwable {
        SecretariaPayFinancialProductionGuardAspect aspect = new SecretariaPayFinancialProductionGuardAspect(true);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"244923000000", "interactive", "8"});
        when(joinPoint.proceed()).thenReturn(Optional.of("Fluxo de teste autorizado."));

        Object result = aspect.protectMainFinancialFlow(joinPoint);

        assertThat(result).isEqualTo(Optional.of("Fluxo de teste autorizado."));
        verify(joinPoint).proceed();
    }
}
