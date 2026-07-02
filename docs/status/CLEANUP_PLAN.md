# SecretáriaPay Académico — Plano de Limpeza Técnica

## 1. Objetivo

Limpar e consolidar o backend do SecretáriaPay Académico sem quebrar os módulos já validados em produção.

A limpeza deve remover ou isolar código herdado do projeto VaiRápido e padronizar a estrutura do projeto para o domínio académico-financeiro.

## 2. Regra principal

Nenhuma remoção deve ser feita sem este ciclo:

```text
alterar → build local → commit → deploy → health check → teste de endpoints críticos
```

## 3. O que não pode quebrar

Endpoints críticos que precisam continuar funcionando:

```text
GET  /actuator/health
POST /api/v1/auth/login
GET  /api/v1/institutions
GET  /api/v1/institution-settings/slug/imetro
GET  /api/v1/courses?institutionId=...
GET  /api/v1/academic-classes?courseId=...
GET  /api/v1/students/...
POST /api/v1/charges
GET  /api/v1/charges?studentId=...
POST /api/v1/payment-proofs
PATCH /api/v1/payment-proofs/{id}/approve
POST /api/v1/receipts/charge/{chargeId}/issue
GET  /api/v1/receipts/{receiptId}/pdf
GET  /api/v1/public/receipts/validate/{receiptCode}
GET  /api/v1/secretariapay/messages/...
POST /api/v1/secretariapay/message-history/...
PATCH /api/v1/secretariapay/message-dispatch/{messageId}/queue
POST /api/v1/secretariapay/message-dispatch/process-queue
GET  /api/v1/secretariapay/whatsapp/diagnostics
GET  /api/v1/public/branding/secretariapay
GET  /api/v1/public/branding/institutions/imetro
GET  /branding/secretariapay-logo.png
```

## 4. Código suspeito herdado do VaiRápido

Mapear classes/pacotes com estes nomes:

```text
Ticket
TicketAuditLog
Trip
TravelRoute
Passenger
TransportCompany
Booking
Boarding
Payment
Pix
Route
Seat
QRCode de bilhete
PDF de bilhete
```

Comando local para procurar:

```powershell
cd C:\Users\dalaw\secretariapay-api

Select-String -Path .\src\main\java\com\secretariapay\api\**\*.java `
  -Pattern "VaiRapido|Vairapido|passagem|passagens|ticket|trip|travel|route|passenger|transport|boarding|seat|pix" `
  -CaseSensitive:$false
```

## 5. Estratégia de limpeza

### Etapa 1 — Classificar

Criar uma lista com três grupos:

```text
A. Código usado pelo SecretáriaPay
B. Código legado que ainda compila mas não é usado
C. Código legado que deve ser removido
```

### Etapa 2 — Isolar antes de remover

Antes de apagar classes grandes, mover mentalmente para um grupo de legado:

```text
legacy.transport
legacy.ticket
legacy.booking
```

Se não houver dependência, remover depois.

### Etapa 3 — Remover endpoints antigos

Remover controllers que exponham rotas antigas de transporte/passagem se não forem necessários:

```text
TransportCompanyController
TravelRouteController
TripController
PassengerController
TicketController legado
```

Atenção: se algum serviço de recibo ainda usa `TicketPdfService`, primeiro criar ou consolidar `ReceiptPdfService`.

### Etapa 4 — Limpar entidades e migrations

Não apagar migrations antigas já aplicadas em produção.

Regra:

```text
Nunca editar migrations antigas aplicadas.
Criar novas migrations corretivas quando necessário.
```

Se existirem tabelas antigas no banco, decidir depois se serão removidas por migration própria:

```sql
DROP TABLE IF EXISTS ...
```

Mas apenas quando o backend não depender mais delas.

### Etapa 5 — Padronizar nomes

Padronizar:

```text
secretariapay
SecretariaPay
SecretáriaPay
SecretariaPayApiApplication
```

Evitar:

```text
VaiRapido
Vairapido
Compra rápida de passagens
Bilhete de viagem
```

### Etapa 6 — Revisar documentação

Atualizar:

```text
README.md
.env.example
.env.production.example
docs/status/MVP_STATUS.md
docs/status/PRODUCTION_ENDPOINTS.md
```

## 6. Checklist por commit

Cada commit de limpeza deve ser pequeno.

Exemplo de sequência:

```text
commit 1: docs: registrar status atual do mvp
commit 2: chore: mapear codigo legado de transporte
commit 3: refactor: remover controllers legados de transporte
commit 4: refactor: remover services legados nao usados
commit 5: chore: atualizar readme e exemplos de ambiente
commit 6: test: validar endpoints principais em producao
```

## 7. Teste mínimo depois de cada limpeza

Na VPS:

```bash
cd /opt/secretariapay-api
git pull
docker compose up -d --build
curl http://127.0.0.1:8080/actuator/health
```

Testes públicos:

```bash
curl -I https://secretariapay-api.paixaoangola.com/branding/secretariapay-logo.png
curl -X GET https://secretariapay-api.paixaoangola.com/api/v1/public/branding/secretariapay
curl -X GET https://secretariapay-api.paixaoangola.com/api/v1/public/branding/institutions/imetro
curl -X GET https://secretariapay-api.paixaoangola.com/api/v1/public/receipts/validate/RCT1782913501413
```

Testes autenticados:

```bash
curl -X GET https://secretariapay-api.paixaoangola.com/api/v1/secretariapay/whatsapp/diagnostics   -H "Authorization: Bearer $TOKEN"
```

## 8. Quando parar a limpeza

Parar a limpeza quando:

```text
Build local estiver OK
Produção estiver OK
Endpoints principais estiverem OK
Código legado estiver removido ou claramente isolado
README estiver coerente com SecretáriaPay
.env.example estiver coerente com Angola/IMETRO
```

Depois disso, iniciar frontend/painel administrativo.
