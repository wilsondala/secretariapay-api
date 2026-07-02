# SecretáriaPay - Limpeza Fase 4: Varredura de rotas e textos legados

## Objetivo

Esta fase prepara a remoção controlada do legado herdado do VaiRápido sem apagar código crítico de forma precipitada.

A Fase 4 não remove entidades, migrations ou tabelas. Ela mapeia os pontos públicos que ainda podem expor termos de passagem, bilhete, viagem, Pix, passageiro, embarque ou domínios antigos.

## Escopo

Verificar ocorrências em:

- controllers públicos antigos;
- serviços de PDF de bilhete;
- páginas públicas de validação de bilhete;
- serviços antigos de WhatsApp;
- propriedades e URLs antigas;
- textos com VaiRápido, passagem, bilhete, viagem, reserva, embarque, Pix e api-vairapido.

## Regra de segurança

Nada em `src/main/resources/db/migration` será apagado nesta fase.

Migrations antigas podem ser mantidas até decidirmos se o banco será recriado do zero ou se a base atual continuará evoluindo com Flyway.

## Próxima decisão

Após a varredura, separar os resultados em:

1. Remover agora: textos públicos e controllers não usados.
2. Isolar como legado: serviços grandes de WhatsApp e fluxo de transporte.
3. Manter temporariamente: classes/tabelas que ainda quebram o build se removidas.

## Testes obrigatórios após qualquer alteração

- `mvn clean package -DskipTests`
- `/actuator/health`
- `/api/v1/health`
- `/api/v1/public/branding/secretariapay`
- `/api/v1/public/branding/institutions/imetro`
- `/branding/secretariapay-logo.png`
- fluxo financeiro já validado: cobranças, comprovativos, recibos e histórico de mensagens.
