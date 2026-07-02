# Cleanup Fase 8 - Desativação segura do webhook WhatsApp legado

## Objetivo

Desativar, por configuração, o webhook antigo de WhatsApp herdado do fluxo VaiRápido/passagens sem apagar classes, entidades, tabelas ou migrations.

## Contexto

A Fase 7 mostrou que a cadeia legada está concentrada em:

```text
WhatsappWebhookController
        ↓
WhatsappWebhookService
        ↓
WhatsappSessionService
        ↓
WhatsappCommandService
        ↓
WhatsappFaqAnswerService
```

O SecretáriaPay Académico atual usa o novo fluxo institucional:

```text
message-history
message-dispatch
whatsapp/diagnostics
branding
academic
financial
receipts
payment-proofs
```

## Alterações desta fase

Arquivos alterados:

```text
src/main/java/com/secretariapay/api/controller/WhatsappWebhookController.java
src/main/java/com/secretariapay/api/service/WhatsappWebhookService.java
```

Mudanças:

```text
1. Adiciona propriedade:
   secretariapay.legacy.whatsapp-webhook.enabled=false

2. Mantém o webhook antigo como @Deprecated.

3. Adiciona:
   GET /api/v1/public/whatsapp/webhook/status

4. Quando o legado estiver desativado:
   - GET de verificação retorna 410 Gone.
   - POST de mensagem retorna JSON processado=false, sem chamar WhatsappCommandService.
```

## Motivo

Evitar uso acidental do fluxo antigo de passagens, bilhete, embarque e Pix em produção.

## Como reativar temporariamente

Somente em caso de necessidade técnica:

```env
SECRETARIAPAY_LEGACY_WHATSAPP_WEBHOOK_ENABLED=true
SECRETARIAPAY_WHATSAPP_VERIFY_TOKEN=<token>
```

ou via propriedades Spring equivalentes:

```properties
secretariapay.legacy.whatsapp-webhook.enabled=true
secretariapay.whatsapp.verify-token=<token>
```

## Testes sugeridos

```bash
curl https://secretariapay-api.paixaoangola.com/api/v1/public/whatsapp/webhook/status

curl -X POST https://secretariapay-api.paixaoangola.com/api/v1/public/whatsapp/webhook \
  -H "Content-Type: application/json" \
  -d '{}'
```

Esperado com legado desligado:

```json
{
  "processed": false,
  "reason": "Webhook legado de passagens desativado. Use o módulo SecretáriaPay message-history/message-dispatch.",
  "commandProcessed": false,
  "commandAllowed": false,
  "commandName": "LEGACY_WEBHOOK_DISABLED"
}
```

## Observação

Esta fase não remove código legado. A remoção definitiva fica para uma fase posterior, após confirmação de que nenhum endpoint novo depende de sessões, comandos ou DTOs antigos.
