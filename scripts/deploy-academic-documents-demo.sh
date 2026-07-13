#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_DIR="${PROJECT_DIR:-/opt/secretariapay-api}"
TARGET_BRANCH="${TARGET_BRANCH:-feat/academic-documents-demo}"
API_CONTAINER="${API_CONTAINER:-secretariapay-api}"
DB_CONTAINER="${DB_CONTAINER:-SecretariaPay-postgres}"
PUBLIC_API="${PUBLIC_API:-https://secretariapay-api.paixaoangola.com}"
BACKUP_ROOT="${BACKUP_ROOT:-/opt/backups/secretariapay-api}"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
BACKUP_DIR="${BACKUP_ROOT}/${TIMESTAMP}"

log() {
  printf '\n[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"
}

fail() {
  printf '\n[ERRO] %s\n' "$*" >&2
  exit 1
}

command -v git >/dev/null 2>&1 || fail "git não encontrado"
command -v docker >/dev/null 2>&1 || fail "docker não encontrado"
command -v curl >/dev/null 2>&1 || fail "curl não encontrado"

docker compose version >/dev/null 2>&1 || fail "docker compose não disponível"
[[ -d "${PROJECT_DIR}/.git" ]] || fail "repositório não encontrado em ${PROJECT_DIR}"

cd "${PROJECT_DIR}"
mkdir -p "${BACKUP_DIR}"

log "Estado atual"
git status --short
git rev-parse --abbrev-ref HEAD | tee "${BACKUP_DIR}/branch-before.txt"
git rev-parse HEAD | tee "${BACKUP_DIR}/commit-before.txt"
docker compose ps | tee "${BACKUP_DIR}/compose-before.txt"

if [[ -n "$(git status --porcelain)" ]]; then
  fail "há alterações locais no repositório. Guarde ou reverta antes do deploy"
fi

log "Backup das configurações"
[[ -f .env.production ]] || fail ".env.production não encontrado"
cp -a .env.production "${BACKUP_DIR}/.env.production"
cp -a docker-compose.yml "${BACKUP_DIR}/docker-compose.yml"

log "Backup do PostgreSQL"
docker inspect "${DB_CONTAINER}" >/dev/null 2>&1 || fail "container ${DB_CONTAINER} não encontrado"
docker exec "${DB_CONTAINER}" sh -lc 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists --no-owner --no-privileges' \
  | gzip -9 > "${BACKUP_DIR}/database.sql.gz"
[[ -s "${BACKUP_DIR}/database.sql.gz" ]] || fail "backup do banco ficou vazio"
sha256sum "${BACKUP_DIR}/database.sql.gz" > "${BACKUP_DIR}/database.sql.gz.sha256"

log "Atualização da branch ${TARGET_BRANCH}"
git fetch --prune origin
git switch "${TARGET_BRANCH}"
git pull --ff-only origin "${TARGET_BRANCH}"
git rev-parse HEAD | tee "${BACKUP_DIR}/commit-deployed.txt"

log "Build da imagem da API"
docker compose build --pull api

log "Subida controlada dos serviços"
docker compose up -d postgres
for attempt in $(seq 1 30); do
  if docker exec "${DB_CONTAINER}" sh -lc 'pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"' >/dev/null 2>&1; then
    break
  fi
  [[ "${attempt}" -lt 30 ]] || fail "PostgreSQL não ficou pronto"
  sleep 2
done

docker compose up -d --no-deps api

log "Aguardar inicialização da API"
for attempt in $(seq 1 45); do
  if curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health >/dev/null 2>&1; then
    break
  fi
  if ! docker inspect -f '{{.State.Running}}' "${API_CONTAINER}" 2>/dev/null | grep -qx true; then
    docker logs --tail 200 "${API_CONTAINER}" || true
    fail "container da API parou durante a inicialização"
  fi
  [[ "${attempt}" -lt 45 ]] || {
    docker logs --tail 250 "${API_CONTAINER}" || true
    fail "API não respondeu ao health check local"
  }
  sleep 2
done

log "Validação das migrations"
docker exec -i "${DB_CONTAINER}" sh -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1' <<'SQL' \
  | tee "${BACKUP_DIR}/flyway-validation.txt"
SELECT installed_rank, version, description, success
FROM flyway_schema_history
WHERE version IN ('203607131900', '203607132000')
ORDER BY installed_rank;
SQL

grep -q "configure official academic service prices" "${BACKUP_DIR}/flyway-validation.txt" \
  || fail "migration de preços não encontrada no histórico"
grep -q "create academic document requests" "${BACKUP_DIR}/flyway-validation.txt" \
  || fail "migration de documentos não encontrada no histórico"

log "Validação da tabela de preços"
docker exec -i "${DB_CONTAINER}" sh -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1' <<'SQL' \
  | tee "${BACKUP_DIR}/catalog-validation.txt"
SELECT code, name, unit_price, currency, active, available_whatsapp
FROM academic_service_catalog
WHERE code IN (
  'TUITION', 'ENROLLMENT', 'ENROLLMENT_CONFIRMATION',
  'REGISTRATION', 'RESIT_EXAM', 'SPECIAL_EXAM',
  'DECLARATION_WITH_GRADES', 'DECLARATION_WITHOUT_GRADES',
  'CERTIFICATE', 'DIPLOMA'
)
ORDER BY display_order, code;
SQL

log "Health checks finais"
curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health | tee "${BACKUP_DIR}/health-local.json"
printf '\n'
curl --fail --silent --show-error "${PUBLIC_API}/actuator/health" | tee "${BACKUP_DIR}/health-public.json"
printf '\n'
curl --fail --silent --show-error "${PUBLIC_API}/api/v1/health" | tee "${BACKUP_DIR}/health-api.json"
printf '\n'

log "Containers finais"
docker compose ps | tee "${BACKUP_DIR}/compose-after.txt"

log "Últimos logs da API"
docker logs --tail 120 "${API_CONTAINER}" | tee "${BACKUP_DIR}/api-last-logs.txt"

log "Deploy controlado concluído"
printf 'Branch: %s\n' "$(git rev-parse --abbrev-ref HEAD)"
printf 'Commit: %s\n' "$(git rev-parse HEAD)"
printf 'Backup: %s\n' "${BACKUP_DIR}"
printf 'Próximo teste: gerar novamente a declaração e validar logos, acentos, QR e envio pelo WhatsApp.\n'
