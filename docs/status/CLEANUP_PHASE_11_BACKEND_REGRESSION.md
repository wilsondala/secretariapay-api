# SecretáriaPay Académico - Fase 11: Teste final de regressão do backend MVP

Esta fase registra o teste final de regressão após a limpeza controlada do backend.

## Objetivo

Garantir que o núcleo do SecretáriaPay Académico continua funcionando depois de:

- Atualização de branding público.
- Atualização de branding institucional IMETRO.
- Correção das páginas legais públicas.
- Marcação do WhatsApp antigo como legado.
- Desativação do webhook legado de passagens.
- Bloqueio seguro das rotas antigas de transporte/passagens.

## O que o script valida

O script `scripts/test-secretariapay-regression.ps1` testa:

1. Health da API.
2. Login ADMIN.
3. Branding público SecretáriaPay.
4. Branding público IMETRO.
5. Logo pública PNG.
6. Instituições.
7. Cursos.
8. Dashboard financeiro.
9. Comprovativos.
10. Recibos.
11. Status do webhook legado desativado.
12. Bloqueio 410 da rota legada `/api/v1/tickets` com token.

## Regra de segurança

O teste de regressão não cria nem apaga dados. Ele faz consultas GET e valida comportamentos existentes.

## Próxima etapa

Após a Fase 11, o backend estará pronto para iniciar o frontend administrativo do SecretáriaPay Académico com segurança.
