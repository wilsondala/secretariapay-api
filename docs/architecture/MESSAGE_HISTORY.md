# Histórico de mensagens - SecretáriaPay Académico

Este módulo registra as mensagens geradas pelo SecretáriaPay antes do envio real via WhatsApp Cloud API.

## Objetivo

Guardar auditoria de comunicação por instituição, estudante, cobrança, comprovativo e recibo.

## Status da mensagem

- `GENERATED`: mensagem criada pelo sistema e pronta para envio.
- `QUEUED`: mensagem colocada em fila de envio.
- `SENT`: mensagem enviada pelo provedor.
- `FAILED`: falha no envio.
- `READ`: mensagem lida pelo destinatário, quando houver webhook do provedor.

## Endpoints principais

Gerar e salvar mensagens:

```http
POST /api/v1/secretariapay/message-history/charges/{chargeId}/before-due?daysBefore=5
POST /api/v1/secretariapay/message-history/charges/{chargeId}/due-today
POST /api/v1/secretariapay/message-history/charges/{chargeId}/overdue?daysLate=7
POST /api/v1/secretariapay/message-history/payment-proofs/{paymentProofId}/received
POST /api/v1/secretariapay/message-history/payment-proofs/{paymentProofId}/approved
POST /api/v1/secretariapay/message-history/receipts/{receiptId}/issued
POST /api/v1/secretariapay/message-history/students/{studentId}/regularized
```

Consultar histórico:

```http
GET /api/v1/secretariapay/message-history/institutions/{institutionId}
GET /api/v1/secretariapay/message-history/students/{studentId}
GET /api/v1/secretariapay/message-history/charges/{chargeId}
```

Atualizar status:

```http
PATCH /api/v1/secretariapay/message-history/{messageId}/queue
PATCH /api/v1/secretariapay/message-history/{messageId}/sent
PATCH /api/v1/secretariapay/message-history/{messageId}/failed
PATCH /api/v1/secretariapay/message-history/{messageId}/read
```

## Próxima etapa

Criar o motor de envio real via WhatsApp Cloud API, consumindo mensagens `GENERATED` ou `QUEUED` e atualizando para `SENT` ou `FAILED`.
