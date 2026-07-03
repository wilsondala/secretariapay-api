Fase 16A — Ajuste de lógica de pendências x atrasos no WhatsApp

Problema corrigido:
- A opção 3 dizia que não havia mensalidades em atraso.
- A opção 6 dizia que havia pendências.
- Isso não era necessariamente erro de banco: atraso é cobrança vencida; pendência pode ser cobrança aberta ainda dentro do vencimento.
- O robô estava comunicando isso de forma confusa.

Nova regra:
1. Mensalidade vencida/em atraso:
   - status diferente de PAID/CANCELLED
   - due_date menor que hoje

2. Mensalidade pendente/a vencer:
   - status diferente de PAID/CANCELLED
   - due_date hoje ou futura

3. Opção 3:
   - se não houver atraso, mas houver pendência, informa:
     "Não encontrei mensalidades vencidas/em atraso. Existem mensalidades pendentes/a vencer."
   - oferece gerar guia do mês.

4. Opção 6:
   - mostra separado:
     Mensalidades pendentes/a vencer
     Mensalidades vencidas/em atraso
     Total em aberto
     Total vencido/em atraso

5. Opção 7:
   - só abre negociação automática se houver cobrança vencida/em atraso.
   - se houver apenas pendência a vencer, orienta gerar guia ou falar com secretaria.

Aplicar:
cd C:\Users\dalaw\secretariapay-api
Expand-Archive -Path "$env:USERPROFILE\Downloads\secretariapay-fase-16a-ajuste-logica-pendencias.zip" -DestinationPath . -Force

Build:
if (Test-Path .\mvnw.cmd) { .\mvnw.cmd clean package -DskipTests } else { mvn clean package -DskipTests }

git status
git add .
git commit -m "fix: separate overdue and pending charges in WhatsApp finance flow"
git push origin main

Produção:
cd /opt/secretariapay-api
git pull origin main
docker compose up -d --build
docker compose logs -f --tail=200 api

Teste WhatsApp:
menu
3
6
7
