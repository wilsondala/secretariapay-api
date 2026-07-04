#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-https://secretariapay-api.paixaoangola.com}"
INSTITUTION_ID="${INSTITUTION_ID:-c3726494-46b5-4563-8e84-0a04334fac8c}"
TOKEN="${TOKEN:-}"

if [ -z "$TOKEN" ]; then
  echo "ERRO: defina TOKEN antes de executar. Exemplo:" >&2
  echo "TOKEN=... bash scripts/test-imetro-dcr-policy.sh" >&2
  exit 1
fi

echo "== Política DCR ativa do IMETRO =="
curl -s "$BASE_URL/api/v1/institution-dcr-policies/institution/$INSTITUTION_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"

echo

echo "== Simulação sem multa, dia 10 =="
curl -s -X POST "$BASE_URL/api/v1/institution-dcr-policies/institution/$INSTITUTION_ID/evaluate" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"baseAmount":45000,"dueDate":"2026-07-01","referenceDate":"2026-07-10"}'

echo

echo "== Simulação multa 20%, dia 11 =="
curl -s -X POST "$BASE_URL/api/v1/institution-dcr-policies/institution/$INSTITUTION_ID/evaluate" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"baseAmount":45000,"dueDate":"2026-07-01","referenceDate":"2026-07-11"}'

echo

echo "== Simulação multa 30%, dia 15 =="
curl -s -X POST "$BASE_URL/api/v1/institution-dcr-policies/institution/$INSTITUTION_ID/evaluate" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"baseAmount":45000,"dueDate":"2026-07-01","referenceDate":"2026-07-15"}'

echo

echo "== Simulação dívida após 30 dias =="
curl -s -X POST "$BASE_URL/api/v1/institution-dcr-policies/institution/$INSTITUTION_ID/evaluate" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"baseAmount":45000,"dueDate":"2026-07-01","referenceDate":"2026-07-31"}'

echo
