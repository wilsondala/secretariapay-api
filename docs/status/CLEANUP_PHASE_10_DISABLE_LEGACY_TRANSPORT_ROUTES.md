# SecretáriaPay Académico — Cleanup Fase 10

## Objetivo

Desativar publicamente as rotas legadas de transporte/passagens herdadas do projeto anterior, sem apagar entidades, services, repositories, migrations ou histórico técnico.

## Estratégia

A Fase 10 adiciona um bloqueio global via `HandlerInterceptor`, controlado por propriedade:

```properties
secretariapay.legacy.transport-api.enabled=false
```

A API legada fica desligada por padrão. Caso seja necessário reativar temporariamente em ambiente controlado, basta definir:

```properties
secretariapay.legacy.transport-api.enabled=true
```

## Rotas bloqueadas

- `/api/v1/bookings/**`
- `/api/v1/passengers/**`
- `/api/v1/payments/**`
- `/api/v1/public/tickets/**`
- `/api/v1/tickets/**`
- `/api/v1/ticket-audit-logs/**`
- `/api/v1/transport-companies/**`
- `/api/v1/routes/**`
- `/api/v1/trips/**`
- `/api/v1/reports/trips/**`

## Rotas preservadas

A Fase 10 não bloqueia os módulos válidos do SecretáriaPay Académico:

- instituições, cursos, turmas e estudantes;
- cobranças académicas;
- comprovativos de pagamento;
- recibos digitais;
- validação pública de recibos;
- dashboard financeiro académico;
- branding público;
- páginas legais;
- message-history e message-dispatch.

## Resposta esperada para rota legada

```json
{
  "status": 410,
  "error": "LEGACY_TRANSPORT_API_DISABLED",
  "message": "API legada de transporte/passagens desativada. Use os módulos SecretáriaPay académico, financeiro, recibos, comprovativos e message-dispatch.",
  "path": "/api/v1/tickets",
  "timestamp": "2026-07-02T00:00:00Z"
}
```

## Regra de segurança

Esta fase não remove código. Apenas impede uso acidental das rotas antigas em produção.
