Fase 12.17 — Guia de pagamento PDF antes do recibo

Objetivo:
- O SecretáriaPay envia primeiro uma GUIA DE PAGAMENTO em PDF, com dados da cobrança e instruções bancárias.
- O recibo digital continua sendo emitido somente após confirmação/validação do pagamento.
- A guia é enviada como documento/anexo no WhatsApp.

Arquivos incluídos:
1. PaymentGuidePdfService.java
2. PublicPaymentGuideController.java
3. SecretariaPayPaymentGuideMessageService.java
4. SecretariaPayPaymentGuideController.java
5. WhatsAppCloudApiClient.java
6. SecretariaPayMessageDispatchService.java

Endpoints novos:
GET  /api/v1/public/payment-guides/{chargeCode}/pdf
POST /api/v1/secretariapay/payment-guides/charges/{chargeId}/message

Variáveis opcionais no .env.production:
SECRETARIAPAY_PAYMENT_BANK_NAME=
SECRETARIAPAY_PAYMENT_ACCOUNT_HOLDER=
SECRETARIAPAY_PAYMENT_IBAN=
SECRETARIAPAY_PAYMENT_ACCOUNT_NUMBER=
SECRETARIAPAY_PAYMENT_MULTICAIXA_REFERENCE=
SECRETARIAPAY_PAYMENT_MOBILE_MONEY_INFO=

Aplicar com o padrão do projeto:
cd C:\Users\dalaw\secretariapay-api

Expand-Archive -Path "$env:USERPROFILE\Downloads\secretariapay-fase-12-17-payment-guide-pdf.zip" -DestinationPath . -Force

if (Test-Path .\mvnw.cmd) {
  .\mvnw.cmd clean package -DskipTests
} else {
  mvn clean package -DskipTests
}

git status
git add .
git commit -m "feat: add payment guide PDF and WhatsApp dispatch"
git push origin main

Produção:
cd /opt/secretariapay-api
git pull origin main
docker compose up -d --build
docker compose logs -f --tail=200 api

Teste:
curl -I "https://secretariapay-api.paixaoangola.com/api/v1/public/payment-guides/CHG1783012061065/pdf"

Gerar mensagem:
curl -i -X POST "https://secretariapay-api.paixaoangola.com/api/v1/secretariapay/payment-guides/charges/5f929dfa-7dbb-45e5-807e-1a83898ead66/message" \
  -H "Authorization: Bearer $JWT"

Enviar:
curl -i -X POST "https://secretariapay-api.paixaoangola.com/api/v1/secretariapay/message-dispatch/$MESSAGE_ID/dispatch" \
  -H "Authorization: Bearer $JWT"
