#!/usr/bin/env bash
set -euo pipefail

API="${API:-https://secretariapay-api.paixaoangola.com}"
INSTITUTION_ID="${INSTITUTION_ID:-c3726494-46b5-4563-8e84-0a04334fac8c}"

if [ -z "${TOKEN:-}" ]; then
  echo "Defina TOKEN antes de rodar: export TOKEN='...'"
  exit 1
fi

api_post() {
  curl -s -X POST "$API$1" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "$2"
}

echo
echo "== Fase 24E | Propina sem multa | 2024/2025 =="
api_post "/api/v1/imetro/tuition-charges/generate" "{
  \"institutionId\":\"$INSTITUTION_ID\",
  \"academicYear\":\"2024/2025\",
  \"referenceMonth\":\"2026-07\",
  \"dueDate\":\"2026-07-30\",
  \"referenceDate\":\"2026-07-04\",
  \"baseAmount\":45000,
  \"serviceCode\":\"PROPINA\",
  \"descriptionPrefix\":\"Propina\"
}"
echo

echo
echo "== Fase 24E | Rodar novamente para validar idempotência =="
api_post "/api/v1/imetro/tuition-charges/generate" "{
  \"institutionId\":\"$INSTITUTION_ID\",
  \"academicYear\":\"2024/2025\",
  \"referenceMonth\":\"2026-07\",
  \"dueDate\":\"2026-07-30\",
  \"referenceDate\":\"2026-07-04\",
  \"baseAmount\":45000,
  \"serviceCode\":\"PROPINA\",
  \"descriptionPrefix\":\"Propina\"
}"
echo

echo
echo "== Fase 24E | Simulação de propina vencida com multa DCR 30% =="
api_post "/api/v1/imetro/tuition-charges/generate" "{
  \"institutionId\":\"$INSTITUTION_ID\",
  \"academicYear\":\"2024/2025\",
  \"referenceMonth\":\"2026-06\",
  \"dueDate\":\"2026-06-01\",
  \"referenceDate\":\"2026-06-15\",
  \"baseAmount\":45000,
  \"serviceCode\":\"PROPINA\",
  \"descriptionPrefix\":\"Propina vencida\"
}"
echo
