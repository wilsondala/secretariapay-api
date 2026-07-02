# SecretáriaPay - Limpeza fase 2: configurações e identidade da API

Esta fase remove referências globais herdadas do VaiRápido em pontos seguros de configuração e identidade pública da API.

## Arquivos ajustados

- `src/main/java/com/secretariapay/api/controller/HealthController.java`
- `src/main/java/com/secretariapay/api/config/SecurityConfig.java`
- `src/main/java/com/secretariapay/api/config/BusinessCountryProperties.java`
- `src/main/java/com/secretariapay/api/config/WhatsappCloudApiProperties.java`
- `src/main/java/com/secretariapay/api/config/WhatsAppCloudProperties.java`

## Mudanças realizadas

1. `/` agora identifica a API como **SecretáriaPay API**.
2. `/api/v1/health` agora retorna `service=secretariapay-api`.
3. CORS removeu domínios do VaiRápido e passou a aceitar domínios planejados do SecretáriaPay.
4. `BusinessCountryProperties` passou de `vairapido.business` para `secretariapay.business`.
5. Configurações WhatsApp Cloud passaram de `vairapido.whatsapp.*` para `secretariapay.whatsapp.*`.
6. Defaults de país/moeda foram alinhados para Angola: `AO` e `AOA`.

## O que ainda fica para próximas fases

Ainda existem módulos legados de bilhetes, viagens, passageiros e Pix. Eles não foram removidos nesta fase para evitar quebra em cascata.

Próximas fases sugeridas:

1. Reescrever ou remover páginas legais antigas do VaiRápido.
2. Isolar `WhatsappCommandService`, `WhatsappFaqAnswerService`, `WhatsappSessionService`, `WhatsappWebhookService` e `WhatsAppService`.
3. Remover ou arquivar controllers, services e entidades de transporte/passagens.
4. Limpar migrations antigas apenas após decisão sobre preservação histórica do banco.

## Checklist de validação

```bash
curl http://127.0.0.1:8080/actuator/health
curl https://secretariapay-api.paixaoangola.com/api/v1/health
curl https://secretariapay-api.paixaoangola.com/api/v1/public/branding/secretariapay
curl https://secretariapay-api.paixaoangola.com/api/v1/public/branding/institutions/imetro
curl -I https://secretariapay-api.paixaoangola.com/branding/secretariapay-logo.png
```
