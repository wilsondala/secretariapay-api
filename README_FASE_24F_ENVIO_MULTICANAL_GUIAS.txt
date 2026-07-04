Fase 24F — Envio multicanal de guias de pagamento IMETRO/DCR

Objetivo:
Enviar guias de pagamento das cobranças de propina por múltiplos canais:

1. WhatsApp, quando o estudante tiver whatsapp cadastrado.
2. E-mail, quando o estudante tiver e-mail ou e-mail do encarregado.
3. SMS, quando não houver WhatsApp, mas existir telefone cadastrado.
4. Relatório de pendências quando o estudante não tiver nenhum contacto útil.

Regra operacional:
- WhatsApp envia PDF/link da guia usando o fluxo já existente do SecretáriaPay.
- E-mail e SMS entram inicialmente em modo MOCK, registrando o envio em secretariapay_messages.
- O SMS usa o telefone do aluno ou, se necessário, o telefone do encarregado.
- O e-mail usa o e-mail do aluno ou, se necessário, o e-mail do encarregado.
- O lote não quebra se algum aluno estiver sem contacto.

Arquivos incluídos:
1. src/main/java/com/secretariapay/api/dto/financial/TuitionChargeGuideDeliveryRequest.java
2. src/main/java/com/secretariapay/api/dto/financial/TuitionChargeGuideDeliveryItem.java
3. src/main/java/com/secretariapay/api/dto/financial/TuitionChargeGuideDeliveryResponse.java
4. src/main/java/com/secretariapay/api/service/notification/NotificationDispatchResult.java
5. src/main/java/com/secretariapay/api/service/notification/SecretariaPayEmailNotificationService.java
6. src/main/java/com/secretariapay/api/service/notification/SecretariaPaySmsNotificationService.java
7. src/main/java/com/secretariapay/api/service/financial/TuitionChargeGuideDeliveryService.java
8. src/main/java/com/secretariapay/api/controller/financial/TuitionChargeGenerationController.java
9. scripts/test-fase-24f-send-guides.sh

Endpoint novo:
POST /api/v1/imetro/tuition-charges/send-guides

Exemplo:
{
  "institutionId":"c3726494-46b5-4563-8e84-0a04334fac8c",
  "referenceMonth":"2026-07",
  "chargeCodePrefix":"IMT-PROPINA-",
  "sendWhatsapp":true,
  "sendEmail":true,
  "sendSms":true,
  "onlyPending":true,
  "maxItems":10
}

Variáveis opcionais no .env.production:
SECRETARIAPAY_EMAIL_ENABLED=false
SECRETARIAPAY_EMAIL_MOCK_ENABLED=true
SECRETARIAPAY_EMAIL_FROM=dcr_pay@imetroangola.com
SECRETARIAPAY_EMAIL_CC=df.oi_pay@imetroangola.com

SECRETARIAPAY_SMS_ENABLED=false
SECRETARIAPAY_SMS_MOCK_ENABLED=true
SECRETARIAPAY_SMS_PROVIDER=MOCK
SECRETARIAPAY_SMS_API_URL=
SECRETARIAPAY_SMS_API_TOKEN=

Aplicar local:
cd C:\Users\dalaw\secretariapay-api

Expand-Archive -Path "$env:USERPROFILE\Downloads\secretariapay-fase-24f-envio-multicanal-guias.zip" -DestinationPath . -Force

if (Test-Path .\mvnw.cmd) {
  .\mvnw.cmd clean package -DskipTests
} else {
  mvn clean package -DskipTests
}

git status
git add .
git commit -m "feat: send tuition guides by multichannel delivery"
git push origin main

Produção:
cd /opt/secretariapay-api
git pull origin main
docker compose up -d --build
docker compose logs -f --tail=200 api

Teste na VPS:
bash scripts/test-fase-24f-send-guides.sh

Validação no banco:
select charge_code, channel, type, recipient_phone, status, provider_message_id, failure_reason, created_at
from secretariapay_messages
where charge_code like 'IMT-PROPINA-2026_07-%'
order by created_at desc
limit 20;
