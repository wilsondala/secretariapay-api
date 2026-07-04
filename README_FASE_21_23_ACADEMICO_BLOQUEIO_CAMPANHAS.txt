
Fase 21-23 — Académico, Bloqueio Controlado e Campanhas

Cria:
- Solicitações Académicas
- Bloqueio/Desbloqueio Académico Controlado
- Campanhas e Avisos Automáticos em Massa

Aplicar:
cd C:\Users\dalaw\secretariapay-api
Expand-Archive -Path "$env:USERPROFILE\Downloads\secretariapay-fase-21-23-academico-bloqueio-campanhas.zip" -DestinationPath . -Force

Build:
if (Test-Path .\mvnw.cmd) { .\mvnw.cmd clean package -DskipTests } else { mvn clean package -DskipTests }

git status
git add .
git commit -m "feat: add academic requests restrictions and billing campaigns"
git push origin main

Produção:
cd /opt/secretariapay-api
git pull origin main
docker compose up -d --build
docker compose logs -f --tail=200 api
