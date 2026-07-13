# SecretáriaPay Académico API

Backend da plataforma **SecretáriaPay Académico**, uma solução institucional da TRIA Company para automação de propinas, cobranças, comprovativos, recibos digitais e atendimento académico via WhatsApp.

## Objetivo do produto

O SecretáriaPay Académico foi criado para apoiar instituições de ensino superior na gestão automatizada do ciclo financeiro académico:

- cadastro de instituições, cursos, turmas e estudantes;
- geração de cobranças de propinas, matrículas, inscrições e taxas;
- recepção e validação de comprovativos;
- emissão de recibos digitais com QR Code e validação pública;
- mensagens de cobrança e regularização via WhatsApp;
- histórico de mensagens e fila de envio;
- dashboards financeiros;
- preparação para bloqueio e desbloqueio académico conforme regras da instituição.

## Cliente piloto

Cliente piloto atual:

**Instituto Superior Politécnico Metropolitano de Angola (IMETRO)**  
Luanda, Angola

## Stack principal

- Java 21
- Spring Boot 3.x
- PostgreSQL
- Flyway
- Docker / Docker Compose
- NGINX + SSL
- JWT / Spring Security
- WhatsApp Cloud API em modo seguro/mock até ativação explícita

## Produção

API em produção:

```text
https://secretariapay-api.paixaoangola.com
```

Health:

```text
GET /actuator/health
GET /api/v1/health
```

## Endpoints públicos principais

```text
GET /api/v1/public/branding/secretariapay
GET /api/v1/public/branding/institutions/imetro
GET /branding/secretariapay-logo.png
GET /api/v1/public/legal/privacy-policy
GET /api/v1/public/legal/terms-of-service
GET /api/v1/public/legal/data-deletion
GET /api/v1/public/receipts/validate/{receiptCode}
```

## Módulos validados no MVP

- Autenticação e usuário administrador
- Instituições e configurações institucionais
- Cursos e turmas
- Estudantes
- Cobranças financeiras
- Comprovativos de pagamento
- Aprovação de comprovativos
- Recibos digitais
- PDF de recibo com QR Code
- Validação pública de recibo
- Dashboard financeiro básico
- Templates de mensagens SecretáriaPay
- Histórico de mensagens
- Fila de envio WhatsApp
- Envio mockado seguro
- Diagnóstico WhatsApp Cloud API
- Branding público do sistema
- Branding público institucional do IMETRO
- Páginas legais públicas do SecretáriaPay

## Consistência financeira das propinas

A propina é tratada como um único lançamento financeiro por estudante e período.

Regras aplicadas:

- a geração mensal não cria nova propina quando já existe cobrança aberta ou paga no mesmo mês;
- a confirmação de pagamento liquida a cobrança que originou a guia;
- a confirmação manual utiliza a mesma lógica canónica de liquidação;
- cobranças pagas, recibos e comprovativos nunca são eliminados;
- duplicatas abertas antigas são canceladas logicamente por migration;
- o banco impede mais de uma propina aberta por estudante e mês.

## Modo WhatsApp

Por segurança, o envio real pela Meta permanece desativado até configuração explícita.

Variáveis esperadas para envio real:

```text
SECRETARIAPAY_WHATSAPP_ENABLED=true
SECRETARIAPAY_WHATSAPP_PHONE_NUMBER_ID=...
SECRETARIAPAY_WHATSAPP_ACCESS_TOKEN=...
SECRETARIAPAY_WHATSAPP_GRAPH_API_VERSION=v20.0
SECRETARIAPAY_WHATSAPP_GRAPH_API_BASE_URL=https://graph.facebook.com
```

Enquanto `SECRETARIAPAY_WHATSAPP_ENABLED=false`, a fila processa mensagens em modo seguro/mock.

## Legado herdado

Este projeto nasceu a partir de uma base técnica anterior do VaiRápido. Durante a consolidação do SecretáriaPay, alguns módulos legados de transporte ainda podem existir no código, mas estão em processo de isolamento e remoção controlada.

Não remover diretamente entidades, migrations ou tabelas antigas sem validação de dependência e build.

Fluxo obrigatório para limpeza:

```text
mapear → isolar → build local → testar produção → remover em blocos pequenos
```

## Comandos principais

Build local:

```powershell
& "C:\tools\apache-maven-3.9.16\bin\mvn.cmd" clean package -DskipTests
```

Deploy na VPS:

```bash
cd /opt/secretariapay-api
git pull
docker compose up -d --build
curl http://127.0.0.1:8080/actuator/health
```

## TRIA Company

Produto desenvolvido pela **TRIA Company** como plataforma institucional de gestão automatizada de propinas, cobranças e regularização académica via WhatsApp.
