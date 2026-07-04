Fase 24E — Gerar propinas/cobranças reais para estudantes sincronizados

Objetivo:
Criar endpoint para gerar cobranças de propina para estudantes reais já sincronizados do WebSchool
com o cadastro académico real do SecretáriaPay.

Contexto validado:
- Já existem students, courses, academic_classes e charges em produção.
- Fase 24C sincronizou staging WebSchool com cadastro académico real.
- Fase 24D validou regras DCR/IMETRO:
  * pagamento sem multa até dia 10
  * 20% depois do dia 10
  * 30% no dia 15
  * sem juros diário
  * dívida após 30 dias
  * inadimplência após 90 dias
  * pagamento automático apenas provisório
  * confirmação manual obrigatória pela DCR

Novo endpoint:
POST /api/v1/imetro/tuition-charges/generate

O endpoint:
- filtra estudantes por instituição
- permite filtrar por ano académico, curso, turma ou lista de estudantes
- gera cobrança de propina com ChargeStatus.PENDING
- aplica multa conforme política DCR quando referenceDate estiver em atraso
- é idempotente por código de cobrança
- não duplica cobranças ao rodar novamente
- deixa a cobrança pronta para geração de guia PDF e envio WhatsApp

Arquivos adicionados:
1. src/main/java/com/secretariapay/api/dto/financial/TuitionChargeGenerationRequest.java
2. src/main/java/com/secretariapay/api/dto/financial/TuitionChargeGeneratedItem.java
3. src/main/java/com/secretariapay/api/dto/financial/TuitionChargeGenerationResponse.java
4. src/main/java/com/secretariapay/api/service/financial/TuitionChargeGenerationService.java
5. src/main/java/com/secretariapay/api/controller/financial/TuitionChargeGenerationController.java
6. scripts/test-fase-24e-tuition-charges.sh

Aplicar local:
cd C:\Users\dalaw\secretariapay-api

Expand-Archive -Path "$env:USERPROFILE\Downloads\secretariapay-fase-24e-gerar-propinas-reais.zip" -DestinationPath . -Force

Build:
if (Test-Path .\mvnw.cmd) {
  .\mvnw.cmd clean package -DskipTests
} else {
  mvn clean package -DskipTests
}

Git:
git status
git add .
git commit -m "feat: generate IMETRO tuition charges"
git push origin main

Produção:
cd /opt/secretariapay-api
git pull origin main
docker compose up -d --build
docker compose logs -f --tail=200 api

Teste rápido com helpers:
export API='https://secretariapay-api.paixaoangola.com'
export INSTITUTION_ID='c3726494-46b5-4563-8e84-0a04334fac8c'
export TOKEN='SEU_TOKEN_ADMIN'

api_post() {
  curl -s -X POST "$API$1" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "$2"
}

api_post "/api/v1/imetro/tuition-charges/generate" '{
  "institutionId":"c3726494-46b5-4563-8e84-0a04334fac8c",
  "academicYear":"2024/2025",
  "referenceMonth":"2026-07",
  "dueDate":"2026-07-30",
  "referenceDate":"2026-07-04",
  "baseAmount":45000,
  "serviceCode":"PROPINA",
  "descriptionPrefix":"Propina"
}'

Teste de idempotência:
rodar o mesmo comando novamente.
Resultado esperado:
- na primeira execução: createdCharges > 0
- na segunda execução: createdCharges = 0 e reusedCharges > 0
