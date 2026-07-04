Fase 24A — Importação de Serviços e Totais IMETRO

Objetivo:
Adicionar no banco os serviços partilhados pelo cliente na tabela Serviço x Total [Akz].

Importante:
- Os valores são totais agregados em AKZ, não preços unitários definitivos.
- A tabela já prepara o catálogo para cobrança futura, mas preserva a origem como total reportado.
- Não altera o fluxo atual de guia, recibo, WhatsApp, DCR ou campanhas.

Tabelas:
1. institution_charge_service_imports
2. institution_charge_service_configs

Migration:
src/main/resources/db/migration/V202607041200__fase_24a_import_servicos_imetro.sql

Aplicar:
cd C:\Users\dalaw\secretariapay-api

Expand-Archive -Path "$env:USERPROFILE\Downloads\secretariapay-fase-24a-import-servicos-imetro.zip" -DestinationPath . -Force

Build:
if (Test-Path .\mvnw.cmd) {
  .\mvnw.cmd clean package -DskipTests
} else {
  mvn clean package -DskipTests
}

Git:
git status
git add .
git commit -m "feat: import IMETRO service revenue catalog"
git push origin main

Produção:
cd /opt/secretariapay-api
git pull origin main
docker compose up -d --build
docker compose logs --tail=120 api

Testes no banco:
docker compose exec -T postgres sh -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"' <<'SQL'
select import_code, reported_grand_total, source_file_name, created_at
from institution_charge_service_imports
order by created_at desc;

select count(*) as total_servicos,
       sum(total_reported_amount) as soma_servicos
from institution_charge_service_configs
where institution_id = 'c3726494-46b5-4563-8e84-0a04334fac8c';

select service_category,
       count(*) as qtd,
       sum(total_reported_amount) as total_akz
from institution_charge_service_configs
where institution_id = 'c3726494-46b5-4563-8e84-0a04334fac8c'
group by service_category
order by total_akz desc;

select service_name, service_category, total_reported_amount
from institution_charge_service_configs
where institution_id = 'c3726494-46b5-4563-8e84-0a04334fac8c'
order by total_reported_amount desc
limit 15;
SQL
