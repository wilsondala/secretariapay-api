package com.secretariapay.api.service.whatsapp;

import com.secretariapay.api.dto.whatsapp.WhatsAppCloudSendResult;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WhatsAppValidationMockAspectTest {

    @Test
    void shouldReturnSyntheticSuccessForLocalValidationEndpoint() {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"+244 923 168 085", "Documento disponível."});
        WhatsAppValidationMockAspect aspect = new WhatsAppValidationMockAspect("http://localhost");

        WhatsAppCloudSendResult result = (WhatsAppCloudSendResult) aspect.mockTextSend(joinPoint);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getHttpStatus()).isEqualTo(200);
        assertThat(result.getProviderMessageId()).startsWith("validation-mock-");
    }

    @Test
    void shouldRejectValidationMockAgainstExternalGraphEndpoint() {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        WhatsAppValidationMockAspect aspect = new WhatsAppValidationMockAspect("https://graph.facebook.com");

        WhatsAppCloudSendResult result = (WhatsAppCloudSendResult) aspect.mockTextSend(joinPoint);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("localhost");
        verifyNoInteractions(joinPoint);
    }
}
