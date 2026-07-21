package com.secretariapay.api.service.whatsapp;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecretariaPayWhatsappWebhookServiceInteractiveTest {

    @Test
    void deveUsarIdDaListaInterativaAntesDoTitulo() {
        SecretariaPayWhatsappBrainService brainService = mock(SecretariaPayWhatsappBrainService.class);
        SecretariaPayWhatsappAcademicSupportService academicSupportService = mock(SecretariaPayWhatsappAcademicSupportService.class);
        SecretariaPayWhatsappFinancialDemoConversationService financialConversationService = mock(SecretariaPayWhatsappFinancialDemoConversationService.class);
        WhatsappInteractiveMenuFactory interactiveMenuFactory = mock(WhatsappInteractiveMenuFactory.class);
        WhatsAppCloudApiClient whatsAppCloudApiClient = mock(WhatsAppCloudApiClient.class);

        SecretariaPayWhatsappWebhookService service = new SecretariaPayWhatsappWebhookService(
                "verify-token",
                false,
                "",
                "",
                "v20.0",
                "https://graph.facebook.com",
                brainService,
                academicSupportService,
                financialConversationService,
                interactiveMenuFactory,
                whatsAppCloudApiClient
        );

        when(financialConversationService.handle("244923000000", "interactive", "3"))
                .thenReturn(Optional.of("Resposta processada."));
        when(interactiveMenuFactory.fromReplyText("Resposta processada."))
                .thenReturn(Optional.empty());

        Map<String, Object> payload = Map.of(
                "entry", List.of(Map.of(
                        "changes", List.of(Map.of(
                                "value", Map.of(
                                        "messages", List.of(Map.of(
                                                "from", "244923000000",
                                                "type", "interactive",
                                                "interactive", Map.of(
                                                        "type", "list_reply",
                                                        "list_reply", Map.of(
                                                                "id", "3",
                                                                "title", "Transferência mesmo banco",
                                                                "description", "Transferência entre contas BAI"
                                                        )
                                                )
                                        ))
                                )
                        ))
                ))
        );

        Map<String, Object> response = service.receiveWebhookPayload(payload);

        assertThat(response.get("messageText")).isEqualTo("3");
        verify(financialConversationService).handle("244923000000", "interactive", "3");
    }

    @Test
    void devePersistirMidiaAntesDoRoteadorFinanceiroResponder() {
        SecretariaPayWhatsappBrainService brainService = mock(SecretariaPayWhatsappBrainService.class);
        SecretariaPayWhatsappAcademicSupportService academicSupportService = mock(SecretariaPayWhatsappAcademicSupportService.class);
        SecretariaPayWhatsappFinancialDemoConversationService financialConversationService = mock(SecretariaPayWhatsappFinancialDemoConversationService.class);
        WhatsappInteractiveMenuFactory interactiveMenuFactory = mock(WhatsappInteractiveMenuFactory.class);
        WhatsAppCloudApiClient whatsAppCloudApiClient = mock(WhatsAppCloudApiClient.class);

        SecretariaPayWhatsappWebhookService service = new SecretariaPayWhatsappWebhookService(
                "verify-token",
                false,
                "",
                "",
                "v20.0",
                "https://graph.facebook.com",
                brainService,
                academicSupportService,
                financialConversationService,
                interactiveMenuFactory,
                whatsAppCloudApiClient
        );

        when(academicSupportService.buildDatabaseAwareReply(
                "5511915102566",
                "image",
                "[imagem recebida]",
                "media-imetro-123",
                "",
                "image/jpeg"
        )).thenReturn(Optional.of("Comprovativo recebido e registado. Estado: Pendente de validação pela DCR."));
        when(interactiveMenuFactory.fromReplyText("Comprovativo recebido e registado. Estado: Pendente de validação pela DCR."))
                .thenReturn(Optional.empty());

        Map<String, Object> payload = Map.of(
                "entry", List.of(Map.of(
                        "changes", List.of(Map.of(
                                "value", Map.of(
                                        "messages", List.of(Map.of(
                                                "from", "5511915102566",
                                                "type", "image",
                                                "image", Map.of(
                                                        "id", "media-imetro-123",
                                                        "mime_type", "image/jpeg"
                                                )
                                        ))
                                )
                        ))
                ))
        );

        Map<String, Object> response = service.receiveWebhookPayload(payload);

        assertThat(response.get("replyText")).isEqualTo("Comprovativo recebido e registado. Estado: Pendente de validação pela DCR.");
        assertThat(response.get("mediaId")).isEqualTo("media-imetro-123");
        verify(academicSupportService).buildDatabaseAwareReply(
                "5511915102566",
                "image",
                "[imagem recebida]",
                "media-imetro-123",
                "",
                "image/jpeg"
        );
        verify(financialConversationService, never()).handle("5511915102566", "image", "[imagem recebida]");
    }
}
