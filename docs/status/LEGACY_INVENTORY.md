# Inventário de Legado — SecretáriaPay API

## Objetivo

Registrar, antes de qualquer remoção, os pontos herdados do projeto VaiRápido que ainda aparecem no backend SecretáriaPay.

Esta etapa não remove código. Ela prepara uma limpeza controlada para evitar quebra de build, endpoints, migrations ou produção.

## Legado identificado

### Configurações

- `BusinessCountryProperties.java` ainda usa prefixo `vairapido.business`.
- `WhatsappCloudApiProperties.java` ainda usa prefixo `vairapido.whatsapp`.
- `WhatsAppCloudProperties.java` ainda lê `vairapido.whatsapp.enabled`, `access-token`, `phone-number-id`, `graph-api-version` e `graph-api-base-url`.
- `SecurityConfig.java` ainda possui origins do VaiRápido.

### Controllers e páginas públicas

- `HealthController.java` ainda retorna projeto/serviço VaiRápido.
- `PublicLegalController.java` ainda possui política, termos e exclusão de dados do VaiRápido.
- `PublicTicketPdfController.java` ainda usa nome `bilhete-vairapido.pdf`.

### Serviços antigos

- `PaymentService.java` ainda contém EMV/QR Pix com VAIRAPIDO.
- `TicketPdfService.java`, `TicketService.java` e `PublicTicketValidationPageService.java` ainda tratam bilhete/passagem.
- `WhatsappCommandService.java` ainda contém fluxo grande de compra de passagem.
- `WhatsappFaqAnswerService.java` ainda contém FAQ do VaiRápido.
- `WhatsAppService.java` ainda monta mensagens de reserva/bilhete.
- `WhatsappWebhookService.java` ainda usa token `vairapido.whatsapp.verify-token`, envio de PDF de bilhete e nome `bilhete-vairapido`.

## Classificação

### Manter

- `academic`
- `financial`
- `branding`
- `secretariapay/message-history`
- `secretariapay/message-dispatch`
- `secretariapay/whatsapp/diagnostics`
- `auth/security`
- `dashboard`
- `public receipts`

### Isolar como legado

- `WhatsappCommandService`
- `WhatsappFaqAnswerService`
- `WhatsappSessionService`
- `WhatsappWebhookService`
- `WhatsAppService`
- serviços de tickets, viagens, passageiros e rotas

### Remover em fase posterior

- endpoints de passagem
- bilhete digital antigo
- Pix simulado antigo
- páginas legais antigas do VaiRápido
- URLs antigas `api-vairapido.triacompany.com`
- CORS antigo do VaiRápido

## Regra de segurança

Cada bloco de limpeza deve seguir:

```text
alterar
build local
commit
deploy
health
teste dos endpoints principais
```
