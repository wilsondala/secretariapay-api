-- ==========================================================
-- SecretáriaPay Académico / IMETRO
-- RESET COMPLETO DA BASE DE TESTES REAIS
--
-- Executar somente no ambiente de teste/homologação autorizado.
-- Este script zera estudantes, cobranças, comprovativos e recibos.
-- Depois cria 3 estudantes oficiais de teste real e uma mensalidade
-- pendente para cada um, com vencimento amanhã.
-- ==========================================================

BEGIN;

-- 1. Limpeza forte da base de teste.
-- CASCADE remove dependências como recibos e comprovativos ligados às cobranças.
TRUNCATE TABLE payment_proofs, receipts, charges, students RESTART IDENTITY CASCADE;

-- 2. Estrutura académica mínima IMETRO
INSERT INTO institutions (
    id, name, legal_name, nif, email, phone, whatsapp, address, active, created_at, updated_at
) VALUES (
    '11111111-1111-1111-1111-111111111111',
    'IMETRO',
    'Instituto Superior Politécnico Metropolitano de Angola',
    '5000000000',
    'dcr_pay@imetroangola.com',
    '+244 000 000 000',
    '+244 000 000 000',
    'Luanda, Angola',
    true,
    now(),
    now()
)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    legal_name = EXCLUDED.legal_name,
    email = EXCLUDED.email,
    updated_at = now();

INSERT INTO courses (
    id, institution_id, name, code, faculty, duration_years, active, created_at, updated_at
) VALUES (
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    'Gestão Financeira e Académica',
    'GFA-TESTE',
    'Faculdade de Ciências Económicas e Gestão',
    4,
    true,
    now(),
    now()
)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    code = EXCLUDED.code,
    faculty = EXCLUDED.faculty,
    duration_years = EXCLUDED.duration_years,
    updated_at = now();

INSERT INTO academic_classes (
    id, course_id, name, academic_year, year_level, shift, active, created_at, updated_at
) VALUES (
    '33333333-3333-3333-3333-333333333333',
    '22222222-2222-2222-2222-222222222222',
    'Turma Testes Reais SecretáriaPay 2026',
    '2026',
    1,
    'NIGHT',
    true,
    now(),
    now()
)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    academic_year = EXCLUDED.academic_year,
    year_level = EXCLUDED.year_level,
    shift = EXCLUDED.shift,
    updated_at = now();

-- 3. Estudantes oficiais da base de testes reais
INSERT INTO students (
    id,
    academic_class_id,
    student_number,
    full_name,
    document_type,
    document_number,
    email,
    phone,
    whatsapp,
    status,
    financially_blocked,
    blocked_reason,
    created_at,
    updated_at
) VALUES
(
    '44444444-4444-4444-4444-444444444401',
    '33333333-3333-3333-3333-333333333333',
    '202301404',
    'Wilson dos Santos Kahango Dala',
    'BI',
    '001058899UE035',
    'dalakahango@hotmail.com',
    '+55 11 91510-2566',
    '+55 11 91510-2566',
    'ACTIVE',
    false,
    null,
    now(),
    now()
),
(
    '44444444-4444-4444-4444-444444444402',
    '33333333-3333-3333-3333-333333333333',
    '202301405',
    'Zaqueu Ricardo Fernando Antonio',
    'BI',
    '001058899UE036',
    'zrf.antonio@unesp.br',
    '+55 14 98199-8011',
    '+55 14 98199-8011',
    'ACTIVE',
    false,
    null,
    now(),
    now()
),
(
    '44444444-4444-4444-4444-444444444403',
    '33333333-3333-3333-3333-333333333333',
    '202301406',
    'Zakeu Antonio Zengo',
    'BI',
    '001058899UE037',
    'zzengo@hotmail.com',
    '+244 925 939 243',
    '+244 925 939 243',
    'ACTIVE',
    false,
    null,
    now(),
    now()
);

-- 4. Primeira mensalidade do ano letivo iniciado no mês passado.
-- Valor baixo para teste real controlado: 5.000,00 Kz.
-- Vencimento: amanhã.
INSERT INTO charges (
    id,
    student_id,
    charge_code,
    description,
    reference_month,
    due_date,
    amount,
    fine_amount,
    interest_amount,
    discount_amount,
    total_amount,
    currency,
    status,
    paid_at,
    cancelled_at,
    created_at,
    updated_at
) VALUES
(
    '55555555-5555-5555-5555-555555555401',
    '44444444-4444-4444-4444-444444444401',
    'REAL-TEST-2026-06-202301404',
    'Propina Junho/2026 - Teste real SecretáriaPay',
    'Junho/2026',
    (CURRENT_DATE + INTERVAL '1 day')::date,
    5000.00,
    0.00,
    0.00,
    0.00,
    5000.00,
    'AOA',
    'PENDING',
    null,
    null,
    now(),
    now()
),
(
    '55555555-5555-5555-5555-555555555402',
    '44444444-4444-4444-4444-444444444402',
    'REAL-TEST-2026-06-202301405',
    'Propina Junho/2026 - Teste real SecretáriaPay',
    'Junho/2026',
    (CURRENT_DATE + INTERVAL '1 day')::date,
    5000.00,
    0.00,
    0.00,
    0.00,
    5000.00,
    'AOA',
    'PENDING',
    null,
    null,
    now(),
    now()
),
(
    '55555555-5555-5555-5555-555555555403',
    '44444444-4444-4444-4444-444444444403',
    'REAL-TEST-2026-06-202301406',
    'Propina Junho/2026 - Teste real SecretáriaPay',
    'Junho/2026',
    (CURRENT_DATE + INTERVAL '1 day')::date,
    5000.00,
    0.00,
    0.00,
    0.00,
    5000.00,
    'AOA',
    'PENDING',
    null,
    null,
    now(),
    now()
);

COMMIT;

-- Conferência rápida
SELECT
    s.student_number AS matricula,
    s.full_name AS estudante,
    s.document_number AS bi,
    s.email,
    s.whatsapp,
    c.reference_month AS mes,
    c.due_date AS vencimento,
    c.amount AS valor_base,
    c.fine_amount AS multa,
    c.interest_amount AS juros,
    c.total_amount AS total,
    c.status
FROM students s
JOIN charges c ON c.student_id = s.id
ORDER BY s.student_number;
