Fase 16 — Roteiro Financeiro WhatsApp completo

Objetivo:
- Inserir o roteiro completo de conversa da Secretária Financeira Universitária.
- Evitar o robô ficar preso em fluxo antigo de comprovativo.
- Criar um roteador financeiro prioritário antes do atendimento académico antigo.
- Permitir atendimento real por WhatsApp com mock: guia PDF, recebi, paguei, recibo PDF e encerramento.

Arquivos:
1. src/main/java/com/secretariapay/api/service/whatsapp/SecretariaPayWhatsappFinancialConversationService.java
2. src/main/java/com/secretariapay/api/service/whatsapp/SecretariaPayWhatsappWebhookService.java

Aplicar:
cd C:\Users\dalaw\secretariapay-api

Expand-Archive -Path "$env:USERPROFILE\Downloads\secretariapay-fase-16-roteiro-financeiro-whatsapp.zip" -DestinationPath . -Force

Build:
if (Test-Path .\mvnw.cmd) {
  .\mvnw.cmd clean package -DskipTests
} else {
  mvn clean package -DskipTests
}

Git:
git status
git add .
git commit -m "feat: add complete financial WhatsApp conversation router"
git push origin main

Produção:
cd /opt/secretariapay-api
git pull origin main
docker compose up -d --build
docker compose logs -f --tail=200 api

Teste limpo no WhatsApp:
1. menu
2. 2
3. IMETRO-2026-TESTE-002
4. 1
5. recebi
6. paguei
7. obrigado

Teste com linguagem livre:
1. Quero boleto do mês
2. IMETRO-2026-TESTE-002
3. 1
4. recebi
5. paguei
6. obrigado

Confirmação no banco:
docker compose exec -T postgres sh -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"' <<'SQL'
select charge_code, description, reference_month, status, paid_at
from charges
order by created_at desc
limit 10;

select id, charge_id, file_url, status, reviewed_at, review_note
from payment_proofs
where file_url like 'mock-bank-payment://%'
order by submitted_at desc
limit 10;

select receipt_code, status, pdf_url, validation_url, issued_at
from receipts
order by created_at desc
limit 10;
SQL
