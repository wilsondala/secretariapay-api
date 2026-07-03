Fase 15 — Atendimento completo 100% pelo WhatsApp com dados mock

Objetivo:
- Não usar curl no fluxo de demonstração.
- O aluno conversa apenas pelo WhatsApp.
- O sistema cria/usa cobrança mock, envia guia PDF, reconhece recebimento, simula pagamento, emite recibo e encerra atendimento.

Fluxo WhatsApp:
1. Aluno:
   Quero boleto do mês

2. SecretáriaPay:
   - identifica o aluno pelo número de WhatsApp
   - cria ou reutiliza cobrança PENDING do mês
   - envia guia-pagamento-CHG....pdf
   - responde com resumo da cobrança

3. Aluno:
   recebi

4. SecretáriaPay:
   - confirma recebimento da guia
   - orienta: responda "paguei"

5. Aluno:
   paguei

6. SecretáriaPay:
   - simula confirmação bancária mock
   - cria payment_proof automático APPROVED
   - marca charge como PAID
   - emite recibo
   - envia recibo-RCT....pdf
   - responde com confirmação

7. Aluno:
   obrigado

8. SecretáriaPay:
   - encerra o atendimento automaticamente

Arquivos:
- src/main/java/com/secretariapay/api/service/whatsapp/SecretariaPayWhatsappFullMockFlowService.java
- src/main/java/com/secretariapay/api/service/whatsapp/SecretariaPayWhatsappWebhookService.java

Aplicar:
cd C:\Users\dalaw\secretariapay-api

Expand-Archive -Path "$env:USERPROFILE\Downloads\secretariapay-fase-15-whatsapp-full-mock-flow.zip" -DestinationPath . -Force

if (Test-Path .\mvnw.cmd) {
  .\mvnw.cmd clean package -DskipTests
} else {
  mvn clean package -DskipTests
}

git status
git add .
git commit -m "feat: complete mock payment flow via WhatsApp"
git push origin main

Produção:
cd /opt/secretariapay-api
git pull origin main
docker compose up -d --build
docker compose logs -f --tail=200 api

Teste real no WhatsApp:
- Quero boleto do mês
- recebi
- paguei
- obrigado

Confirmação no banco:
docker compose exec -T postgres sh -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"' <<'SQL'
select charge_code, description, reference_month, status, paid_at
from charges
where description ilike '%Atendimento WhatsApp Mock%'
order by created_at desc
limit 5;

select id, charge_id, file_url, status, reviewed_at, review_note
from payment_proofs
where file_url like 'mock-bank-payment://%'
order by created_at desc
limit 5;

select receipt_code, status, pdf_url, validation_url, issued_at
from receipts
order by created_at desc
limit 5;

select type, charge_code, receipt_code, status, provider_message_id, sent_at, failure_reason
from secretariapay_messages
where type in ('RECEIPT_ISSUED', 'PAYMENT_GUIDE')
order by created_at desc
limit 10;
SQL
