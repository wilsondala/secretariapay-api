Fase 12.16B — Enviar recibo digital como documento PDF pelo WhatsApp

Problema:
- O aluno recebe o link do PDF, mas queremos também enviar o recibo como documento/anexo no WhatsApp, igual ao fluxo do VaiRápido.
- O cliente atual do WhatsApp só enviava texto.

Correção:
1. WhatsAppCloudApiClient.java
   - Mantém sendText(...)
   - Adiciona sendDocumentByLink(...)
   - Usa payload do WhatsApp Cloud API com type=document.

2. SecretariaPayMessageDispatchService.java
   - Se a mensagem for RECEIPT_ISSUED, envia documento PDF em vez de só texto.
   - Usa o link público: /api/v1/public/receipts/{receiptCode}/pdf
   - Mantém texto para os demais tipos.

Como aplicar:
cd C:\Users\dalaw\secretariapay-api

Expand-Archive -Path "$env:USERPROFILE\Downloads\secretariapay-fase-12-16b-whatsapp-receipt-document.zip" -DestinationPath . -Force

if (Test-Path .\mvnw.cmd) {
  .\mvnw.cmd clean package -DskipTests
} else {
  mvn clean package -DskipTests
}

git status
git add .
git commit -m "feat: send receipt PDF document via WhatsApp"
git push origin main

Produção:
cd /opt/secretariapay-api
git pull origin main
docker compose up -d --build
docker compose logs -f --tail=200 api

Teste:
1. Gerar nova mensagem RECEIPT_ISSUED.
2. Fazer dispatch.
3. No WhatsApp deve chegar um documento PDF chamado recibo-RCT....pdf.
