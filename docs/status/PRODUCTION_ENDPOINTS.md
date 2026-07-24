# SecretáriaPay Académico — Endpoints de Produção

Base URL:

```text
https://secretariapay-api.paixaoangola.com
```

## 1. Health

```bash
curl http://127.0.0.1:8080/actuator/health
curl https://secretariapay-api.paixaoangola.com/actuator/health
```

## 2. Autenticação

### Register

```bash
curl -X POST https://secretariapay-api.paixaoangola.com/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Administrador SecretáriaPay",
    "email": "admin@secretariapay.com",
    "password": "Admin@123456",
    "role": "ADMIN_GLOBAL"
  }'
```

### Login

```bash
TOKEN=$(curl -s -X POST https://secretariapay-api.paixaoangola.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@secretariapay.com",
    "password": "Admin@123456"
  }' | jq -r '.token')

echo $TOKEN
```

## 3. Branding público

### Logo

```bash
curl -I https://secretariapay-api.paixaoangola.com/branding/secretariapay-logo.png
```

### Branding do sistema

```bash
curl -X GET https://secretariapay-api.paixaoangola.com/api/v1/public/branding/secretariapay
```

### Branding institucional IMETRO

```bash
curl -X GET https://secretariapay-api.paixaoangola.com/api/v1/public/branding/institutions/imetro
```

## 4. Instituições

```bash
curl -X GET https://secretariapay-api.paixaoangola.com/api/v1/institutions \
  -H "Authorization: Bearer $TOKEN"
```

```bash
curl -X GET https://secretariapay-api.paixaoangola.com/api/v1/institution-settings/slug/imetro \
  -H "Authorization: Bearer $TOKEN"
```

## 5. Cursos e turmas

```bash
curl -X GET "https://secretariapay-api.paixaoangola.com/api/v1/courses?institutionId=c3726494-46b5-4563-8e84-0a04334fac8c" \
  -H "Authorization: Bearer $TOKEN"
```

```bash
curl -X GET "https://secretariapay-api.paixaoangola.com/api/v1/academic-classes?courseId=02a3bb75-0fbf-4771-ab14-4a88f5e38b2e" \
  -H "Authorization: Bearer $TOKEN"
```

## 6. Dashboard financeiro

```bash
curl -X GET https://secretariapay-api.paixaoangola.com/api/v1/dashboard/financial \
  -H "Authorization: Bearer $TOKEN"
```

## 7. Cobranças

```bash
curl -X GET "https://secretariapay-api.paixaoangola.com/api/v1/charges?studentId=970a3790-db44-491b-99b2-375d54dc05fc" \
  -H "Authorization: Bearer $TOKEN"
```

## 8. Comprovativos

```bash
curl -X GET "https://secretariapay-api.paixaoangola.com/api/v1/payment-proofs?chargeId=5690d838-8f2d-43e9-bc9b-cf0dfdb71056" \
  -H "Authorization: Bearer $TOKEN"
```

```bash
curl -X PATCH https://secretariapay-api.paixaoangola.com/api/v1/payment-proofs/eaa8616b-9815-432e-a6af-cc8c32603521/approve \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "reviewedByUserId": "ca00816d-c0c8-4cb2-91ef-6b84917c6682",
    "reviewNote": "Comprovativo validado pela tesouraria. Pagamento confirmado."
  }'
```

## 9. Recibos

### Emitir recibo

```bash
curl -X POST https://secretariapay-api.paixaoangola.com/api/v1/receipts/charge/5690d838-8f2d-43e9-bc9b-cf0dfdb71056/issue \
  -H "Authorization: Bearer $TOKEN"
```

### Listar recibos

```bash
curl -X GET https://secretariapay-api.paixaoangola.com/api/v1/receipts \
  -H "Authorization: Bearer $TOKEN"
```

### Baixar PDF

```bash
curl -L -o recibo-imetro.pdf \
  https://secretariapay-api.paixaoangola.com/api/v1/receipts/7b1dd67d-797c-4884-b062-c32393cb0300/pdf \
  -H "Authorization: Bearer $TOKEN"
```

### Validação pública

```bash
curl -X GET https://secretariapay-api.paixaoangola.com/api/v1/public/receipts/validate/RCT1782913501413
```

## 10. Mensagens WhatsApp — Preview

```bash
curl -X GET "https://secretariapay-api.paixaoangola.com/api/v1/secretariapay/messages/charges/5690d838-8f2d-43e9-bc9b-cf0dfdb71056/before-due?daysBefore=5" \
  -H "Authorization: Bearer $TOKEN"
```

```bash
curl -X GET "https://secretariapay-api.paixaoangola.com/api/v1/secretariapay/messages/charges/5690d838-8f2d-43e9-bc9b-cf0dfdb71056/overdue?daysLate=7" \
  -H "Authorization: Bearer $TOKEN"
```

```bash
curl -X GET https://secretariapay-api.paixaoangola.com/api/v1/secretariapay/messages/receipts/7b1dd67d-797c-4884-b062-c32393cb0300/issued \
  -H "Authorization: Bearer $TOKEN"
```

## 11. Histórico de mensagens

### Gerar e salvar mensagem

```bash
curl -X POST "https://secretariapay-api.paixaoangola.com/api/v1/secretariapay/message-history/charges/5690d838-8f2d-43e9-bc9b-cf0dfdb71056/before-due?daysBefore=3" \
  -H "Authorization: Bearer $TOKEN"
```

### Histórico por estudante

```bash
curl -X GET https://secretariapay-api.paixaoangola.com/api/v1/secretariapay/message-history/students/970a3790-db44-491b-99b2-375d54dc05fc \
  -H "Authorization: Bearer $TOKEN"
```

## 12. Fila de envio

### Colocar na fila

```bash
curl -X PATCH https://secretariapay-api.paixaoangola.com/api/v1/secretariapay/message-dispatch/COLE_O_MESSAGE_ID/queue \
  -H "Authorization: Bearer $TOKEN"
```

### Processar fila

```bash
curl -X POST "https://secretariapay-api.paixaoangola.com/api/v1/secretariapay/message-dispatch/process-queue?limit=10" \
  -H "Authorization: Bearer $TOKEN"
```

## 13. Diagnóstico WhatsApp

```bash
curl -X GET https://secretariapay-api.paixaoangola.com/api/v1/secretariapay/whatsapp/diagnostics \
  -H "Authorization: Bearer $TOKEN"
```
