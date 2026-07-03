#!/usr/bin/env bash
set -euo pipefail

API_BASE_URL="${API_BASE_URL:-https://secretariapay-api.paixaoangola.com}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@secretariapay.com}"
CHARGE_ID="${CHARGE_ID:-}"
PAYMENT_PROOF_ID="${PAYMENT_PROOF_ID:-}"
REVIEWED_BY_USER_ID="${REVIEWED_BY_USER_ID:-}"

if [ -z "${ADMIN_PASS:-}" ]; then
  read -s -p "Senha admin: " ADMIN_PASS
  echo
fi

JWT=$(curl -s -X POST "$API_BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASS\"}" \
  | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')

echo "JWT length: ${#JWT}"

if [ -n "$CHARGE_ID" ]; then
  echo "\n== Enviando guia de pagamento =="
  curl -s -X POST "$API_BASE_URL/api/v1/secretariapay/financial-flow/charges/$CHARGE_ID/send-guide" \
    -H "Authorization: Bearer $JWT" | tee /tmp/secretariapay-send-guide.json
  echo
fi

if [ -n "$PAYMENT_PROOF_ID" ] && [ -n "$REVIEWED_BY_USER_ID" ]; then
  echo "\n== Aprovando comprovativo, emitindo recibo e notificando aluno =="
  curl -s -X POST "$API_BASE_URL/api/v1/secretariapay/financial-flow/payment-proofs/$PAYMENT_PROOF_ID/approve-complete" \
    -H "Authorization: Bearer $JWT" \
    -H "Content-Type: application/json" \
    -d "{\"reviewedByUserId\":\"$REVIEWED_BY_USER_ID\",\"reviewNote\":\"Aprovado pelo fluxo completo do SecretáriaPay.\"}" \
    | tee /tmp/secretariapay-approve-complete.json
  echo
fi

cat <<MSG

Uso rápido:
CHARGE_ID="..." ADMIN_PASS="..." bash scripts/test-secretariapay-financial-flow.sh

Fluxo completo com aprovação:
CHARGE_ID="..." PAYMENT_PROOF_ID="..." REVIEWED_BY_USER_ID="..." ADMIN_PASS="..." bash scripts/test-secretariapay-financial-flow.sh
MSG
