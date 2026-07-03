Fase 12.14 — Gravar comprovativo recebido pelo WhatsApp no banco

Arquivos incluídos:
1. src/main/java/com/secretariapay/api/service/whatsapp/SecretariaPayWhatsappWebhookService.java
2. src/main/java/com/secretariapay/api/service/whatsapp/SecretariaPayWhatsappAcademicSupportService.java
3. src/main/java/com/secretariapay/api/service/whatsapp/SecretariaPayWhatsappConversationContextService.java

Objetivo:
- Capturar mediaId, fileName e mimeType de imagem/documento recebido no webhook.
- Usar a última cobrança salva no contexto da conversa.
- Criar registro real em payment_proofs com status PENDING_REVIEW.
- Manter a resposta automática informando que o comprovativo foi registado e ficou pendente de validação.

Como aplicar no projeto local:
1. Extraia o ZIP na raiz do projeto:
   C:\Users\dalaw\secretariapay-api

2. Compile:
   cd C:\Users\dalaw\secretariapay-api
   .\mvnw.cmd clean package -DskipTests

3. Commit e push:
   git status
   git add .
   git commit -m "feat: register WhatsApp payment proof in database"
   git push origin main

4. Produção:
   cd /opt/secretariapay-api
   git pull origin main
   docker compose up -d --build
   docker compose logs -f --tail=200 api

Teste esperado:
- Enviar CHG1783012061065
- Responder 1
- Enviar imagem/PDF
- O robô deve responder: Comprovativo recebido e registado para a cobrança CHG1783012061065.
- No banco/tela de comprovativos deve existir registro PENDING_REVIEW.
