Fase 12.18 — QRCode de pagamento na Guia de Pagamento

Objetivo:
- Adicionar um QRCode de pagamento na guia/boleto institucional.
- Manter o QRCode de consulta da guia que já existia.
- O QRCode de pagamento fica configurável por ambiente, para receber depois o payload oficial do banco/processador/EMIS/gateway.

Arquivo alterado:
src/main/java/com/secretariapay/api/service/financial/PaymentGuidePdfService.java

Variáveis opcionais no .env.production:
SECRETARIAPAY_PAYMENT_QR_ENABLED=true
SECRETARIAPAY_PAYMENT_QR_LABEL=QRCode de pagamento
SECRETARIAPAY_PAYMENT_QR_INSTRUCTIONS=Ler no aplicativo de pagamento autorizado pela instituição.
SECRETARIAPAY_PAYMENT_QR_PAYLOAD_TEMPLATE=

Exemplo de template configurável:
SECRETARIAPAY_PAYMENT_QR_PAYLOAD_TEMPLATE=SECRETARIAPAY|CHARGE={chargeCode}|AMOUNT={amount}|CURRENCY={currency}|IBAN={iban}|STUDENT={studentNumber}

Placeholders suportados:
{chargeCode}
{amount}
{currency}
{dueDate}
{iban}
{accountNumber}
{accountHolder}
{studentNumber}
{studentName}

Aplicar com o padrão do projeto:
cd C:\Users\dalaw\secretariapay-api

Expand-Archive -Path "$env:USERPROFILE\Downloads\secretariapay-fase-12-18-payment-qr-guide.zip" -DestinationPath . -Force

if (Test-Path .\mvnw.cmd) {
  .\mvnw.cmd clean package -DskipTests
} else {
  mvn clean package -DskipTests
}

git status
git add .
git commit -m "feat: add payment QR code to payment guide"
git push origin main

Produção:
cd /opt/secretariapay-api
git pull origin main
docker compose up -d --build
docker compose logs -f --tail=200 api

Teste:
curl -o /tmp/guia-pagamento-qrcode.pdf "https://secretariapay-api.paixaoangola.com/api/v1/public/payment-guides/CHG1783012061065/pdf"
ls -lh /tmp/guia-pagamento-qrcode.pdf
