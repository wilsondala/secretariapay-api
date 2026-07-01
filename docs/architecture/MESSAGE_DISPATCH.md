# SecretáriaPay - Fila de envio de mensagens

Este módulo prepara o SecretáriaPay para sair do modo de preview/histórico e operar com uma fila de envio.

## Fluxo

1. A mensagem é gerada e salva como `GENERATED`.
2. O operador ou serviço marca a mensagem como `QUEUED`.
3. O dispatcher processa mensagens `QUEUED`.
4. Enquanto `secretariapay.whatsapp.enabled=false`, o envio é simulado e a mensagem vira `SENT` com `providerMessageId` mockado.
5. Quando o WhatsApp Cloud API for configurado, o mesmo ponto do serviço será usado para enviar pela Meta.

## Endpoints

```http
PATCH /api/v1/secretariapay/message-dispatch/{messageId}/queue
POST  /api/v1/secretariapay/message-dispatch/{messageId}/dispatch
POST  /api/v1/secretariapay/message-dispatch/process-queue?limit=10
```

## Segurança operacional

Nesta etapa, o envio real ainda não é feito automaticamente. Isso evita disparos acidentais antes de configurar número oficial, token, templates e permissões da Meta.
