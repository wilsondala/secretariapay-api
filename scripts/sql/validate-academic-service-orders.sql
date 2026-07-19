\set ON_ERROR_STOP on

\echo '=== SecretáriaPay: validação da migration de pedidos académicos ==='

SELECT installed_rank, version, description, success
FROM flyway_schema_history
WHERE version = '203607181000';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM flyway_schema_history
        WHERE version = '203607181000' AND success = TRUE
    ) THEN
        RAISE EXCEPTION 'Migration 203607181000 não foi aplicada com sucesso.';
    END IF;
END $$;

\echo '=== Estrutura e índices ==='

SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name = 'academic_service_orders';

SELECT indexname
FROM pg_indexes
WHERE schemaname = 'public'
  AND tablename = 'academic_service_orders'
ORDER BY indexname;

DO $$
DECLARE
    missing_indexes TEXT;
BEGIN
    SELECT string_agg(required.index_name, ', ')
    INTO missing_indexes
    FROM (
        VALUES
            ('idx_academic_service_orders_status'),
            ('idx_academic_service_orders_student'),
            ('idx_academic_service_orders_service'),
            ('idx_academic_service_orders_ready_secretaria')
    ) AS required(index_name)
    WHERE NOT EXISTS (
        SELECT 1
        FROM pg_indexes existing
        WHERE existing.schemaname = 'public'
          AND existing.tablename = 'academic_service_orders'
          AND existing.indexname = required.index_name
    );

    IF missing_indexes IS NOT NULL THEN
        RAISE EXCEPTION 'Índices obrigatórios ausentes: %', missing_indexes;
    END IF;
END $$;

\echo '=== Totais por estado ==='

SELECT status, COUNT(*) AS total
FROM academic_service_orders
GROUP BY status
ORDER BY CASE status
    WHEN 'SOLICITADO' THEN 1
    WHEN 'AGUARDANDO_PAGAMENTO' THEN 2
    WHEN 'PAGO' THEN 3
    WHEN 'DOCUMENTO_GERADO' THEN 4
    WHEN 'PRONTO_PARA_IMPRESSAO' THEN 5
    WHEN 'IMPRESSO' THEN 6
    WHEN 'AGUARDANDO_ASSINATURA' THEN 7
    WHEN 'ASSINADO' THEN 8
    WHEN 'PRONTO_PARA_LEVANTAMENTO' THEN 9
    WHEN 'WHATSAPP_ENVIADO' THEN 10
    WHEN 'ENTREGUE' THEN 11
    ELSE 99
END;

\echo '=== Verificação de invariantes ==='

WITH inconsistencies AS (
    SELECT o.id, o.order_code, o.status, 'PAGO_SEM_COBRANCA_CONFIRMADA' AS problem
    FROM academic_service_orders o
    LEFT JOIN charges c ON c.id = o.charge_id
    WHERE o.status IN (
        'PAGO','DOCUMENTO_GERADO','PRONTO_PARA_IMPRESSAO','IMPRESSO',
        'AGUARDANDO_ASSINATURA','ASSINADO','PRONTO_PARA_LEVANTAMENTO',
        'WHATSAPP_ENVIADO','ENTREGUE'
    )
      AND (c.id IS NULL OR c.status <> 'PAID')

    UNION ALL

    SELECT o.id, o.order_code, o.status, 'ETAPA_DOCUMENTAL_SEM_DOCUMENTO' AS problem
    FROM academic_service_orders o
    WHERE o.status IN (
        'DOCUMENTO_GERADO','PRONTO_PARA_IMPRESSAO','IMPRESSO',
        'AGUARDANDO_ASSINATURA','ASSINADO','PRONTO_PARA_LEVANTAMENTO',
        'WHATSAPP_ENVIADO','ENTREGUE'
    )
      AND o.document_request_id IS NULL

    UNION ALL

    SELECT o.id, o.order_code, o.status, 'IMPRESSAO_SEM_RESPONSAVEL_OU_DATA' AS problem
    FROM academic_service_orders o
    WHERE o.status IN (
        'IMPRESSO','AGUARDANDO_ASSINATURA','ASSINADO',
        'PRONTO_PARA_LEVANTAMENTO','WHATSAPP_ENVIADO','ENTREGUE'
    )
      AND (o.printed_at IS NULL OR NULLIF(BTRIM(o.printed_by), '') IS NULL)

    UNION ALL

    SELECT o.id, o.order_code, o.status, 'ASSINATURA_SEM_RESPONSAVEL_OU_DATA' AS problem
    FROM academic_service_orders o
    WHERE o.status IN ('ASSINADO','PRONTO_PARA_LEVANTAMENTO','WHATSAPP_ENVIADO','ENTREGUE')
      AND (o.signed_at IS NULL OR NULLIF(BTRIM(o.signed_by), '') IS NULL)

    UNION ALL

    SELECT o.id, o.order_code, o.status, 'WHATSAPP_SEM_DATA_OU_RESPONSAVEL' AS problem
    FROM academic_service_orders o
    WHERE o.status IN ('WHATSAPP_ENVIADO','ENTREGUE')
      AND (o.whatsapp_sent_at IS NULL OR NULLIF(BTRIM(o.whatsapp_sent_by), '') IS NULL)

    UNION ALL

    SELECT o.id, o.order_code, o.status, 'ENTREGA_INCOMPLETA' AS problem
    FROM academic_service_orders o
    WHERE o.status = 'ENTREGUE'
      AND (
          o.delivered_at IS NULL
          OR NULLIF(BTRIM(o.delivered_by), '') IS NULL
          OR NULLIF(BTRIM(o.recipient_name), '') IS NULL
          OR NULLIF(BTRIM(o.recipient_document_number), '') IS NULL
      )
)
SELECT *
FROM inconsistencies
ORDER BY order_code, problem;

DO $$
DECLARE
    inconsistent_count BIGINT;
BEGIN
    WITH inconsistencies AS (
        SELECT o.id
        FROM academic_service_orders o
        LEFT JOIN charges c ON c.id = o.charge_id
        WHERE o.status IN (
            'PAGO','DOCUMENTO_GERADO','PRONTO_PARA_IMPRESSAO','IMPRESSO',
            'AGUARDANDO_ASSINATURA','ASSINADO','PRONTO_PARA_LEVANTAMENTO',
            'WHATSAPP_ENVIADO','ENTREGUE'
        )
          AND (c.id IS NULL OR c.status <> 'PAID')

        UNION ALL

        SELECT o.id FROM academic_service_orders o
        WHERE o.status IN (
            'DOCUMENTO_GERADO','PRONTO_PARA_IMPRESSAO','IMPRESSO',
            'AGUARDANDO_ASSINATURA','ASSINADO','PRONTO_PARA_LEVANTAMENTO',
            'WHATSAPP_ENVIADO','ENTREGUE'
        ) AND o.document_request_id IS NULL

        UNION ALL

        SELECT o.id FROM academic_service_orders o
        WHERE o.status = 'ENTREGUE'
          AND (
              o.delivered_at IS NULL
              OR NULLIF(BTRIM(o.delivered_by), '') IS NULL
              OR NULLIF(BTRIM(o.recipient_name), '') IS NULL
              OR NULLIF(BTRIM(o.recipient_document_number), '') IS NULL
          )
    )
    SELECT COUNT(*) INTO inconsistent_count FROM inconsistencies;

    IF inconsistent_count > 0 THEN
        RAISE EXCEPTION 'Foram encontradas % inconsistências críticas no fluxo académico.', inconsistent_count;
    END IF;
END $$;

\echo 'Validação SQL concluída sem inconsistências críticas.'
