# SecretáriaPay Académico - Pré-visualização de mensagens WhatsApp

Este módulo cria a primeira camada de comunicação do SecretáriaPay Académico.

## Objetivo

Gerar mensagens padronizadas para cobrança, validação de comprovativos, emissão de recibo e regularização académica.

Nesta etapa, a API ainda não envia mensagens reais pelo WhatsApp Cloud API. Ela apenas gera o texto oficial que será usado posteriormente pelo motor de envio.

## Endpoint base

```http
/api/v1/secretariapay/messages
```

## Endpoints disponíveis

### Aviso antes do vencimento

```http
GET /api/v1/secretariapay/messages/charges/{chargeId}/before-due?daysBefore=5
```

### Aviso no dia do vencimento

```http
GET /api/v1/secretariapay/messages/charges/{chargeId}/due-today
```

### Aviso de atraso

```http
GET /api/v1/secretariapay/messages/charges/{chargeId}/overdue?daysLate=7
```

### Comprovativo recebido

```http
GET /api/v1/secretariapay/messages/payment-proofs/{paymentProofId}/received
```

### Comprovativo aprovado

```http
GET /api/v1/secretariapay/messages/payment-proofs/{paymentProofId}/approved
```

### Recibo emitido

```http
GET /api/v1/secretariapay/messages/receipts/{receiptId}/issued
```

### Situação académica regularizada

```http
GET /api/v1/secretariapay/messages/students/{studentId}/regularized
```

## Próximos passos

1. Persistir mensagens em tabela própria.
2. Criar fila de envio.
3. Integrar WhatsApp Cloud API.
4. Registrar status: PENDING, SENT, FAILED, READ.
5. Criar templates oficiais por instituição.
6. Permitir personalização por universidade.
