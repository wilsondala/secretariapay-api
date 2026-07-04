Fase 24B — Importação Real de Alunos IMETRO / WebSchool

Objetivo:
Criar uma camada de staging para importar os alunos vindos do sistema académico real
WebSchool/AdminUT sem quebrar a tabela atual de estudantes.

Estrutura detectada no print do cliente:
- Ano Lectivo
- Semestre
- Nr. Estudante
- Nome
- Curso
- Turma
- Ações: Recibo / Renegociar

Tabelas:
- academic_student_import_batches
- academic_student_import_rows

Endpoints:
POST  /api/v1/academic-student-imports
GET   /api/v1/academic-student-imports
GET   /api/v1/academic-student-imports/institution/{institutionId}
GET   /api/v1/academic-student-imports/{id}
POST  /api/v1/academic-student-imports/{id}/rows
GET   /api/v1/academic-student-imports/{id}/rows
PATCH /api/v1/academic-student-imports/{id}/validate
PATCH /api/v1/academic-student-imports/{id}/complete

Migration:
V203001010900__fase_24b_importacao_alunos_webschool.sql

A versão foi colocada alta de propósito para não voltar a causar erro de Flyway out-of-order.

Aplicar:
cd C:\Users\dalaw\secretariapay-api

Expand-Archive -Path "$env:USERPROFILE\Downloads\secretariapay-fase-24b-importacao-alunos-webschool.zip" -DestinationPath . -Force

Build:
if (Test-Path .\mvnw.cmd) {
  .\mvnw.cmd clean package -DskipTests
} else {
  mvn clean package -DskipTests
}

Git:
git status
git add .
git commit -m "feat: add WebSchool student import staging"
git push origin main

Produção:
cd /opt/secretariapay-api
git pull origin main
docker compose up -d --build
docker compose logs --tail=120 api
