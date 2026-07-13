-- Configuração dos preços oficiais partilhados pelo IMETRO.
-- Mantém os totais históricos importados e altera apenas o preço unitário usado nas novas cobranças.

UPDATE academic_service_catalog
SET unit_price = 33828.00,
    currency = 'AOA',
    active = TRUE,
    generates_guide = TRUE,
    generates_receipt = TRUE,
    available_whatsapp = TRUE,
    available_portal = TRUE,
    available_panel = TRUE,
    source_reference = 'IMETRO_TABELA_PRECOS_2026_07',
    updated_at = CURRENT_TIMESTAMP
WHERE code = 'TUITION';

UPDATE academic_service_catalog
SET unit_price = 28000.00,
    currency = 'AOA',
    active = TRUE,
    generates_guide = TRUE,
    generates_receipt = TRUE,
    available_whatsapp = TRUE,
    available_portal = TRUE,
    available_panel = TRUE,
    source_reference = 'IMETRO_TABELA_PRECOS_2026_07',
    updated_at = CURRENT_TIMESTAMP
WHERE code = 'ENROLLMENT_CONFIRMATION';

UPDATE academic_service_catalog
SET unit_price = 7800.00,
    currency = 'AOA',
    active = TRUE,
    generates_guide = TRUE,
    generates_receipt = TRUE,
    available_whatsapp = TRUE,
    available_portal = TRUE,
    available_panel = TRUE,
    source_reference = 'IMETRO_TABELA_PRECOS_2026_07',
    updated_at = CURRENT_TIMESTAMP
WHERE code = 'RESIT_EXAM';

UPDATE academic_service_catalog
SET unit_price = 17000.00,
    currency = 'AOA',
    active = TRUE,
    generates_guide = TRUE,
    generates_receipt = TRUE,
    available_whatsapp = TRUE,
    available_portal = TRUE,
    available_panel = TRUE,
    source_reference = 'IMETRO_TABELA_PRECOS_2026_07',
    updated_at = CURRENT_TIMESTAMP
WHERE code = 'SPECIAL_EXAM';

UPDATE academic_service_catalog
SET unit_price = 8625.00,
    currency = 'AOA',
    active = TRUE,
    generates_guide = TRUE,
    generates_receipt = TRUE,
    available_whatsapp = TRUE,
    available_portal = TRUE,
    available_panel = TRUE,
    source_reference = 'IMETRO_TABELA_PRECOS_2026_07',
    updated_at = CURRENT_TIMESTAMP
WHERE code = 'DECLARATION_WITH_GRADES';

UPDATE academic_service_catalog
SET unit_price = 4400.00,
    currency = 'AOA',
    active = TRUE,
    generates_guide = TRUE,
    generates_receipt = TRUE,
    available_whatsapp = TRUE,
    available_portal = TRUE,
    available_panel = TRUE,
    source_reference = 'IMETRO_TABELA_PRECOS_2026_07',
    updated_at = CURRENT_TIMESTAMP
WHERE code = 'DECLARATION_WITHOUT_GRADES';

INSERT INTO academic_service_catalog
(id, code, name, category, unit_price, historical_total, currency, active, generates_guide, generates_receipt,
 allows_discount, allows_penalty, available_whatsapp, available_portal, available_panel, display_order,
 source_reference, created_at, updated_at)
VALUES
(gen_random_uuid(), 'ENROLLMENT', 'Matrícula', 'ENROLLMENT', 30800.00, NULL, 'AOA', TRUE, TRUE, TRUE,
 FALSE, FALSE, TRUE, TRUE, TRUE, 82, 'IMETRO_TABELA_PRECOS_2026_07', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'REGISTRATION', 'Inscrição', 'ENROLLMENT', 5000.00, NULL, 'AOA', TRUE, TRUE, TRUE,
 FALSE, FALSE, TRUE, TRUE, TRUE, 84, 'IMETRO_TABELA_PRECOS_2026_07', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'CERTIFICATE', 'Certificado', 'DOCUMENT', 39000.00, NULL, 'AOA', TRUE, TRUE, TRUE,
 FALSE, FALSE, TRUE, TRUE, TRUE, 145, 'IMETRO_TABELA_PRECOS_2026_07', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'DIPLOMA', 'Diploma', 'DOCUMENT', 67000.00, NULL, 'AOA', TRUE, TRUE, TRUE,
 FALSE, FALSE, TRUE, TRUE, TRUE, 150, 'IMETRO_TABELA_PRECOS_2026_07', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    category = EXCLUDED.category,
    unit_price = EXCLUDED.unit_price,
    currency = EXCLUDED.currency,
    active = EXCLUDED.active,
    generates_guide = EXCLUDED.generates_guide,
    generates_receipt = EXCLUDED.generates_receipt,
    available_whatsapp = EXCLUDED.available_whatsapp,
    available_portal = EXCLUDED.available_portal,
    available_panel = EXCLUDED.available_panel,
    display_order = EXCLUDED.display_order,
    source_reference = EXCLUDED.source_reference,
    updated_at = CURRENT_TIMESTAMP;

UPDATE academic_service_catalog
SET active = FALSE,
    available_whatsapp = FALSE,
    available_portal = FALSE,
    source_reference = 'SUBSTITUIDO_POR_CERTIFICATE_E_DIPLOMA',
    updated_at = CURRENT_TIMESTAMP
WHERE code = 'DIPLOMA_CERTIFICATE';
