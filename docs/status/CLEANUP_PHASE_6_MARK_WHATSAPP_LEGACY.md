# Fase 6 - Marcação de WhatsApp legado

Esta fase marca os serviços antigos de WhatsApp herdados do fluxo VaiRápido como legado temporário.

Arquivos afetados:

- `src/main/java/com/secretariapay/api/service/WhatsappCommandService.java`
- `src/main/java/com/secretariapay/api/service/WhatsappFaqAnswerService.java`

Ação aplicada:

- Adição de comentário `LEGADO TEMPORÁRIO`.
- Adição de `@Deprecated(since = "2026-07-02", forRemoval = false)`.

Importante:

- Nenhum arquivo é apagado.
- Nenhuma entidade, migration ou tabela é removida.
- Nenhum endpoint novo é criado.
- A marcação serve para evitar uso acidental dos serviços antigos no fluxo novo do SecretáriaPay Académico.

Depois da aplicação:

1. Rodar build local.
2. Commitar alteração.
3. Validar produção somente se houver alteração de código publicada.
