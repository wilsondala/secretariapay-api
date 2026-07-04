Fase 16B — Envio aberto para o número que está testando no WhatsApp

Problema corrigido:
- O aluno podia se identificar com uma matrícula de outro cadastro.
- A guia/referência de pagamento era enviada para o telefone cadastrado no estudante.
- Para demonstração/teste, isso desvia o PDF para outro número.

Nova regra de teste:
- Durante uma conversa recebida pelo WhatsApp, qualquer documento gerado nesse atendimento
  deve ser enviado para o número que está conversando agora.
- Isso vale para guia de pagamento, referência e recibo automático.
- O histórico de mensagem também guarda esse número da conversa como destinatário.

Regra institucional futura:
- Para voltar ao comportamento oficial, configure:
  SECRETARIAPAY_WHATSAPP_TEST_OPEN_RECIPIENT_ENABLED=false

Arquivos alterados/adicionados:
1. src/main/java/com/secretariapay/api/service/whatsapp/SecretariaPayWhatsappWebhookService.java
2. src/main/java/com/secretariapay/api/service/whatsapp/SecretariaPayMessageHistoryService.java
3. src/main/java/com/secretariapay/api/service/whatsapp/WhatsappRecipientOverrideContext.java
4. src/main/java/com/secretariapay/api/service/whatsapp/TestOpenRecipientWhatsAppCloudApiClient.java
