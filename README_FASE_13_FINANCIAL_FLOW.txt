Fase 13 — Motor Financeiro Completo do SecretáriaPay

Objetivo:
Criar endpoints orquestradores para executar o ciclo financeiro ponta-a-ponta sem precisar chamar vários endpoints manualmente.

Novo serviço:
- SecretariaPayFinancialFlowService

Novo controller:
- /api/v1/secretariapay/financial-flow

Novos endpoints:
1) Enviar guia de pagamento completa pelo WhatsApp:
POST /api/v1/secretariapay/financial-flow/charges/{chargeId}/send-guide

Executa:
- gera mensagem PAYMENT_GUIDE
- envia PDF da guia pelo WhatsApp
- retorna status SENT/FAILED com providerMessageId

2) Aprovar comprovativo, emitir recibo e notificar aluno:
POST /api/v1/secretariapay/financial-flow/payment-proofs/{paymentProofId}/approve-complete

Body:
{
  "reviewedByUserId": "UUID_DO_ADMIN_OU_TESOUREIRO",
  "reviewNote": "Pagamento confirmado pela tesouraria."
}

Executa:
- aprova comprovativo
- marca cobrança como PAID
- emite recibo se ainda não existir
- gera mensagem RECEIPT_ISSUED
- envia PDF do recibo pelo WhatsApp

3) Rejeitar comprovativo:
POST /api/v1/secretariapay/financial-flow/payment-proofs/{paymentProofId}/reject-complete

Body:
{
  "reviewedByUserId": "UUID_DO_ADMIN_OU_TESOUREIRO",
  "reviewNote": "Comprovativo ilegível ou valor divergente."
}

Executa:
- rejeita comprovativo
- mantém cobrança aberta
- gera mensagem PAYMENT_PROOF_REJECTED
- envia aviso ao aluno pelo WhatsApp com o motivo

Aplicação local com padrão fixo:
cd C:\Users\dalaw\secretariapay-api

Expand-Archive -Path "$env:USERPROFILE\Downloads\secretariapay-fase-13-financial-flow.zip" -DestinationPath . -Force

if (Test-Path .\mvnw.cmd) {
  .\mvnw.cmd clean package -DskipTests
} else {
  mvn clean package -DskipTests
}

git status
git add .
git commit -m "feat: add complete financial flow orchestrator"
git push origin main

Produção:
cd /opt/secretariapay-api
git pull origin main
docker compose up -d --build
docker compose logs -f --tail=200 api

Teste rápido na VPS:
CHARGE_ID="5f929dfa-7dbb-45e5-807e-1a83898ead66" bash scripts/test-secretariapay-financial-flow.sh

Fluxo completo com aprovação:
CHARGE_ID="..." PAYMENT_PROOF_ID="..." REVIEWED_BY_USER_ID="..." bash scripts/test-secretariapay-financial-flow.sh
