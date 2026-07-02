# Cleanup Phase 5 - README e decisão sobre legado WhatsApp antigo

## Objetivo

Atualizar a identidade pública do repositório e registrar a decisão técnica sobre o legado de transporte herdado da base VaiRápido.

Esta fase é segura porque não remove entidades, migrations, controllers ou serviços operacionais.

## Alterações

- `README.md` passa a descrever o projeto como SecretáriaPay Académico.
- Remove descrição antiga de compra de passagens, Pix e bilhete digital do README.
- Registra os módulos atuais validados em produção.
- Registra a regra de limpeza controlada do legado.

## Resultado da varredura anterior

A varredura da Fase 4 mostrou que as referências públicas restantes estão concentradas principalmente em:

- `WhatsappCommandService.java`
- `WhatsappFaqAnswerService.java`
- documentação de limpeza
- `README.md`

O README pode ser corrigido agora.

Os serviços `WhatsappCommandService` e `WhatsappFaqAnswerService` devem ser tratados em uma fase separada, porque são grandes e podem ter dependências com o webhook antigo, sessão WhatsApp, reservas, tickets e classes herdadas.

## Decisão técnica

Não apagar ainda:

- `WhatsappCommandService.java`
- `WhatsappFaqAnswerService.java`
- `WhatsappWebhookService.java`
- entidades antigas de ticket/trip/passenger/transport
- migrations antigas

Próxima fase recomendada:

1. Criar novos serviços de atendimento académico do SecretáriaPay, se necessário.
2. Marcar os serviços antigos como legado/deprecated.
3. Remover rotas públicas antigas somente após confirmar que nenhum endpoint do SecretáriaPay depende delas.
4. Só depois excluir entidades e migrations antigas em bloco final.

## Testes obrigatórios após esta fase

```bash
curl http://127.0.0.1:8080/actuator/health
curl https://secretariapay-api.paixaoangola.com/api/v1/health
curl https://secretariapay-api.paixaoangola.com/api/v1/public/branding/secretariapay
curl https://secretariapay-api.paixaoangola.com/api/v1/public/branding/institutions/imetro
```
