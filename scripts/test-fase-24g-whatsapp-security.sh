#!/usr/bin/env bash
set -euo pipefail

API="${API:-https://secretariapay-api.paixaoangola.com}"
INSTITUTION_ID="${INSTITUTION_ID:-c3726494-46b5-4563-8e84-0a04334fac8c}"

if [ -z "${TOKEN:-}" ]; then
  echo "Defina TOKEN antes de executar."
  exit 1
fi

api_post() {
  curl -s -X POST "$API$1" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "$2"
  echo
}

echo "== Envio seguro sem reenvio duplicado =="
api_post "/api/v1/imetro/tuition-charges/send-guides" '{
  "institutionId":"'"$INSTITUTION_ID"'",
  "referenceMonth":"2026-07",
  "chargeCodePrefix":"IMT-PROPINA-",
  "sendWhatsapp":true,
  "sendEmail":true,
  "sendSms":true,
  "onlyPending":true,
  "forceResend":false,
  "maxItems":10
}'
