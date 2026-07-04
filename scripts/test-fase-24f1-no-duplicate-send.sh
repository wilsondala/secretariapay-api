#!/usr/bin/env bash
set -euo pipefail

API="${API:-https://secretariapay-api.paixaoangola.com}"
INSTITUTION_ID="${INSTITUTION_ID:-c3726494-46b5-4563-8e84-0a04334fac8c}"
REFERENCE_MONTH="${REFERENCE_MONTH:-2026-07}"

if [ -z "${TOKEN:-}" ]; then
  echo "TOKEN não definido. Faça login e exporte TOKEN antes de rodar." >&2
  exit 1
fi

echo "== Teste 24F.1: envio normal sem forceResend =="
curl -s -X POST "$API/api/v1/imetro/tuition-charges/send-guides" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"institutionId\":\"$INSTITUTION_ID\",\"referenceMonth\":\"$REFERENCE_MONTH\",\"chargeCodePrefix\":\"IMT-PROPINA-\",\"sendWhatsapp\":true,\"sendEmail\":true,\"sendSms\":true,\"onlyPending\":true,\"forceResend\":false,\"maxItems\":10}"

echo

echo "== Reenvio manual autorizado, apenas quando necessário =="
echo "Para forçar reenvio, rode com forceResend=true no body. Não use em lote real sem autorização da DCR."
