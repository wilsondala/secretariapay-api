Correção Fase 21-23 — packages dos enums

Problema:
Os enums da Fase 21-23 foram criados com package começando em:
secretariapay.api...

Correto:
com.secretariapay.api...

Isso causava:
duplicate class: secretariapay.api...
bad source file: file does not contain class com.secretariapay.api...

Aplicar:
cd C:\Users\dalaw\secretariapay-api
Expand-Archive -Path "$env:USERPROFILE\Downloads\secretariapay-fase-21-23-fix-enum-packages.zip" -DestinationPath . -Force

Build:
if (Test-Path .\mvnw.cmd) {
  .\mvnw.cmd clean package -DskipTests
} else {
  mvn clean package -DskipTests
}

Git:
git status
git add .
git commit -m "fix: correct enum package names for academic campaigns restrictions"
git push origin main
