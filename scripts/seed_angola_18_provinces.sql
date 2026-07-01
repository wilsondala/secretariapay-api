BEGIN;

-- =========================================================
-- SecretáriaPay - Massa Angola Demo 18 Províncias
-- País resolvido pela cidade no bot:
-- country=AO, currency=AOA, documents=BI/PASSPORT
-- =========================================================

INSERT INTO transport_companies (
    name,
    trade_name,
    document_number,
    email,
    phone,
    whatsapp,
    status,
    created_at,
    updated_at
)
VALUES
('MACON Transportes Demo', 'MACON Demo', 'AO-DEMO-MACON-001', 'macon.demo@SecretariaPay.ao', '+244900000001', '+244900000001', 'ACTIVE', NOW(), NOW()),
('TCUL Interprovincial Demo', 'TCUL Demo', 'AO-DEMO-TCUL-002', 'tcul.demo@SecretariaPay.ao', '+244900000002', '+244900000002', 'ACTIVE', NOW(), NOW()),
('AngoReal Express Demo', 'AngoReal Demo', 'AO-DEMO-ANGOREAL-003', 'angoreal.demo@SecretariaPay.ao', '+244900000003', '+244900000003', 'ACTIVE', NOW(), NOW()),
('Benguela Bus Demo', 'Benguela Bus Demo', 'AO-DEMO-BENGUELA-004', 'benguela.demo@SecretariaPay.ao', '+244900000004', '+244900000004', 'ACTIVE', NOW(), NOW()),
('Huambo Express Demo', 'Huambo Express Demo', 'AO-DEMO-HUAMBO-005', 'huambo.demo@SecretariaPay.ao', '+244900000005', '+244900000005', 'ACTIVE', NOW(), NOW()),
('Lubango Executivo Demo', 'Lubango Executivo Demo', 'AO-DEMO-LUBANGO-006', 'lubango.demo@SecretariaPay.ao', '+244900000006', '+244900000006', 'ACTIVE', NOW(), NOW()),
('Namibe Express Demo', 'Namibe Express Demo', 'AO-DEMO-NAMIBE-007', 'namibe.demo@SecretariaPay.ao', '+244900000007', '+244900000007', 'ACTIVE', NOW(), NOW()),
('Cabinda Shuttle Demo', 'Cabinda Shuttle Demo', 'AO-DEMO-CABINDA-008', 'cabinda.demo@SecretariaPay.ao', '+244900000008', '+244900000008', 'ACTIVE', NOW(), NOW()),
('Malanje Line Demo', 'Malanje Line Demo', 'AO-DEMO-MALANJE-009', 'malanje.demo@SecretariaPay.ao', '+244900000009', '+244900000009', 'ACTIVE', NOW(), NOW()),
('Zaire Bus Demo', 'Zaire Bus Demo', 'AO-DEMO-ZAIRE-010', 'zaire.demo@SecretariaPay.ao', '+244900000010', '+244900000010', 'ACTIVE', NOW(), NOW())
ON CONFLICT (document_number) DO NOTHING;

-- Rotas Angola: Luanda ida e volta para as capitais/províncias
INSERT INTO routes (
    origin_city,
    origin_state,
    origin_terminal,
    destination_city,
    destination_state,
    destination_terminal,
    distance_km,
    estimated_duration_minutes,
    status,
    created_at,
    updated_at
)
VALUES
('Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 'Caxito', 'Bengo', 'Terminal Rodoviário de Caxito', 60, 90, 'ACTIVE', NOW(), NOW()),
('Caxito', 'Bengo', 'Terminal Rodoviário de Caxito', 'Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 60, 90, 'ACTIVE', NOW(), NOW()),

('Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 'Benguela', 'Benguela', 'Terminal Rodoviário de Benguela', 540, 480, 'ACTIVE', NOW(), NOW()),
('Benguela', 'Benguela', 'Terminal Rodoviário de Benguela', 'Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 540, 480, 'ACTIVE', NOW(), NOW()),

('Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 'Cuito', 'Bié', 'Terminal Rodoviário do Cuito', 710, 660, 'ACTIVE', NOW(), NOW()),
('Cuito', 'Bié', 'Terminal Rodoviário do Cuito', 'Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 710, 660, 'ACTIVE', NOW(), NOW()),

('Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 'Cabinda', 'Cabinda', 'Terminal Rodoviário de Cabinda', 480, 600, 'ACTIVE', NOW(), NOW()),
('Cabinda', 'Cabinda', 'Terminal Rodoviário de Cabinda', 'Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 480, 600, 'ACTIVE', NOW(), NOW()),

('Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 'Menongue', 'Cuando Cubango', 'Terminal Rodoviário de Menongue', 980, 900, 'ACTIVE', NOW(), NOW()),
('Menongue', 'Cuando Cubango', 'Terminal Rodoviário de Menongue', 'Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 980, 900, 'ACTIVE', NOW(), NOW()),

('Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 'Ndalatando', 'Cuanza Norte', 'Terminal Rodoviário de Ndalatando', 250, 240, 'ACTIVE', NOW(), NOW()),
('Ndalatando', 'Cuanza Norte', 'Terminal Rodoviário de Ndalatando', 'Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 250, 240, 'ACTIVE', NOW(), NOW()),

('Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 'Sumbe', 'Cuanza Sul', 'Terminal Rodoviário do Sumbe', 330, 300, 'ACTIVE', NOW(), NOW()),
('Sumbe', 'Cuanza Sul', 'Terminal Rodoviário do Sumbe', 'Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 330, 300, 'ACTIVE', NOW(), NOW()),

('Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 'Ondjiva', 'Cunene', 'Terminal Rodoviário de Ondjiva', 1150, 1050, 'ACTIVE', NOW(), NOW()),
('Ondjiva', 'Cunene', 'Terminal Rodoviário de Ondjiva', 'Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 1150, 1050, 'ACTIVE', NOW(), NOW()),

('Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 'Huambo', 'Huambo', 'Terminal Rodoviário do Huambo', 600, 540, 'ACTIVE', NOW(), NOW()),
('Huambo', 'Huambo', 'Terminal Rodoviário do Huambo', 'Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 600, 540, 'ACTIVE', NOW(), NOW()),

('Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 'Lubango', 'Huíla', 'Terminal Rodoviário do Lubango', 880, 780, 'ACTIVE', NOW(), NOW()),
('Lubango', 'Huíla', 'Terminal Rodoviário do Lubango', 'Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 880, 780, 'ACTIVE', NOW(), NOW()),

('Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 'Dundo', 'Lunda Norte', 'Terminal Rodoviário do Dundo', 1000, 960, 'ACTIVE', NOW(), NOW()),
('Dundo', 'Lunda Norte', 'Terminal Rodoviário do Dundo', 'Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 1000, 960, 'ACTIVE', NOW(), NOW()),

('Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 'Saurimo', 'Lunda Sul', 'Terminal Rodoviário de Saurimo', 940, 900, 'ACTIVE', NOW(), NOW()),
('Saurimo', 'Lunda Sul', 'Terminal Rodoviário de Saurimo', 'Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 940, 900, 'ACTIVE', NOW(), NOW()),

('Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 'Malanje', 'Malanje', 'Terminal Rodoviário de Malanje', 380, 360, 'ACTIVE', NOW(), NOW()),
('Malanje', 'Malanje', 'Terminal Rodoviário de Malanje', 'Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 380, 360, 'ACTIVE', NOW(), NOW()),

('Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 'Luena', 'Moxico', 'Terminal Rodoviário do Luena', 1310, 1200, 'ACTIVE', NOW(), NOW()),
('Luena', 'Moxico', 'Terminal Rodoviário do Luena', 'Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 1310, 1200, 'ACTIVE', NOW(), NOW()),

('Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 'Moçâmedes', 'Namibe', 'Terminal Rodoviário de Moçâmedes', 1010, 930, 'ACTIVE', NOW(), NOW()),
('Moçâmedes', 'Namibe', 'Terminal Rodoviário de Moçâmedes', 'Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 1010, 930, 'ACTIVE', NOW(), NOW()),

('Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 'Uíge', 'Uíge', 'Terminal Rodoviário do Uíge', 345, 330, 'ACTIVE', NOW(), NOW()),
('Uíge', 'Uíge', 'Terminal Rodoviário do Uíge', 'Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 345, 330, 'ACTIVE', NOW(), NOW()),

('Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 'Mbanza Kongo', 'Zaire', 'Terminal Rodoviário de Mbanza Kongo', 480, 450, 'ACTIVE', NOW(), NOW()),
('Mbanza Kongo', 'Zaire', 'Terminal Rodoviário de Mbanza Kongo', 'Luanda', 'Luanda', 'Terminal Rodoviário de Luanda', 480, 450, 'ACTIVE', NOW(), NOW());

-- Viagens para 25/06/2026 e 26/06/2026
INSERT INTO trips (
    transport_company_id,
    route_id,
    departure_at,
    arrival_at,
    price,
    currency,
    total_seats,
    available_seats,
    bus_plate,
    vehicle_description,
    status,
    created_at,
    updated_at
)
SELECT
    c.id,
    r.id,
    d.departure_at,
    d.departure_at + (r.estimated_duration_minutes || ' minutes')::interval,
    CASE
        WHEN r.distance_km <= 100 THEN 3500
        WHEN r.distance_km <= 300 THEN 7500
        WHEN r.distance_km <= 500 THEN 12000
        WHEN r.distance_km <= 700 THEN 18000
        WHEN r.distance_km <= 900 THEN 25000
        ELSE 32000
    END AS price,
    'AOA',
    48,
    48,
    'LD-' || LPAD(((ROW_NUMBER() OVER ())::text), 4, '0') || '-AO',
    'Autocarro Executivo Angola Demo',
    'SCHEDULED',
    NOW(),
    NOW()
FROM routes r
CROSS JOIN (
    VALUES
    (TIMESTAMP '2026-06-25 07:00:00'),
    (TIMESTAMP '2026-06-25 14:00:00'),
    (TIMESTAMP '2026-06-26 07:00:00'),
    (TIMESTAMP '2026-06-26 14:00:00')
) AS d(departure_at)
JOIN LATERAL (
    SELECT id
    FROM transport_companies
    WHERE document_number LIKE 'AO-DEMO-%'
    ORDER BY random()
    LIMIT 1
) c ON TRUE
WHERE r.origin_state IN (
    'Luanda',
    'Bengo',
    'Benguela',
    'Bié',
    'Cabinda',
    'Cuando Cubango',
    'Cuanza Norte',
    'Cuanza Sul',
    'Cunene',
    'Huambo',
    'Huíla',
    'Lunda Norte',
    'Lunda Sul',
    'Malanje',
    'Moxico',
    'Namibe',
    'Uíge',
    'Zaire'
)
AND r.destination_state IN (
    'Luanda',
    'Bengo',
    'Benguela',
    'Bié',
    'Cabinda',
    'Cuando Cubango',
    'Cuanza Norte',
    'Cuanza Sul',
    'Cunene',
    'Huambo',
    'Huíla',
    'Lunda Norte',
    'Lunda Sul',
    'Malanje',
    'Moxico',
    'Namibe',
    'Uíge',
    'Zaire'
);

COMMIT;

