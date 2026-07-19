# Validação controlada — pedidos de serviços académicos

## Objetivo

Validar, fora de produção, o fluxo institucional completo entre DCR, Secretaria e Direção:

`SOLICITADO → AGUARDANDO_PAGAMENTO → PAGO → DOCUMENTO_GERADO → PRONTO_PARA_IMPRESSAO → IMPRESSO → AGUARDANDO_ASSINATURA → ASSINADO → PRONTO_PARA_LEVANTAMENTO → WHATSAPP_ENVIADO → ENTREGUE`

Nenhum merge ou deploy deve ocorrer antes da aprovação desta validação.

## Pré-condições

- API executada localmente ou em homologação;
- migration `V203607181000__create_academic_service_orders_workflow.sql` aplicada;
- estudante de teste existente, por padrão matrícula `202301404`;
- serviço ativo com preço configurado, por padrão `DECLARATION_WITHOUT_GRADES`;
- três utilizadores distintos:
  - DCR (`DCR_COORDENACAO` ou `DCR_OPERADOR`);
  - Secretaria (`SECRETARIA`);
  - Direção (`DIRECAO`);
- WhatsApp configurado em modo controlado/mock ou num número autorizado para homologação;
- nunca apontar o roteiro para `https://secretariapay-api.paixaoangola.com`.

## Variáveis PowerShell

```powershell
$env:SECRETARIAPAY_VALIDATION_DCR_EMAIL="dcr-homologacao@exemplo.com"
$env:SECRETARIAPAY_VALIDATION_SECRETARIA_EMAIL="secretaria-homologacao@exemplo.com"
$env:SECRETARIAPAY_VALIDATION_DIRECAO_EMAIL="direcao-homologacao@exemplo.com"
$env:SECRETARIAPAY_VALIDATION_PASSWORD="SENHA_DO_AMBIENTE_CONTROLADO"
```

## Executar o roteiro de API

```powershell
Set-Location C:\Users\dalaw\secretariapay-api

powershell -ExecutionPolicy Bypass -File .\scripts\validate-academic-service-orders-phase1.ps1 `
  -BaseUrl "http://localhost:8080" `
  -StudentNumber "202301404" `
  -ServiceCode "DECLARATION_WITHOUT_GRADES"
```

O roteiro valida:

1. criação do pedido pela DCR;
2. bloqueio da Secretaria na emissão da cobrança;
3. emissão da cobrança pela DCR;
4. bloqueio da geração documental antes do pagamento;
5. confirmação do pagamento;
6. geração e impressão pela Secretaria;
7. bloqueio da assinatura para a Secretaria;
8. assinatura pela Direção;
9. bloqueio da disponibilização física para a Direção;
10. disponibilização e WhatsApp pela Secretaria;
11. identificação de quem levantou;
12. estado final `ENTREGUE` e evidência JSON.

## Validar o banco PostgreSQL

No container PostgreSQL:

```bash
docker exec -i SecretariaPay-postgres \
  sh -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1' \
  < scripts/sql/validate-academic-service-orders.sql
```

A consulta falha quando encontra:

- migration não aplicada;
- índices obrigatórios ausentes;
- pedido em etapa pós-pagamento sem cobrança `PAID`;
- etapa documental sem documento associado;
- impressão, assinatura, WhatsApp ou entrega sem registos mínimos;
- entrega sem nome ou documento de identificação de quem levantou.

## Validação visual do painel

Executar com os três perfis, em desktop e mobile, nos modos claro e escuro.

### DCR

- visualiza `Novo pedido`;
- cria o pedido;
- emite a cobrança;
- não visualiza ações de impressão, assinatura, WhatsApp ou entrega.

### Secretaria

- não cria cobrança;
- recebe o pedido operacional somente após `PAGO`;
- gera o documento;
- coloca na fila de impressão;
- regista impressão;
- encaminha para assinatura;
- não consegue assinar;
- confirma localização física;
- envia WhatsApp;
- regista levantamento e entrega.

### Direção

- visualiza o pedido em `AGUARDANDO_ASSINATURA`;
- consegue assinar;
- não executa impressão, WhatsApp ou entrega.

### Arquivo

- documentos a partir de `ASSINADO` aparecem no arquivo;
- pedido `ENTREGUE` mostra responsável, data, nome e identificação de quem levantou;
- PDF associado permanece acessível para consulta autorizada.

## Critério de aprovação

A fase é aprovada somente quando:

- Backend Build estiver verde;
- Frontend Build estiver verde;
- roteiro PowerShell terminar com `VALIDAÇÃO CONTROLADA APROVADA`;
- SQL terminar sem inconsistências críticas;
- matriz visual DCR–Secretaria–Direção estiver validada;
- nenhuma ação tiver sido executada em produção.
