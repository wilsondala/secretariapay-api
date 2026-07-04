Fase 24C — Sincronizar staging WebSchool com students/courses/classes reais

Objetivo:
Ligar as linhas importadas do WebSchool/AdminUT ao cadastro académico real do SecretáriaPay.

Tabelas envolvidas:
- academic_student_import_batches
- academic_student_import_rows
- courses
- academic_classes
- students

Novo endpoint:
PATCH /api/v1/academic-student-imports/{id}/sync

Regras implementadas:
1. Lê linhas VALID, IMPORTED ou SYNCED do staging WebSchool.
2. Encontra ou cria o curso em courses usando institution_id + nome do curso.
3. Encontra ou cria a turma em academic_classes usando course_id + nome + ano académico + turno.
4. Encontra ou cria/atualiza o estudante em students usando student_number.
5. Preenche academic_student_import_rows.matched_student_id.
6. Marca a linha como SYNCED.
7. Mantém a execução idempotente: rodar novamente não duplica estudante, curso ou turma.

Arquivos alterados/criados:
- src/main/java/com/secretariapay/api/dto/imports/AcademicStudentImportSyncResponse.java
- src/main/java/com/secretariapay/api/entity/enums/imports/AcademicStudentImportRowStatus.java
- src/main/java/com/secretariapay/api/repository/academic/CourseRepository.java
- src/main/java/com/secretariapay/api/repository/academic/AcademicClassRepository.java
- src/main/java/com/secretariapay/api/repository/imports/AcademicStudentImportRowRepository.java
- src/main/java/com/secretariapay/api/service/imports/AcademicStudentImportService.java
- src/main/java/com/secretariapay/api/controller/imports/AcademicStudentImportController.java

Aplicar local:
cd C:\Users\dalaw\secretariapay-api

Expand-Archive -Path "$env:USERPROFILE\Downloads\secretariapay-fase-24c-sync-webschool-staging.zip" -DestinationPath . -Force

Build:
if (Test-Path .\mvnw.cmd) {
  .\mvnw.cmd clean package -DskipTests
} else {
  mvn clean package -DskipTests
}

Git:
git status
git add .
git commit -m "feat: sync WebSchool staging with academic records"
git push origin main

Produção:
cd /opt/secretariapay-api
git pull origin main
docker compose up -d --build
docker compose logs -f --tail=200 api

Teste com token:
TOKEN="SEU_TOKEN"
BATCH_ID="ID_DO_LOTE"

curl -X PATCH "https://secretariapay-api.paixaoangola.com/api/v1/academic-student-imports/$BATCH_ID/sync" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"

Consulta SQL de validação:
select
  b.import_code,
  b.status,
  b.total_rows,
  b.valid_rows,
  b.invalid_rows,
  b.imported_rows,
  count(r.id) filter (where r.status = 'SYNCED') as synced_rows,
  count(r.id) filter (where r.matched_student_id is not null) as matched_rows
from academic_student_import_batches b
left join academic_student_import_rows r on r.batch_id = b.id
where b.import_code = 'WSI-1783164337518'
group by b.import_code, b.status, b.total_rows, b.valid_rows, b.invalid_rows, b.imported_rows;
