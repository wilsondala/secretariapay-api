Fase 16B — Dados bancários reais do IMETRO na guia de pagamento

Dados aplicados na guia:
- Coordenada bancária: OMNEN INTELENGENDA
- Banco: Banco Angolano de Investimento
- Nº Conta AKZ: 060144677 10 001
- IBAN: AO06 0040 0000 6014 4677 1017 1

Arquivo alterado:
- src/main/java/com/secretariapay/api/service/financial/PaymentGuidePdfService.java

Observação:
O serviço continua aceitando configuração por .env.production. Caso as variáveis estejam vazias ou com placeholders como "A definir" ou "Banco da instituição", o sistema assume automaticamente os dados reais acima.

Variáveis recomendadas no .env.production:
SECRETARIAPAY_PAYMENT_BANK_NAME=Banco Angolano de Investimento
SECRETARIAPAY_PAYMENT_ACCOUNT_HOLDER=OMNEN INTELENGENDA
SECRETARIAPAY_PAYMENT_IBAN=AO06 0040 0000 6014 4677 1017 1
SECRETARIAPAY_PAYMENT_ACCOUNT_NUMBER=060144677 10 001
SECRETARIAPAY_PAYMENT_MULTICAIXA_REFERENCE=Multicaixa Express / transferência bancária para a conta AKZ indicada
SECRETARIAPAY_PAYMENT_MOBILE_MONEY_INFO=Unitel Money/Afrimoney quando autorizado pela instituição

Aplicar:
cd C:\Users\dalaw\secretariapay-api

Expand-Archive -Path "$env:USERPROFILE\Downloads\secretariapay-fase-16b-dados-bancarios-imetro.zip" -DestinationPath . -Force

Build:
if (Test-Path .\mvnw.cmd) {
  .\mvnw.cmd clean package -DskipTests
} else {
  mvn clean package -DskipTests
}

Git:
git status
git add .
git commit -m "fix: add IMETRO real bank details to payment guide"
git push origin main

Produção:
cd /opt/secretariapay-api
git pull origin main
docker compose up -d --build
docker compose logs -f --tail=200 api

Teste:
curl -L "https://secretariapay-api.paixaoangola.com/api/v1/public/payment-guides/CHG1783012061065/pdf" -o /tmp/guia-imetro-banco.pdf
ls -lh /tmp/guia-imetro-banco.pdf

No WhatsApp:
menu
2
IMETRO-2026-TESTE-002
1

A guia enviada deve mostrar:
Coordenada: OMNEN INTELENGENDA
Banco: Banco Angolano de Investimento
IBAN: AO06 0040 0000 6014 4677 1017 1
Nº Conta AKZ: 060144677 10 001
