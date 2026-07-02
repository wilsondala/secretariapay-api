# Cleanup Fase 1 — Checklist

## Objetivo

Fase 1 é apenas inventário, documentação e marcação de legado. Não remover código crítico ainda.

## Checklist local

```powershell
cd C:\Users\dalaw\secretariapay-api

Select-String -Path .\src\main\java\com\secretariapay\api\**\*.java,.\src\main\resources\**\* `
  -Pattern "vairapido|VaiRápido|VaiRapido|api-vairapido|Comprar passagem|bilhete-vairapido|passagem|ticket|trip|passenger|transport|boarding|pix" `
  -CaseSensitive:$false
```

## Primeira limpeza segura

1. Corrigir `HealthController` para SecretáriaPay.
2. Corrigir CORS do `SecurityConfig`.
3. Substituir prefixos antigos `vairapido.whatsapp.*` por `secretariapay.whatsapp.*`, mantendo fallback temporário quando necessário.
4. Criar páginas legais do SecretáriaPay.
5. Marcar serviços antigos de passagem como `@Deprecated`, sem apagar ainda.

## Não fazer nesta fase

- Não apagar entidades antigas de `Ticket`, `Trip`, `Passenger`, `Booking`, `TravelRoute`, `TransportCompany`.
- Não apagar migrations antigas.
- Não remover controllers antigos sem antes confirmar dependências.
- Não mexer nas tabelas de produção.

## Testes mínimos após cada alteração

```bash
curl http://127.0.0.1:8080/actuator/health

curl -X GET https://secretariapay-api.paixaoangola.com/api/v1/public/branding/secretariapay

curl -X GET https://secretariapay-api.paixaoangola.com/api/v1/public/branding/institutions/imetro

curl -X GET https://secretariapay-api.paixaoangola.com/api/v1/secretariapay/whatsapp/diagnostics   -H "Authorization: Bearer $TOKEN"
```

## Critério de conclusão da fase 1

- Inventário registrado.
- Build local OK.
- Produção OK.
- Nenhum endpoint principal do SecretáriaPay quebrado.
