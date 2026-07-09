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
-- CASCADE remove dependências como recibos, comprovativos e bloqueios ligados aos alunos/cobranças.
TRUNCATE TABLE payment_proofs, receipts, charges, academic_blocks, students RESTART IDENTITY CASCADE;

-- 2. Estrutura académica mínima IMETRO.
-- Importante: a instituição pode já existir com o mesmo NIF e outro ID.
-- Por isso reutilizamos a instituição existente em vez de forçar um ID fixo.
DO $$
DECLARE
    v_institution_id uuid;
    v_course_id uuid;
    v_class_id uuid;
BEGIN
    SELECT id
      INTO v_institution_id
      FROM institutions
     WHERE nif = '5000000000'
     ORDER BY created_at NULLS LAST
     LIMIT 1;

    IF v_institution_id IS NULL THEN
        v_institution_id := '11111111-1111-1111-1111-111111111111';
        INSERT INTO institutions (
            id, name, legal_name, nif, email, phone, whatsapp, address, active, created_at, updated_at
        ) VALUES (
            v_institution_id,
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
        );
    ELSE
        UPDATE institutions
           SET name = 'IMETRO',
               legal_name = 'Instituto Superior Politécnico Metropolitano de Angola',
               email = 'dcr_pay@imetroangola.com',
               phone = '+244 000 000 000',
               whatsapp = '+244 000 000 000',
               address = 'Luanda, Angola',
               active = true,
               updated_at = now()
         WHERE id = v_institution_id;
    END IF;

    SELECT id
      INTO v_course_id
      FROM courses
     WHERE id = '22222222-2222-2222-2222-222222222222'
        OR (institution_id = v_institution_id AND code = 'GFA-TESTE')
     ORDER BY created_at NULLS LAST
     LIMIT 1;

    IF v_course_id IS NULL THEN
        v_course_id := '22222222-2222-2222-2222-222222222222';
        INSERT INTO courses (
            id, institution_id, name, code, faculty, duration_years, active, created_at, updated_at
        ) VALUES (
            v_course_id,
            v_institution_id,
            'Gestão Financeira e Académica',
            'GFA-TESTE',
            'Faculdade de Ciências Económicas e Gestão',
            4,
            true,
            now(),
            now()
        );
    ELSE
        UPDATE courses
           SET institution_id = v_institution_id,
               name = 'Gestão Financeira e Académica',
               code = 'GFA-TESTE',
               faculty = 'Faculdade de Ciências Económicas e Gestão',
               duration_years = 4,
               active = true,
               updated_at = now()
         WHERE id = v_course_id;
    END IF;

    SELECT id
      INTO v_class_id
      FROM academic_classes
     WHERE id = '33333333-3333-3333-3333-333333333333'
        OR (course_id = v_course_id AND name = 'Turma Testes Reais SecretáriaPay 2026')
     ORDER BY created_at NULLS LAST
     LIMIT 1;

    IF v_class_id IS NULL THEN
        v_class_id := '33333333-3333-3333-3333-333333333333';
        INSERT INTO academic_classes (
            id, course_id, name, academic_year, year_level, shift, active, created_at, updated_at
        ) VALUES (
            v_class_id,
            v_course_id,
            'Turma Testes Reais SecretáriaPay 2026',
            '2026',
            1,
            'NIGHT',
            true,
            now(),
            now()
        );
    ELSE
        UPDATE academic_classes
           SET course_id = v_course_id,
               name = 'Turma Testes Reais SecretáriaPay 2026',
               academic_year = '2026',
               year_level = 1,
               shift = 'NIGHT',
               active = true,
               updated_at = now()
         WHERE id = v_class_id;
    END IF;

    -- 3. Estudantes oficiais da base de testes reais
    INSERT INTO students (
        id, academic_class_id, student_number, full_name, document_type, document_number,
        email, phone, whatsapp, status, financially_blocked, blocked_reason, created_at, updated_at
    ) VALUES
    (
        '44444444-4444-4444-4444-444444444401',
        v_class_id,
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
        v_class_id,
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
        v_class_id,
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
        id, student_id, charge_code, description, reference_month, due_date,
        amount, fine_amount, interest_amount, discount_amount, total_amount,
        currency, status, paid_at, cancelled_at, created_at, updated_at
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
END $$;

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
