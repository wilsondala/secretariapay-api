Fase 24D — Regras institucionais IMETRO/DCR

Objetivo:
Registrar no backend as regras reais levantadas com o cliente IMETRO para propinas, multas, dívida, inadimplência, WhatsApp e confirmação manual pela DCR.

Esta fase NÃO substitui o fluxo financeiro existente e NÃO altera dados reais de cobrança.
Ela cria uma camada institucional de configuração e avaliação, segura para produção, para ser usada nas próximas fases de geração de propinas/cobranças reais.

Regras configuradas:
- Instituição: Universidade Metropolitana de Angola / IMETRO
- Política: IMETRO_DCR_2026
- Moeda: AOA
- Período de pagamento sem multa: até dia 10
- Multa depois do dia 10: 20%
- Multa depois do dia 15: 30%
- Juros diário: não
- Dívida: após 30 dias
- Inadimplência: após 90 dias
- Lembrete preventivo: 5 dias antes
- Cobrança compulsória: a partir do dia 22
- WhatsApp permitido: 07h às 19h
- Confirmação automática: apenas provisória
- Confirmação manual DCR: obrigatória
- Aprovador DCR: DCR_COORDENACAO
- E-mail oficial sugerido: dcr_pay@imetroangola.com
- Cópia sugerida: df.oi_pay@imetroangola.com

Arquivos adicionados:
- src/main/resources/db/migration/V203001011000__fase_24d_regras_dcr_imetro.sql
- src/main/java/com/secretariapay/api/entity/config/InstitutionDcrPolicy.java
- src/main/java/com/secretariapay/api/entity/enums/config/DcrChargeStatus.java
- src/main/java/com/secretariapay/api/dto/config/DcrChargeEvaluationRequest.java
- src/main/java/com/secretariapay/api/dto/config/DcrChargeEvaluationResponse.java
- src/main/java/com/secretariapay/api/dto/config/InstitutionDcrPolicyResponse.java
- src/main/java/com/secretariapay/api/repository/config/InstitutionDcrPolicyRepository.java
- src/main/java/com/secretariapay/api/service/config/InstitutionDcrPolicyService.java
- src/main/java/com/secretariapay/api/controller/config/InstitutionDcrPolicyController.java
- src/main/java/com/secretariapay/api/entity/enums/UserRole.java
- scripts/test-imetro-dcr-policy.sh

Novos endpoints:

1) Consultar política DCR ativa da instituição:
GET /api/v1/institution-dcr-policies/institution/{institutionId}

2) Simular/evaluar cobrança conforme regras DCR:
POST /api/v1/institution-dcr-policies/institution/{institutionId}/evaluate

Exemplo body:
{
  "baseAmount": 45000,
  "dueDate": "2026-07-01",
  "referenceDate": "2026-07-15"
}

Status possíveis:
- UPCOMING
- PAYMENT_WINDOW
- LATE_FIRST_PENALTY
- LATE_SECOND_PENALTY
- DEBT
- DELINQUENT

Aplicar local:
cd C:\Users\dalaw\secretariapay-api

Expand-Archive -Path "$env:USERPROFILE\Downloads\secretariapay-fase-24d-regras-dcr-imetro.zip" -DestinationPath . -Force

Build:
if (Test-Path .\mvnw.cmd) {
  .\mvnw.cmd clean package -DskipTests
} else {
  mvn clean package -DskipTests
}

Git:
git status
git add .
git commit -m "feat: add IMETRO DCR institutional rules"
git push origin main

Produção:
cd /opt/secretariapay-api
git pull origin main
docker compose up -d --build
docker compose logs -f --tail=200 api

Teste rápido em produção:
TOKEN="SEU_TOKEN_ADMIN"
INSTITUTION_ID="c3726494-46b5-4563-8e84-0a04334fac8c"

curl -s "https://secretariapay-api.paixaoangola.com/api/v1/institution-dcr-policies/institution/$INSTITUTION_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"

curl -s -X POST "https://secretariapay-api.paixaoangola.com/api/v1/institution-dcr-policies/institution/$INSTITUTION_ID/evaluate" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"baseAmount":45000,"dueDate":"2026-07-01","referenceDate":"2026-07-15"}'

Ou:
TOKEN="SEU_TOKEN_ADMIN" bash scripts/test-imetro-dcr-policy.sh

Validação no banco:
docker compose exec -T postgres sh -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" <<'"'"'SQL'"'"'
select
  policy_code,
  no_penalty_until_day,
  first_penalty_start_day,
  first_penalty_percent,
  second_penalty_start_day,
  second_penalty_percent,
  daily_interest_enabled,
  debt_after_days,
  delinquency_after_days,
  whatsapp_allowed_start,
  whatsapp_allowed_end,
  provisional_automatic_confirmation,
  manual_dcr_confirmation_required,
  dcr_approval_role,
  official_email,
  cc_email,
  active
from institution_dcr_policies
where policy_code = 'IMETRO_DCR_2026';
SQL'

Resultado esperado:
- first_penalty_percent = 20.00
- second_penalty_percent = 30.00
- daily_interest_enabled = false
- debt_after_days = 30
- delinquency_after_days = 90
- manual_dcr_confirmation_required = true

Próxima fase recomendada:
Fase 24E — Gerar propinas/cobranças reais com base nos estudantes sincronizados e na política DCR IMETRO.
