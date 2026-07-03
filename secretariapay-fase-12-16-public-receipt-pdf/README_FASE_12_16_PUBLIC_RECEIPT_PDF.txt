Fase 12.16A — Corrigir link público do PDF do recibo

Problema:
- O WhatsApp enviou o link /api/v1/receipts/{id}/pdf.
- Esse endpoint é administrativo e exige JWT.
- Ao abrir no navegador, o aluno recebe 403 Forbidden.

Correção:
- Criar endpoint público:
  GET /api/v1/public/receipts/{receiptCode}/pdf
  GET /api/v1/public/receipts/validate/{receiptCode}/pdf

- Atualizar ReceiptService para gerar pdfUrl público:
  https://secretariapay-api.paixaoangola.com/api/v1/public/receipts/{receiptCode}/pdf

Arquivos alterados:
1. src/main/java/com/secretariapay/api/controller/publicapi/PublicReceiptController.java
2. src/main/java/com/secretariapay/api/service/financial/ReceiptService.java

Como aplicar:
1. Extrair este ZIP na raiz:
   C:\Users\dalaw\secretariapay-api

2. Compilar:
   cd C:\Users\dalaw\secretariapay-api
   .\mvnw.cmd clean package -DskipTests

3. Commit e push:
   git status
   git add .
   git commit -m "fix: expose public receipt PDF download"
   git push origin main

4. Produção:
   cd /opt/secretariapay-api
   git pull origin main
   docker compose up -d --build
   docker compose logs -f --tail=200 api

Teste:
curl -I https://secretariapay-api.paixaoangola.com/api/v1/public/receipts/RCT1783070751346/pdf

Esperado:
HTTP/1.1 200
Content-Type: application/pdf
