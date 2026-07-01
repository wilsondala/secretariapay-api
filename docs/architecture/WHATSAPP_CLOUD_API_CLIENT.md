# SecretáriaPay - WhatsApp Cloud API Client

Este módulo adiciona a primeira versão do cliente real para envio de mensagens pela WhatsApp Cloud API.

## Comportamento seguro

Por padrão, o envio real continua desativado:

```yaml
secretariapay:
  whatsapp:
    enabled: false
```

Quando `enabled=false`, a fila continua operando em modo simulado:

```text
QUEUED -> SENT
providerMessageId = mock-whatsapp-{messageId}
```

## Envio real

O envio real só acontece quando todas as variáveis abaixo estiverem configuradas:

```bash
SECRETARIAPAY_WHATSAPP_ENABLED=true
SECRETARIAPAY_WHATSAPP_PHONE_NUMBER_ID=...
SECRETARIAPAY_WHATSAPP_ACCESS_TOKEN=...
SECRETARIAPAY_WHATSAPP_GRAPH_API_VERSION=v20.0
SECRETARIAPAY_WHATSAPP_GRAPH_API_BASE_URL=https://graph.facebook.com
```

## Fluxo

```text
1. Mensagem é criada no histórico.
2. Mensagem entra na fila como QUEUED.
3. process-queue chama o WhatsAppCloudApiClient.
4. Se a Meta retornar messages[0].id:
   - status = SENT
   - providerMessageId = ID retornado pela Meta
   - sentAt = data/hora atual
5. Se a Meta retornar erro:
   - status = FAILED
   - failureReason = erro resumido
```

## Observação importante

Antes de ativar produção real, validar número oficial da instituição, phone number id correto, token permanente ou token de sistema, templates aprovados quando o envio sair da janela de 24h e autorização da instituição para comunicação via WhatsApp.
