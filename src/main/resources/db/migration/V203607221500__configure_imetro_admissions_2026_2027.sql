-- SecretáriaPay Académico / IMETRO
-- Primeira fase oficial de produção: inscrições, matrículas e rematrículas 2026/2027.
-- Dados institucionais fornecidos pela Secretaria-Geral do IMETRO.

INSERT INTO institutions (
    id, name, legal_name, email, phone, whatsapp, address, active, created_at, updated_at
) VALUES (
    'c3726494-46b5-4563-8e84-0a04334fac8c'::uuid,
    'Instituto Superior Politécnico Metropolitano de Angola',
    'Instituto Superior Politécnico Metropolitano de Angola (IMETRO)',
    'info.academico@imetroangola.com',
    '+244 929 179 683',
    '+244 929 179 683',
    'Campus da Metropolitana, Avenida 21 de Janeiro, Travessa da Talatona S/N, Morro Bento, Luanda',
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    legal_name = EXCLUDED.legal_name,
    email = EXCLUDED.email,
    phone = EXCLUDED.phone,
    whatsapp = EXCLUDED.whatsapp,
    address = EXCLUDED.address,
    active = TRUE,
    updated_at = NOW();

INSERT INTO institution_settings (
    id, institution_id, public_slug, official_whatsapp, support_email,
    timezone, country, currency, subscription_plan, subscription_status,
    allow_academic_blocking, auto_unblock_after_payment,
    payment_grace_days, monthly_due_day, active, created_at, updated_at
) VALUES (
    'c3726494-46b5-4563-8e84-0a04334fac8d'::uuid,
    'c3726494-46b5-4563-8e84-0a04334fac8c'::uuid,
    'imetro',
    '+244 929 179 683',
    'info.academico@imetroangola.com',
    'Africa/Luanda',
    'AO',
    'AOA',
    'PILOT',
    'TRIAL',
    FALSE,
    TRUE,
    5,
    10,
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (institution_id) DO UPDATE SET
    public_slug = EXCLUDED.public_slug,
    official_whatsapp = EXCLUDED.official_whatsapp,
    support_email = EXCLUDED.support_email,
    timezone = EXCLUDED.timezone,
    country = EXCLUDED.country,
    currency = EXCLUDED.currency,
    active = TRUE,
    updated_at = NOW();

WITH official_courses(id, code, name, department_code) AS (
    VALUES
        ('11111111-1004-4000-8000-000000000004'::uuid, 'ARQ', 'Arquitectura', 'DTEC'),
        ('11111111-1005-4000-8000-000000000005'::uuid, 'ENG-CIVIL', 'Engenharia Civil', 'DTEC'),
        ('11111111-1001-4000-8000-000000000001'::uuid, 'ELEC-TELECOM', 'Engenharia Electrónica e Telecomunicações', 'DTEC'),
        ('11111111-1006-4000-8000-000000000006'::uuid, 'GEO-MINAS', 'Engenharia de Geologia e Minas', 'DTEC'),
        ('11111111-1013-4000-8000-000000000013'::uuid, 'ADM-EMP', 'Administração de Empresas', 'DCEG'),
        ('11111111-1010-4000-8000-000000000010'::uuid, 'ECO', 'Economia', 'DCEG'),
        ('11111111-1012-4000-8000-000000000012'::uuid, 'GPUB', 'Gestão Pública', 'DCEG'),
        ('11111111-1011-4000-8000-000000000011'::uuid, 'GRH', 'Gestão de Recursos Humanos', 'DCEG')
)
UPDATE courses c
SET
    name = oc.name,
    faculty = oc.department_code,
    active = TRUE,
    updated_at = NOW()
FROM official_courses oc
WHERE c.institution_id = 'c3726494-46b5-4563-8e84-0a04334fac8c'::uuid
  AND UPPER(c.code) = UPPER(oc.code);

WITH official_courses(id, code, name, department_code) AS (
    VALUES
        ('11111111-1004-4000-8000-000000000004'::uuid, 'ARQ', 'Arquitectura', 'DTEC'),
        ('11111111-1005-4000-8000-000000000005'::uuid, 'ENG-CIVIL', 'Engenharia Civil', 'DTEC'),
        ('11111111-1001-4000-8000-000000000001'::uuid, 'ELEC-TELECOM', 'Engenharia Electrónica e Telecomunicações', 'DTEC'),
        ('11111111-1006-4000-8000-000000000006'::uuid, 'GEO-MINAS', 'Engenharia de Geologia e Minas', 'DTEC'),
        ('11111111-1013-4000-8000-000000000013'::uuid, 'ADM-EMP', 'Administração de Empresas', 'DCEG'),
        ('11111111-1010-4000-8000-000000000010'::uuid, 'ECO', 'Economia', 'DCEG'),
        ('11111111-1012-4000-8000-000000000012'::uuid, 'GPUB', 'Gestão Pública', 'DCEG'),
        ('11111111-1011-4000-8000-000000000011'::uuid, 'GRH', 'Gestão de Recursos Humanos', 'DCEG')
)
INSERT INTO courses (
    id, institution_id, name, code, faculty, duration_years, active, created_at, updated_at
)
SELECT
    oc.id,
    'c3726494-46b5-4563-8e84-0a04334fac8c'::uuid,
    oc.name,
    oc.code,
    oc.department_code,
    NULL,
    TRUE,
    NOW(),
    NOW()
FROM official_courses oc
WHERE NOT EXISTS (
    SELECT 1
    FROM courses c
    WHERE c.institution_id = 'c3726494-46b5-4563-8e84-0a04334fac8c'::uuid
      AND UPPER(c.code) = UPPER(oc.code)
);

CREATE TABLE IF NOT EXISTS admission_campaigns (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL REFERENCES institutions(id),
    campaign_code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(180) NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    registration_start DATE NOT NULL,
    registration_end DATE NOT NULL,
    registration_fee NUMERIC(14,2) NOT NULL,
    enrollment_fee NUMERIC(14,2) NOT NULL,
    reenrollment_fee NUMERIC(14,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'AOA',
    public_form_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    whatsapp_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_admission_campaign_dates CHECK (registration_end >= registration_start),
    CONSTRAINT ck_admission_campaign_amounts CHECK (
        registration_fee > 0 AND enrollment_fee > 0 AND reenrollment_fee > 0
    ),
    CONSTRAINT uk_admission_campaign_institution_year UNIQUE (institution_id, academic_year)
);

CREATE INDEX IF NOT EXISTS idx_admission_campaigns_institution_active
    ON admission_campaigns(institution_id, active, registration_start, registration_end);

CREATE TABLE IF NOT EXISTS admission_course_offerings (
    id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL REFERENCES admission_campaigns(id) ON DELETE CASCADE,
    course_id UUID NOT NULL REFERENCES courses(id),
    department_code VARCHAR(20) NOT NULL,
    shift VARCHAR(20) NOT NULL,
    decree_reference VARCHAR(220),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_admission_course_offerings_department CHECK (department_code IN ('DTEC','DCEG')),
    CONSTRAINT ck_admission_course_offerings_shift CHECK (shift IN ('MANHA','TARDE','NOITE')),
    CONSTRAINT uk_admission_course_offering UNIQUE (campaign_id, course_id, shift)
);

CREATE INDEX IF NOT EXISTS idx_admission_course_offerings_catalog
    ON admission_course_offerings(campaign_id, department_code, course_id, shift, active);

INSERT INTO admission_campaigns (
    id, institution_id, campaign_code, name, academic_year,
    registration_start, registration_end,
    registration_fee, enrollment_fee, reenrollment_fee,
    currency, public_form_enabled, whatsapp_enabled, active,
    created_at, updated_at
) VALUES (
    '22222222-2026-4000-8000-000000002027'::uuid,
    'c3726494-46b5-4563-8e84-0a04334fac8c'::uuid,
    'IMETRO-ADM-2026-2027',
    'Inscrições, Matrículas e Rematrículas 2026/2027',
    '2026/2027',
    DATE '2026-08-10',
    DATE '2026-09-08',
    6500.00,
    23500.00,
    23500.00,
    'AOA',
    TRUE,
    TRUE,
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (campaign_code) DO UPDATE SET
    name = EXCLUDED.name,
    academic_year = EXCLUDED.academic_year,
    registration_start = EXCLUDED.registration_start,
    registration_end = EXCLUDED.registration_end,
    registration_fee = EXCLUDED.registration_fee,
    enrollment_fee = EXCLUDED.enrollment_fee,
    reenrollment_fee = EXCLUDED.reenrollment_fee,
    currency = EXCLUDED.currency,
    public_form_enabled = TRUE,
    whatsapp_enabled = TRUE,
    active = TRUE,
    updated_at = NOW();

WITH official_courses(code, department_code, decree_reference) AS (
    VALUES
        ('ARQ', 'DTEC', 'Decreto Executivo n.º 39/12, de 02 de Fevereiro'),
        ('ENG-CIVIL', 'DTEC', 'Decreto Executivo n.º 39/12, de 02 de Fevereiro'),
        ('ELEC-TELECOM', 'DTEC', 'Decreto Executivo n.º 39/12, de 02 de Fevereiro'),
        ('GEO-MINAS', 'DTEC', 'Decreto Executivo n.º 219/17, de 12 de Abril'),
        ('ADM-EMP', 'DCEG', 'Decreto Executivo n.º 39/12, de 02 de Fevereiro'),
        ('ECO', 'DCEG', 'Decreto Executivo n.º 39/12, de 02 de Fevereiro'),
        ('GPUB', 'DCEG', 'Decreto Executivo n.º 39/12, de 02 de Fevereiro'),
        ('GRH', 'DCEG', 'Decreto Executivo n.º 39/12, de 02 de Fevereiro')
), shifts(shift) AS (
    VALUES ('MANHA'), ('TARDE'), ('NOITE')
)
INSERT INTO admission_course_offerings (
    id, campaign_id, course_id, department_code, shift,
    decree_reference, active, created_at, updated_at
)
SELECT
    md5('IMETRO-ADM-2026-2027-' || c.code || '-' || s.shift)::uuid,
    '22222222-2026-4000-8000-000000002027'::uuid,
    c.id,
    oc.department_code,
    s.shift,
    oc.decree_reference,
    TRUE,
    NOW(),
    NOW()
FROM official_courses oc
JOIN courses c
  ON c.institution_id = 'c3726494-46b5-4563-8e84-0a04334fac8c'::uuid
 AND UPPER(c.code) = UPPER(oc.code)
CROSS JOIN shifts s
ON CONFLICT (campaign_id, course_id, shift) DO UPDATE SET
    department_code = EXCLUDED.department_code,
    decree_reference = EXCLUDED.decree_reference,
    active = TRUE,
    updated_at = NOW();

ALTER TABLE admission_applications
    ADD COLUMN IF NOT EXISTS campaign_id UUID,
    ADD COLUMN IF NOT EXISTS source_channel VARCHAR(30) NOT NULL DEFAULT 'INTERNAL';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_admission_applications_campaign'
    ) THEN
        ALTER TABLE admission_applications
            ADD CONSTRAINT fk_admission_applications_campaign
            FOREIGN KEY (campaign_id) REFERENCES admission_campaigns(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_admission_applications_source_channel'
    ) THEN
        ALTER TABLE admission_applications
            ADD CONSTRAINT ck_admission_applications_source_channel
            CHECK (source_channel IN ('FORM','WHATSAPP','INTERNAL','IMPORT'));
    END IF;
END $$;

UPDATE admission_applications
SET campaign_id = '22222222-2026-4000-8000-000000002027'::uuid
WHERE institution_id = 'c3726494-46b5-4563-8e84-0a04334fac8c'::uuid
  AND academic_year = '2026/2027'
  AND campaign_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_admission_applications_campaign_status
    ON admission_applications(campaign_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_admission_applications_identity_year
    ON admission_applications(institution_id, academic_year, document_number);
