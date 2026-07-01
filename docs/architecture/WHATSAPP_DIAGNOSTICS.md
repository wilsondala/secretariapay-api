# WhatsApp Diagnostics

Este módulo adiciona um endpoint de diagnóstico seguro para verificar se o WhatsApp Cloud API está configurado.

Endpoint:

```http
GET /api/v1/secretariapay/whatsapp/diagnostics
```

O endpoint não expõe token, não expõe phone-number-id real e não envia mensagens.

Campos retornados:

- `enabled`: informa se o envio real está habilitado.
- `phoneNumberIdConfigured`: informa apenas se o phone-number-id foi configurado.
- `accessTokenConfigured`: informa apenas se o access-token foi configurado.
- `graphApiVersion`: versão configurada da Graph API.
- `graphApiBaseUrl`: URL base configurada da Graph API.
- `mode`: modo operacional atual.
- `safetyMessage`: orientação de segurança.

Modos:

- `MOCK_SAFE`: envio real desativado; mensagens continuam em modo simulado.
- `REAL_SEND_BLOCKED_BY_MISSING_CONFIG`: envio real ativado, mas credenciais incompletas.
- `REAL_SEND_READY`: envio real ativado e credenciais básicas configuradas.

Variáveis esperadas:

```env
SECRETARIAPAY_WHATSAPP_ENABLED=false
SECRETARIAPAY_WHATSAPP_PHONE_NUMBER_ID=
SECRETARIAPAY_WHATSAPP_ACCESS_TOKEN=
SECRETARIAPAY_WHATSAPP_GRAPH_API_VERSION=v20.0
SECRETARIAPAY_WHATSAPP_GRAPH_API_BASE_URL=https://graph.facebook.com
```

Observação: antes de ativar `SECRETARIAPAY_WHATSAPP_ENABLED=true`, validar número oficial, token, permissões da Meta, limites e destinatários de teste.
