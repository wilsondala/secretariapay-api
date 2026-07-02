# Fase 7 - Varredura de dependências do WhatsApp legado

## Objetivo

Mapear quais classes ainda dependem dos serviços antigos de WhatsApp herdados do fluxo de passagens:

- `WhatsappCommandService`
- `WhatsappFaqAnswerService`

Esses serviços foram marcados como `@Deprecated` na Fase 6, mas ainda não devem ser removidos sem rastrear dependências.

## Motivo

O SecretáriaPay Académico deve priorizar o fluxo institucional de propinas, cobranças, comprovativos, recibos digitais, bloqueio/desbloqueio académico e atendimento via WhatsApp.

O fluxo antigo de passagens, bilhetes, viagens, passageiros, rotas, embarque e Pix não faz parte do núcleo atual do produto.

## Script criado

Arquivo:

```text
scripts/scan-whatsapp-legacy-dependencies.ps1
```

Comando:

```powershell
.\scripts\scan-whatsapp-legacy-dependencies.ps1
```

## Critério de decisão

Após a varredura, cada dependência deve ser classificada como:

1. **Remover agora**: referências públicas antigas que não afetam o backend académico-financeiro.
2. **Isolar como legado**: webhook/comandos antigos ainda conectados ao build.
3. **Manter temporariamente**: entidades, migrations ou serviços que ainda são dependência estrutural.
4. **Migrar para novo módulo**: se alguma dependência útil puder ser reaproveitada no fluxo SecretáriaPay.

## Próximo passo

Com o resultado do script, preparar a Fase 8:

- desligar endpoints públicos antigos de WhatsApp de passagem, ou
- mover serviços antigos para pacote `legacy`, ou
- remover dependências simples se o build permitir.
