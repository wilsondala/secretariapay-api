# SecretáriaPay Académico — MVP Backend Status

Projeto: **SecretáriaPay Académico**  
Executor: **TRIA Company**  
Cliente piloto: **Instituto Superior Politécnico Metropolitano de Angola (IMETRO)**  
Ambiente de produção: `https://secretariapay-api.paixaoangola.com`

## 1. Visão do produto

O SecretáriaPay Académico é uma plataforma institucional para automação de propinas, cobranças, comprovativos, recibos digitais, regularização financeira/académica e atendimento via WhatsApp para instituições de ensino superior em Angola.

O produto não deve ser tratado apenas como robô de WhatsApp. Ele é uma plataforma académica-financeira com integração ao WhatsApp como canal principal de atendimento, cobrança e comunicação.

## 2. Estado geral

Status atual do backend MVP: **funcional em produção**.

Itens validados:

- API Spring Boot em produção.
- PostgreSQL em produção.
- Docker Compose funcional.
- NGINX + SSL ativo.
- Subdomínio configurado.
- Health check funcionando.
- Autenticação JWT funcionando.
- Fluxo académico-financeiro validado com dados reais/simulados do IMETRO.
- Recibo PDF institucional validado.
- QR Code e validação pública de recibo funcionando.
- Mensagens WhatsApp em preview funcionando.
- Histórico de mensagens funcionando.
- Fila de envio funcionando em modo mock seguro.
- Diagnóstico WhatsApp funcionando.
- Branding público do sistema e da instituição funcionando.

## 3. Infraestrutura validada

Produção:

```text
Servidor: VPS HostGator/Ubuntu
IP: 143.95.218.153
Porta SSH: 22022
API: https://secretariapay-api.paixaoangola.com
Banco: PostgreSQL em container Docker
Aplicação: Spring Boot Java 21 em container Docker
Proxy: NGINX
SSL: Let's Encrypt / Certbot
```

Comandos principais usados em produção:

```bash
cd /opt/secretariapay-api
docker compose up -d --build
curl http://127.0.0.1:8080/actuator/health
```

## 4. Módulos concluídos

### 4.1 Autenticação e segurança

- Cadastro/login de usuário administrador.
- JWT Bearer Token.
- Rotas públicas separadas de rotas autenticadas.
- Liberação pública para:
  - `/actuator/**`
  - `/api/v1/public/**`
  - `/branding/**`

### 4.2 Instituições

- Cadastro de instituição.
- Configuração institucional.
- Suporte ao modelo multiuniversidade.
- IMETRO configurado como cliente piloto.
- Configuração pública por slug.

Dados reais do IMETRO usados:

```text
Nome: Instituto Superior Politécnico Metropolitano de Angola
Nome legal: Instituto Superior Politécnico Metropolitano de Angola (IMETRO)
Slogan: A Marca da Educação
Endereço: Avenida 21 de Janeiro, Travessa da Talatona S/N, bairro Morro Bento, município de Belas, Luanda, Angola
País: AO
Moeda: AOA
Timezone: Africa/Luanda
```

### 4.3 Cursos e turmas

Cursos iniciais cadastrados:

- Electrónica e Telecomunicações
- Ciências da Computação
- Planeamento Regional e Urbano
- Arquitectura
- Engenharia Civil
- Geologia e Minas
- Jornalismo
- Cinema e TV
- Direito
- Economia
- Gestão de Recursos Humanos
- Gestão Pública
- Administração de Empresas

Turma validada:

```text
Curso: Engenharia Informática
Turma: EI - 1º Ano - Noite
Ano académico: 2026
Turno: NIGHT
```

### 4.4 Estudantes

Estudante de teste validado:

```text
Nome: João António Manuel
Nº estudante: IMETRO-2026-0001
WhatsApp: +244 923 000 001
Status: ACTIVE
Bloqueio financeiro: false
```

### 4.5 Cobranças

Cobrança validada:

```text
Descrição: Propina de Julho 2026
Referência: 2026-07
Valor: 45.000,00 AOA
Status inicial: PENDING
```

Dashboard financeiro validado:

```text
totalStudents: 1
blockedStudents: 0
pendingCharges: 1
paidCharges: 0
pendingPaymentProofs: 1
expectedRevenue: 45000.00
pendingRevenue: 45000.00
currency: AOA
```

### 4.6 Comprovativos

Fluxo validado:

- Submissão de comprovativo.
- Consulta por cobrança.
- Aprovação pela tesouraria.
- Atualização de status do comprovativo.

Status testados:

```text
PENDING_REVIEW
APPROVED
```

### 4.7 Recibos digitais

Fluxo validado:

- Emissão de recibo a partir de cobrança aprovada.
- Geração de PDF.
- QR Code no PDF.
- Validação pública por código.
- Branding institucional do IMETRO no PDF.

Endpoint de PDF validado:

```text
GET /api/v1/receipts/{receiptId}/pdf
```

Endpoint público de validação validado:

```text
GET /api/v1/public/receipts/validate/{receiptCode}
```

### 4.8 Mensagens WhatsApp

Tipos validados:

- BEFORE_DUE
- OVERDUE
- PROOF_RECEIVED
- PROOF_APPROVED
- RECEIPT_ISSUED
- REGULARIZED

Mensagens em português de Angola (`pt-AO`) e com valores em Kz.

### 4.9 Histórico de mensagens

Fluxo validado:

```text
GENERATED
SENT
READ
FAILED
```

Campos importantes:

- institutionId
- studentId
- chargeId
- paymentProofId
- receiptId
- type
- channel
- recipientPhone
- message
- status
- providerMessageId
- failureReason
- sentAt
- readAt

### 4.10 Fila de envio

Fluxo validado:

```text
GENERATED → QUEUED → SENT
```

Modo atual:

```text
MOCK_SAFE
```

Ou seja: nenhuma mensagem real é enviada pela Meta enquanto o WhatsApp estiver desativado.

### 4.11 Diagnóstico WhatsApp

Endpoint validado:

```text
GET /api/v1/secretariapay/whatsapp/diagnostics
```

Retorno atual esperado:

```json
{
  "enabled": false,
  "phoneNumberIdConfigured": false,
  "accessTokenConfigured": false,
  "graphApiVersion": "v20.0",
  "graphApiBaseUrl": "https://graph.facebook.com",
  "mode": "MOCK_SAFE"
}
```

### 4.12 Branding público

Logo pública validada:

```text
https://secretariapay-api.paixaoangola.com/branding/secretariapay-logo.png
```

Endpoint público do sistema validado:

```text
GET /api/v1/public/branding/secretariapay
```

Endpoint público institucional validado:

```text
GET /api/v1/public/branding/institutions/imetro
```

## 5. Próximo momento técnico

Antes de iniciar o frontend administrativo, recomenda-se a fase de limpeza/consolidação técnica do backend:

1. Documentar status atual.
2. Mapear código herdado do VaiRápido.
3. Remover ou isolar classes de transporte/passagens.
4. Padronizar nomes e pacotes.
5. Revisar README e `.env.example`.
6. Rodar build completo.
7. Testar endpoints principais em produção.
8. Só depois iniciar frontend/painel administrativo.
