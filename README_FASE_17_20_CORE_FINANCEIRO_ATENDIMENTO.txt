
Fase 17-20 — Core Financeiro e Atendimento

Inclui:
- Fase 17: support_tickets e support_ticket_messages
- Fase 18: financial_negotiations e financial_negotiation_installments
- Fase 19: payment_method_configs e bank_account_configs
- Fase 20: billing_rules

Endpoints:
POST/GET /api/v1/support-tickets
PATCH /api/v1/support-tickets/{id}/assign
PATCH /api/v1/support-tickets/{id}/close
POST /api/v1/support-tickets/{id}/messages

POST/GET /api/v1/financial-negotiations
PATCH /api/v1/financial-negotiations/{id}/approve
PATCH /api/v1/financial-negotiations/{id}/reject

POST/GET /api/v1/payment-configurations/methods
POST/GET /api/v1/payment-configurations/bank-accounts

POST/GET /api/v1/billing-rules

Aplicar:
cd C:\Users\dalaw\secretariapay-api
Expand-Archive -Path "$env:USERPROFILE\Downloads\secretariapay-fase-17-20-core-financeiro-atendimento.zip" -DestinationPath . -Force

Build:
if (Test-Path .\mvnw.cmd) { .\mvnw.cmd clean package -DskipTests } else { mvn clean package -DskipTests }

git status
git add .
git commit -m "feat: add core finance support negotiation payment config billing rules"
git push origin main

Produção:
cd /opt/secretariapay-api
git pull origin main
docker compose up -d --build
docker compose logs -f --tail=200 api
