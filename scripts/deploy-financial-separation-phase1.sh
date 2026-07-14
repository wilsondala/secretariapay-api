#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_DIR="${PROJECT_DIR:-/opt/secretariapay-api}"
TARGET_BRANCH="${TARGET_BRANCH:-fix/official-financial-flow-phase1}"
API_CONTAINER="${API_CONTAINER:-secretariapay-api}"
DB_CONTAINER="${DB_CONTAINER:-SecretariaPay-postgres}"
PUBLIC_API="${PUBLIC_API:-https://secretariapay-api.paixaoangola.com}"
BACKUP_ROOT="${BACKUP_ROOT:-/opt/backups/secretariapay-api}"
STUDENT_NUMBER="${STUDENT_NUMBER:-202301404}"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
BACKUP_DIR="${BACKUP_ROOT}/${TIMESTAMP}-financial-separation"

log() { printf '\n[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"; }
fail() { printf '\n[ERRO] %s\n' "$*" >&2; exit 1; }

command -v git >/dev/null || fail "git não encontrado"
command -v docker >/dev/null || fail "docker não encontrado"
command -v curl >/dev/null || fail "curl não encontrado"
docker compose version >/dev/null || fail "docker compose não disponível"
[[ -d "${PROJECT_DIR}/.git" ]] || fail "repositório não encontrado em ${PROJECT_DIR}"

cd "${PROJECT_DIR}"
mkdir -p "${BACKUP_DIR}"

log "Validar repositório"
git status --short
[[ -z "$(git status --porcelain)" ]] || fail "há alterações locais; interrompido por segurança"
git rev-parse --abbrev-ref HEAD | tee "${BACKUP_DIR}/branch-before.txt"
git rev-parse HEAD | tee "${BACKUP_DIR}/commit-before.txt"

log "Backup da configuração"
[[ -f .env.production ]] || fail ".env.production não encontrado"
cp -a .env.production "${BACKUP_DIR}/.env.production"
cp -a docker-compose.yml "${BACKUP_DIR}/docker-compose.yml"

log "Backup completo do PostgreSQL"
docker inspect "${DB_CONTAINER}" >/dev/null 2>&1 || fail "container PostgreSQL não encontrado"
docker exec "${DB_CONTAINER}" sh -lc 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists --no-owner --no-privileges' \
  | gzip -9 > "${BACKUP_DIR}/database.sql.gz"
[[ -s "${BACKUP_DIR}/database.sql.gz" ]] || fail "backup do banco ficou vazio"
sha256sum "${BACKUP_DIR}/database.sql.gz" > "${BACKUP_DIR}/database.sql.gz.sha256"

log "Atualizar branch controlada"
git fetch --prune origin
git switch "${TARGET_BRANCH}"
git pull --ff-only origin "${TARGET_BRANCH}"
git rev-parse HEAD | tee "${BACKUP_DIR}/commit-deployed.txt"

log "Confirmar proteção do ambiente institucional"
if grep -Eq '^SECRETARIAPAY_INFINITEPAY_TEST_ENABLED=true' .env.production; then
  fail "InfinitePay de teste está ativado na produção"
fi
printf 'InfinitePay teste: desativado\n' | tee "${BACKUP_DIR}/environment-validation.txt"

log "Build da API"
docker compose build --pull api

log "Subir API e aplicar Flyway"
docker compose up -d postgres
for attempt in $(seq 1 30); do
  docker exec "${DB_CONTAINER}" sh -lc 'pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"' >/dev/null 2>&1 && break
  [[ "${attempt}" -lt 30 ]] || fail "PostgreSQL não ficou pronto"
  sleep 2
done
docker compose up -d --no-deps api

log "Aguardar health local"
for attempt in $(seq 1 60); do
  if curl --fail --silent http://127.0.0.1:8080/actuator/health >/dev/null 2>&1; then break; fi
  if ! docker inspect -f '{{.State.Running}}' "${API_CONTAINER}" 2>/dev/null | grep -qx true; then
    docker logs --tail 250 "${API_CONTAINER}" || true
    fail "API parou durante a inicialização"
  fi
  [[ "${attempt}" -lt 60 ]] || { docker logs --tail 250 "${API_CONTAINER}" || true; fail "API não respondeu ao health"; }
  sleep 2
done

log "Validar migration financeira"
docker exec -i "${DB_CONTAINER}" sh -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1' <<'SQL' \
  | tee "${BACKUP_DIR}/flyway-validation.txt"
SELECT installed_rank, version, description, success
FROM flyway_schema_history
WHERE version = '203607141000';
SQL
grep -q "separate tuition and academic services" "${BACKUP_DIR}/flyway-validation.txt" \
  || fail "migration 203607141000 não encontrada"

log "Validar histórico do estudante ${STUDENT_NUMBER}"
docker exec -i "${DB_CONTAINER}" sh -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1' <<SQL \
  | tee "${BACKUP_DIR}/student-financial-classification.txt"
SELECT
  c.charge_code,
  c.description,
  c.reference_month,
  c.charge_category,
  c.service_code,
  c.status,
  c.total_amount,
  c.paid_at
FROM charges c
JOIN students s ON s.id = c.student_id
WHERE s.student_number = '${STUDENT_NUMBER}'
ORDER BY c.due_date, c.created_at;
SQL

grep -q "TUITION" "${BACKUP_DIR}/student-financial-classification.txt" \
  || fail "nenhuma propina foi classificada para o estudante teste"

log "Totais separados do estudante"
docker exec -i "${DB_CONTAINER}" sh -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1' <<SQL \
  | tee "${BACKUP_DIR}/student-financial-totals.txt"
SELECT
  c.charge_category,
  COUNT(*) AS lancamentos,
  COUNT(*) FILTER (WHERE c.status = 'PAID') AS pagos,
  COALESCE(SUM(c.total_amount) FILTER (WHERE c.status = 'PAID'), 0) AS total_pago,
  COALESCE(SUM(c.total_amount) FILTER (WHERE c.status NOT IN ('PAID','CANCELLED','RENEGOTIATED')), 0) AS saldo_aberto
FROM charges c
JOIN students s ON s.id = c.student_id
WHERE s.student_number = '${STUDENT_NUMBER}'
GROUP BY c.charge_category
ORDER BY c.charge_category;
SQL

log "Recibos separados por categoria"
docker exec -i "${DB_CONTAINER}" sh -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1' <<SQL \
  | tee "${BACKUP_DIR}/student-receipt-categories.txt"
SELECT
  c.charge_category,
  COUNT(r.id) AS recibos_validos,
  COALESCE(SUM(c.total_amount), 0) AS total_documentado
FROM receipts r
JOIN charges c ON c.id = r.charge_id
JOIN students s ON s.id = c.student_id
WHERE s.student_number = '${STUDENT_NUMBER}'
  AND r.status = 'VALID'
GROUP BY c.charge_category
ORDER BY c.charge_category;
SQL

log "Health checks finais"
curl --fail --silent http://127.0.0.1:8080/actuator/health | tee "${BACKUP_DIR}/health-local.json"
printf '\n'
curl --fail --silent "${PUBLIC_API}/actuator/health" | tee "${BACKUP_DIR}/health-public.json"
printf '\n'
curl --fail --silent "${PUBLIC_API}/api/v1/health" | tee "${BACKUP_DIR}/health-api.json"
printf '\n'

log "Estado final"
docker compose ps | tee "${BACKUP_DIR}/compose-after.txt"
docker logs --tail 120 "${API_CONTAINER}" | tee "${BACKUP_DIR}/api-last-logs.txt"

log "Deploy controlado concluído"
printf 'Branch: %s\n' "$(git rev-parse --abbrev-ref HEAD)"
printf 'Commit: %s\n' "$(git rev-parse HEAD)"
printf 'Backup: %s\n' "${BACKUP_DIR}"
printf 'Próximo teste: Preview Vercel em /students, /proofs, /receipts, /operations e /reports; depois menu real do WhatsApp.\n'
