# Teste controlado — documentos académicos

Branch: `feat/academic-documents-demo`

Objetivo: validar preços institucionais, cards do WhatsApp e demonstração de declaração simples com assinatura eletrónica demonstrativa de Zakeu António Zengo.

## Regras

- Não mesclar na `main` antes da aprovação final.
- Fazer backup do PostgreSQL e do `.env.production`.
- Manter o PostgreSQL em execução durante a troca da API.
- Validar as migrations `203607131900` e `203607132000`.
- Usar a matrícula de teste `202301404`.
- Confirmar o número de WhatsApp do estudante antes do envio.
- Em caso de rollback completo, restaurar também o backup do banco, porque as migrations são progressivas.

## Sequência

1. Guardar branch, commit e imagem atuais.
2. Criar backup do banco e do ambiente.
3. Trocar para `feat/academic-documents-demo`.
4. Construir a imagem da API.
5. Recriar somente o container da API.
6. Verificar Flyway, health checks e logs.
7. Testar pelo Preview do frontend.
8. Registrar resultado no PR #7.
