BEGIN;

INSERT INTO transport_companies (
    name,
    trade_name,
    document_number,
    email,
    phone,
    whatsapp,
    logo_url,
    status,
    created_at,
    updated_at
)
VALUES
    (
        'Expresso Brasil Teste LTDA',
        'Expresso Brasil',
        'VR-BR-TEST-0001',
        'contato@expressobrasil.test',
        '+551130000001',
        '+551130000001',
        NULL,
        'ACTIVE',
        NOW(),
        NOW()
    ),
    (
        'Rápido Sudeste Teste LTDA',
        'Rápido Sudeste',
        'VR-BR-TEST-0002',
        'contato@rapidosudeste.test',
        '+551130000002',
        '+551130000002',
        NULL,
        'ACTIVE',
        NOW(),
        NOW()
    ),
    (
        'Atlântico Bus Teste LTDA',
        'Atlântico Bus',
        'VR-BR-TEST-0003',
        'contato@atlanticobus.test',
        '+551130000003',
        '+551130000003',
        NULL,
        'ACTIVE',
        NOW(),
        NOW()
    ),
    (
        'Nacional Rodoviária Teste LTDA',
        'Nacional Rodoviária',
        'VR-BR-TEST-0004',
        'contato@nacionalrodoviaria.test',
        '+551130000004',
        '+551130000004',
        NULL,
        'ACTIVE',
        NOW(),
        NOW()
    ),
    (
        'União Brasil Bus Teste LTDA',
        'União Brasil Bus',
        'VR-BR-TEST-0005',
        'contato@uniaobrasilbus.test',
        '+551130000005',
        '+551130000005',
        NULL,
        'ACTIVE',
        NOW(),
        NOW()
    ),
    (
        'Via Nordeste Teste LTDA',
        'Via Nordeste',
        'VR-BR-TEST-0006',
        'contato@vianordeste.test',
        '+551130000006',
        '+551130000006',
        NULL,
        'ACTIVE',
        NOW(),
        NOW()
    ),
    (
        'Sul Premium Bus Teste LTDA',
        'Sul Premium Bus',
        'VR-BR-TEST-0007',
        'contato@sulpremiumbus.test',
        '+551130000007',
        '+551130000007',
        NULL,
        'ACTIVE',
        NOW(),
        NOW()
    ),
    (
        'Centro Oeste Express Teste LTDA',
        'Centro Oeste Express',
        'VR-BR-TEST-0008',
        'contato@centrooesteexpress.test',
        '+551130000008',
        '+551130000008',
        NULL,
        'ACTIVE',
        NOW(),
        NOW()
    )
ON CONFLICT (document_number) DO UPDATE
SET
    name = EXCLUDED.name,
    trade_name = EXCLUDED.trade_name,
    email = EXCLUDED.email,
    phone = EXCLUDED.phone,
    whatsapp = EXCLUDED.whatsapp,
    status = EXCLUDED.status,
    updated_at = NOW();

DO $$
DECLARE
    company_ids UUID[];
    current_company_id UUID;
    current_route_id UUID;

    route_data RECORD;
    slot_data RECORD;

    day_offset INTEGER;
    departure_timestamp TIMESTAMP;
    arrival_timestamp TIMESTAMP;
    generated_plate TEXT;
    generated_price NUMERIC(10, 2);
BEGIN
    SELECT ARRAY_AGG(id ORDER BY trade_name)
    INTO company_ids
    FROM transport_companies
    WHERE document_number LIKE 'VR-BR-TEST-%';

    IF company_ids IS NULL OR ARRAY_LENGTH(company_ids, 1) IS NULL THEN
        RAISE EXCEPTION 'Nenhuma empresa de teste encontrada.';
    END IF;

    FOR route_data IN
        SELECT *
        FROM (
            VALUES
                ('São Paulo', 'SP', 'Terminal Rodoviário Tietê', 'Rio de Janeiro', 'RJ', 'Rodoviária Novo Rio', 430.00, 390, 89.90),
                ('Rio de Janeiro', 'RJ', 'Rodoviária Novo Rio', 'São Paulo', 'SP', 'Terminal Rodoviário Tietê', 430.00, 390, 89.90),

                ('São Paulo', 'SP', 'Terminal Rodoviário Tietê', 'Belo Horizonte', 'MG', 'Terminal Rodoviário de Belo Horizonte', 590.00, 510, 119.90),
                ('Belo Horizonte', 'MG', 'Terminal Rodoviário de Belo Horizonte', 'São Paulo', 'SP', 'Terminal Rodoviário Tietê', 590.00, 510, 119.90),

                ('São Paulo', 'SP', 'Terminal Rodoviário Barra Funda', 'Curitiba', 'PR', 'Rodoferroviária de Curitiba', 410.00, 370, 99.90),
                ('Curitiba', 'PR', 'Rodoferroviária de Curitiba', 'São Paulo', 'SP', 'Terminal Rodoviário Barra Funda', 410.00, 370, 99.90),

                ('São Paulo', 'SP', 'Terminal Rodoviário Tietê', 'Florianópolis', 'SC', 'Terminal Rita Maria', 700.00, 620, 159.90),
                ('Florianópolis', 'SC', 'Terminal Rita Maria', 'São Paulo', 'SP', 'Terminal Rodoviário Tietê', 700.00, 620, 159.90),

                ('São Paulo', 'SP', 'Terminal Rodoviário Tietê', 'Porto Alegre', 'RS', 'Rodoviária de Porto Alegre', 1120.00, 960, 229.90),
                ('Porto Alegre', 'RS', 'Rodoviária de Porto Alegre', 'São Paulo', 'SP', 'Terminal Rodoviário Tietê', 1120.00, 960, 229.90),

                ('Rio de Janeiro', 'RJ', 'Rodoviária Novo Rio', 'Belo Horizonte', 'MG', 'Terminal Rodoviário de Belo Horizonte', 440.00, 420, 109.90),
                ('Belo Horizonte', 'MG', 'Terminal Rodoviário de Belo Horizonte', 'Rio de Janeiro', 'RJ', 'Rodoviária Novo Rio', 440.00, 420, 109.90),

                ('Rio de Janeiro', 'RJ', 'Rodoviária Novo Rio', 'Vitória', 'ES', 'Rodoviária de Vitória', 520.00, 480, 129.90),
                ('Vitória', 'ES', 'Rodoviária de Vitória', 'Rio de Janeiro', 'RJ', 'Rodoviária Novo Rio', 520.00, 480, 129.90),

                ('Brasília', 'DF', 'Rodoviária Interestadual de Brasília', 'Goiânia', 'GO', 'Terminal Rodoviário de Goiânia', 210.00, 210, 59.90),
                ('Goiânia', 'GO', 'Terminal Rodoviário de Goiânia', 'Brasília', 'DF', 'Rodoviária Interestadual de Brasília', 210.00, 210, 59.90),

                ('Brasília', 'DF', 'Rodoviária Interestadual de Brasília', 'Salvador', 'BA', 'Terminal Rodoviário de Salvador', 1450.00, 1260, 279.90),
                ('Salvador', 'BA', 'Terminal Rodoviário de Salvador', 'Brasília', 'DF', 'Rodoviária Interestadual de Brasília', 1450.00, 1260, 279.90),

                ('Salvador', 'BA', 'Terminal Rodoviário de Salvador', 'Aracaju', 'SE', 'Rodoviária Nova de Aracaju', 330.00, 300, 79.90),
                ('Aracaju', 'SE', 'Rodoviária Nova de Aracaju', 'Salvador', 'BA', 'Terminal Rodoviário de Salvador', 330.00, 300, 79.90),

                ('Salvador', 'BA', 'Terminal Rodoviário de Salvador', 'Recife', 'PE', 'TIP Recife', 800.00, 720, 179.90),
                ('Recife', 'PE', 'TIP Recife', 'Salvador', 'BA', 'Terminal Rodoviário de Salvador', 800.00, 720, 179.90),

                ('Recife', 'PE', 'TIP Recife', 'Fortaleza', 'CE', 'Terminal Rodoviário Engenheiro João Thomé', 800.00, 760, 189.90),
                ('Fortaleza', 'CE', 'Terminal Rodoviário Engenheiro João Thomé', 'Recife', 'PE', 'TIP Recife', 800.00, 760, 189.90),

                ('Fortaleza', 'CE', 'Terminal Rodoviário Engenheiro João Thomé', 'Natal', 'RN', 'Terminal Rodoviário de Natal', 535.00, 510, 139.90),
                ('Natal', 'RN', 'Terminal Rodoviário de Natal', 'Fortaleza', 'CE', 'Terminal Rodoviário Engenheiro João Thomé', 535.00, 510, 139.90),

                ('Natal', 'RN', 'Terminal Rodoviário de Natal', 'João Pessoa', 'PB', 'Terminal Rodoviário de João Pessoa', 185.00, 180, 49.90),
                ('João Pessoa', 'PB', 'Terminal Rodoviário de João Pessoa', 'Natal', 'RN', 'Terminal Rodoviário de Natal', 185.00, 180, 49.90),

                ('João Pessoa', 'PB', 'Terminal Rodoviário de João Pessoa', 'Recife', 'PE', 'TIP Recife', 120.00, 140, 39.90),
                ('Recife', 'PE', 'TIP Recife', 'João Pessoa', 'PB', 'Terminal Rodoviário de João Pessoa', 120.00, 140, 39.90),

                ('Curitiba', 'PR', 'Rodoferroviária de Curitiba', 'Florianópolis', 'SC', 'Terminal Rita Maria', 300.00, 270, 79.90),
                ('Florianópolis', 'SC', 'Terminal Rita Maria', 'Curitiba', 'PR', 'Rodoferroviária de Curitiba', 300.00, 270, 79.90),

                ('Florianópolis', 'SC', 'Terminal Rita Maria', 'Porto Alegre', 'RS', 'Rodoviária de Porto Alegre', 470.00, 420, 119.90),
                ('Porto Alegre', 'RS', 'Rodoviária de Porto Alegre', 'Florianópolis', 'SC', 'Terminal Rita Maria', 470.00, 420, 119.90),

                ('Cuiabá', 'MT', 'Terminal Rodoviário de Cuiabá', 'Campo Grande', 'MS', 'Terminal Rodoviário de Campo Grande', 700.00, 660, 169.90),
                ('Campo Grande', 'MS', 'Terminal Rodoviário de Campo Grande', 'Cuiabá', 'MT', 'Terminal Rodoviário de Cuiabá', 700.00, 660, 169.90),

                ('Campo Grande', 'MS', 'Terminal Rodoviário de Campo Grande', 'São Paulo', 'SP', 'Terminal Rodoviário Barra Funda', 1010.00, 900, 219.90),
                ('São Paulo', 'SP', 'Terminal Rodoviário Barra Funda', 'Campo Grande', 'MS', 'Terminal Rodoviário de Campo Grande', 1010.00, 900, 219.90),

                ('Belém', 'PA', 'Terminal Rodoviário de Belém', 'Manaus', 'AM', 'Terminal Rodoviário de Manaus', 1300.00, 1440, 299.90),
                ('Manaus', 'AM', 'Terminal Rodoviário de Manaus', 'Belém', 'PA', 'Terminal Rodoviário de Belém', 1300.00, 1440, 299.90)
        ) AS r(
            origin_city,
            origin_state,
            origin_terminal,
            destination_city,
            destination_state,
            destination_terminal,
            distance_km,
            duration_minutes,
            base_price
        )
    LOOP
        current_route_id := NULL;

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
        SELECT
            route_data.origin_city,
            route_data.origin_state,
            route_data.origin_terminal,
            route_data.destination_city,
            route_data.destination_state,
            route_data.destination_terminal,
            route_data.distance_km,
            route_data.duration_minutes,
            'ACTIVE',
            NOW(),
            NOW()
        WHERE NOT EXISTS (
            SELECT 1
            FROM routes existing_route
            WHERE LOWER(existing_route.origin_city) = LOWER(route_data.origin_city)
              AND LOWER(existing_route.destination_city) = LOWER(route_data.destination_city)
              AND LOWER(COALESCE(existing_route.origin_terminal, '')) = LOWER(route_data.origin_terminal)
              AND LOWER(COALESCE(existing_route.destination_terminal, '')) = LOWER(route_data.destination_terminal)
        )
        RETURNING id INTO current_route_id;

        IF current_route_id IS NULL THEN
            SELECT id
            INTO current_route_id
            FROM routes existing_route
            WHERE LOWER(existing_route.origin_city) = LOWER(route_data.origin_city)
              AND LOWER(existing_route.destination_city) = LOWER(route_data.destination_city)
              AND LOWER(COALESCE(existing_route.origin_terminal, '')) = LOWER(route_data.origin_terminal)
              AND LOWER(COALESCE(existing_route.destination_terminal, '')) = LOWER(route_data.destination_terminal)
            ORDER BY created_at ASC
            LIMIT 1;
        END IF;

        FOR day_offset IN 0..29 LOOP
            FOR slot_data IN
                SELECT *
                FROM (
                    VALUES
                        (TIME '06:30', 0.00, 'Executivo'),
                        (TIME '13:00', 18.00, 'Semi-leito'),
                        (TIME '21:30', 45.00, 'Leito')
                ) AS s(departure_time, price_addition, vehicle_type)
            LOOP
                departure_timestamp := (CURRENT_DATE + day_offset) + slot_data.departure_time;
                arrival_timestamp := departure_timestamp + MAKE_INTERVAL(mins => route_data.duration_minutes);

                current_company_id := company_ids[
                    1 + (
                        (
                            day_offset
                            + EXTRACT(HOUR FROM slot_data.departure_time)::INTEGER
                            + LENGTH(route_data.origin_city)
                            + LENGTH(route_data.destination_city)
                        ) % ARRAY_LENGTH(company_ids, 1)
                    )
                ];

                generated_plate :=
                    'VRSEED-'
                    || SUBSTRING(REPLACE(current_route_id::TEXT, '-', '') FROM 1 FOR 6)
                    || '-'
                    || day_offset
                    || '-'
                    || REPLACE(SUBSTRING(slot_data.departure_time::TEXT FROM 1 FOR 5), ':', '');

                generated_price := route_data.base_price + slot_data.price_addition;

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
                    current_company_id,
                    current_route_id,
                    departure_timestamp,
                    arrival_timestamp,
                    generated_price,
                    'BRL',
                    46,
                    46,
                    generated_plate,
                    slot_data.vehicle_type || ' - Massa nacional Brasil',
                    'SCHEDULED',
                    NOW(),
                    NOW()
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM trips existing_trip
                    WHERE existing_trip.route_id = current_route_id
                      AND existing_trip.departure_at = departure_timestamp
                      AND existing_trip.bus_plate = generated_plate
                );
            END LOOP;
        END LOOP;
    END LOOP;
END $$;

COMMIT;

SELECT
    'transport_companies' AS tabela,
    COUNT(*) AS total
FROM transport_companies
WHERE document_number LIKE 'VR-BR-TEST-%'

UNION ALL

SELECT
    'routes' AS tabela,
    COUNT(*) AS total
FROM routes

UNION ALL

SELECT
    'future_scheduled_trips' AS tabela,
    COUNT(*) AS total
FROM trips
WHERE status = 'SCHEDULED'
  AND departure_at >= CURRENT_DATE;