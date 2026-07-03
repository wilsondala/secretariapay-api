Fase 12.19 — Confirmação de recebimento da guia pelo aluno

Objetivo:
- Depois que a guia de pagamento é enviada por WhatsApp, o aluno pode responder: "recebi", "recebido", "já recebi", "guia recebida" etc.
- O SecretáriaPay identifica a última guia PAYMENT_GUIDE enviada para aquele número.
- O sistema associa a resposta à cobrança correta e responde aguardando o pagamento/comprovativo.

Arquivos alterados:
1. SecretariaPayMessageRepository.java
2. SecretariaPayWhatsappAcademicSupportService.java

Como aplicar:
cd C:\Users\dalaw\secretariapay-api

Expand-Archive -Path "$env:USERPROFILE\Downloads\secretariapay-fase-12-19-payment-guide-ack.zip" -DestinationPath . -Force

if (Test-Path .\mvnw.cmd) {
  .\mvnw.cmd clean package -DskipTests
} else {
  mvn clean package -DskipTests
}

git status
git add .
git commit -m "feat: acknowledge payment guide receipt via WhatsApp"
git push origin main

Produção:
cd /opt/secretariapay-api
git pull origin main
docker compose up -d --build
docker compose logs -f --tail=200 api

Teste no WhatsApp:
Aluno responde: recebi

Resposta esperada:
Perfeito. Confirmamos o recebimento da guia de pagamento.
Cobrança: CHG...
Agora aguardamos o pagamento.
Após pagar, envie o comprovativo por aqui em imagem ou PDF para a tesouraria validar.
O recibo digital será emitido apenas após a confirmação do pagamento.
