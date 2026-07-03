Fase 14 — Pagamentos automáticos mock

Objetivo:
- Simular integrações bancárias/QR/Multicaixa/IBAN sem depender ainda do banco.
- Quando o sistema detecta a entrada do pagamento, ele confirma automaticamente.
- Não exige validação manual da secretaria/tesouraria.
- O sistema emite recibo digital e envia o PDF pelo WhatsApp.

Arquivos:
1. src/main/java/com/secretariapay/api/dto/financial/MockAutomaticPaymentRequest.java
2. src/main/java/com/secretariapay/api/dto/financial/MockAutomaticPaymentResponse.java
3. src/main/java/com/secretariapay/api/service/financial/SecretariaPayMockAutomaticPaymentService.java
4. src/main/java/com/secretariapay/api/controller/financial/SecretariaPayMockAutomaticPaymentController.java

Endpoints:
POST /api/v1/secretariapay/mock-payments/charges/{chargeId}/confirm
POST /api/v1/secretariapay/mock-payments/charges/code/{chargeCode}/confirm
POST /api/v1/secretariapay/mock-payments/multicaixa-express/charges/{chargeId}/confirm
POST /api/v1/secretariapay/mock-payments/iban-same-bank/charges/{chargeId}/confirm
POST /api/v1/secretariapay/mock-payments/iban-other-bank/charges/{chargeId}/settle
POST /api/v1/secretariapay/mock-payments/deposit/charges/{chargeId}/settle-after-24h
POST /api/v1/secretariapay/mock-payments/unitel-money/charges/{chargeId}/confirm
POST /api/v1/secretariapay/mock-payments/afrimoney/charges/{chargeId}/confirm

O que faz:
1. Recebe uma cobrança PENDING/OVERDUE.
2. Cria payment_proof aprovado automaticamente com file_url mock-bank-payment://...
3. Muda a cobrança para PAID.
4. Emite recibo, se ainda não existir.
5. Gera mensagem RECEIPT_ISSUED.
6. Envia o recibo PDF pelo WhatsApp.
7. Retorna providerMessageId real da Meta, se envio funcionar.

Aplicar:
cd C:\Users\dalaw\secretariapay-api
Expand-Archive -Path "$env:USERPROFILE\Downloads\secretariapay-fase-14-mock-automatic-payments.zip" -DestinationPath . -Force

if (Test-Path .\mvnw.cmd) {
  .\mvnw.cmd clean package -DskipTests
} else {
  mvn clean package -DskipTests
}

git status
git add .
git commit -m "feat: add mock automatic payment confirmation"
git push origin main

Produção:
cd /opt/secretariapay-api
git pull origin main
docker compose up -d --build
docker compose logs -f --tail=200 api
