Fase 24F.1 — Bloquear reenvio duplicado de guia de pagamento

Objetivo:
- Evitar que a DCR envie várias vezes a mesma guia de pagamento para o aluno.
- Se uma cobrança já tiver mensagem PAYMENT_GUIDE/PAYMENT_GUIDE_EMAIL/PAYMENT_GUIDE_SMS com status SENT, o endpoint de lote retorna ALREADY_SENT e não reenvia.
- Reenvio manual continua possível usando forceResend=true.

Arquivos alterados:
1. src/main/java/com/secretariapay/api/dto/financial/TuitionChargeGuideDeliveryRequest.java
2. src/main/java/com/secretariapay/api/dto/financial/TuitionChargeGuideDeliveryResponse.java
3. src/main/java/com/secretariapay/api/service/financial/TuitionChargeGuideDeliveryService.java
4. src/main/java/com/secretariapay/api/service/whatsapp/SecretariaPayPaymentGuideMessageService.java
5. src/main/java/com/secretariapay/api/service/whatsapp/SecretariaPayMessageDispatchService.java
6. scripts/test-fase-24f1-no-duplicate-send.sh

Novidade no body:
{
  "forceResend": false
}

Comportamento padrão:
- forceResend=false:
  - cobrança já enviada -> ALREADY_SENT
  - nenhuma nova mensagem é criada/enviada.

- forceResend=true:
  - cria nova mensagem e reenvia a guia.
  - usar apenas com autorização manual da DCR.

Aplicar local:
cd C:\Users\dalaw\secretariapay-api
Expand-Archive -Path "$env:USERPROFILE\Downloads\secretariapay-fase-24f1-bloquear-reenvio-duplicado.zip" -DestinationPath . -Force

Build:
if (Test-Path .\mvnw.cmd) {
  .\mvnw.cmd clean package -DskipTests
} else {
  mvn clean package -DskipTests
}

Git:
git status
git add .
git commit -m "fix: prevent duplicate payment guide delivery"
git push origin main

Produção:
cd /opt/secretariapay-api
git pull origin main
docker compose up -d --build
docker compose logs -f --tail=200 api

Teste:
api_post "/api/v1/imetro/tuition-charges/send-guides" '{
  "institutionId":"c3726494-46b5-4563-8e84-0a04334fac8c",
  "referenceMonth":"2026-07",
  "chargeCodePrefix":"IMT-PROPINA-",
  "sendWhatsapp":true,
  "sendEmail":true,
  "sendSms":true,
  "onlyPending":true,
  "forceResend":false,
  "maxItems":10
}'

Resultado esperado após já ter mensagens SENT:
- sentWhatsapp: 0
- skippedAlreadySent: 2
- skippedNoContact: 1
- items com finalStatus ALREADY_SENT para cobranças já entregues.

Validação no banco:
select charge_code, channel, type, recipient_phone, status, count(*) as total
from secretariapay_messages
where charge_code like 'IMT-PROPINA-2026_07-%'
group by charge_code, channel, type, recipient_phone, status
order by charge_code, status;
