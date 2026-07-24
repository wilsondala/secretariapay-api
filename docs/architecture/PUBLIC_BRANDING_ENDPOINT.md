# Endpoint público de branding do SecretáriaPay

Este módulo cria um endpoint público para o frontend consultar a identidade visual oficial do sistema sem fixar essas informações no React.

## Endpoint

GET /api/v1/public/branding/secretariapay

## Retorno esperado

```json
{
  "name": "SecretáriaPay",
  "product": "SecretáriaPay Académico",
  "description": "Plataforma institucional de automação de propinas, cobranças, comprovativos, recibos digitais e atendimento académico via WhatsApp.",
  "logoUrl": "https://secretariapay-api.paixaoangola.com/branding/secretariapay-logo.png",
  "primaryColor": "#0B3B82",
  "secondaryColor": "#16A34A",
  "accentColor": "#D4AF37",
  "company": "SecretáriaPay Académico",
  "platform": "Academic Financial Automation",
  "countryFocus": "Angola"
}
```

## Observação

A rota está em /api/v1/public/**, portanto já segue a regra pública configurada no SecurityConfig.
