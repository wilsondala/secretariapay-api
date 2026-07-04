Fase 24B — Correção do contador imported_rows

Problema:
Ao executar o complete pela segunda vez, as linhas já estavam como IMPORTED.
O serviço contava apenas linhas VALID e sobrescrevia imported_rows com 0.

Correção:
completeBatch agora é idempotente:
- Linhas VALID viram IMPORTED
- Linhas já IMPORTED também entram na contagem
- imported_rows permanece correto mesmo se o complete for executado novamente.

Aplicar:
cd C:\Users\dalaw\secretariapay-api

Expand-Archive -Path "$env:USERPROFILE\Downloads\secretariapay-fase-24b-fix-imported-rows-counter.zip" -DestinationPath . -Force

Build:
if (Test-Path .\mvnw.cmd) {
  .\mvnw.cmd clean package -DskipTests
} else {
  mvn clean package -DskipTests
}

Git:
git status
git add .
git commit -m "fix: keep WebSchool imported rows counter idempotent"
git push origin main
