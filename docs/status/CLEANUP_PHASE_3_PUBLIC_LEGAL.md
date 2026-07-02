# Cleanup Phase 3 — Public Legal Pages

## Objetivo

Substituir páginas legais antigas herdadas do VaiRápido por páginas públicas alinhadas ao SecretáriaPay Académico.

## Arquivo alterado

- `src/main/java/com/secretariapay/api/controller/PublicLegalController.java`

## Endpoints mantidos

- `GET /api/v1/public/legal/privacy-policy`
- `GET /api/v1/public/legal/terms-of-service`
- `GET /api/v1/public/legal/data-deletion`

## Alterações principais

- Troca de identidade visual textual de VaiRápido para SecretáriaPay.
- Remoção de textos sobre passagens, reservas, bilhetes de transporte, rotas e Pix.
- Inclusão de contexto académico-financeiro: propinas, cobranças, comprovativos, recibos digitais, WhatsApp institucional, auditoria, tesouraria e regras da instituição.
- Idioma ajustado para `pt-AO`.
- Cores alinhadas ao branding público do SecretáriaPay: azul institucional, verde e dourado.

## Testes recomendados

```bash
curl -I https://secretariapay-api.paixaoangola.com/api/v1/public/legal/privacy-policy
curl -I https://secretariapay-api.paixaoangola.com/api/v1/public/legal/terms-of-service
curl -I https://secretariapay-api.paixaoangola.com/api/v1/public/legal/data-deletion
```

Abrir no navegador:

- https://secretariapay-api.paixaoangola.com/api/v1/public/legal/privacy-policy
- https://secretariapay-api.paixaoangola.com/api/v1/public/legal/terms-of-service
- https://secretariapay-api.paixaoangola.com/api/v1/public/legal/data-deletion

## Segurança

Esta fase não altera banco de dados, autenticação, endpoints financeiros, recibos, WhatsApp dispatch, fila ou integrações. É uma alteração de conteúdo público.
