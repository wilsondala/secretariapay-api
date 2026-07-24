-- SecretáriaPay Académico / IMETRO
--
-- Callback de compatibilidade para instalações novas.
--
-- O histórico legado usa dois padrões de versão Flyway:
--   V20260710_0001  -> versão 20260710.0001
--   V2026070101     -> versão 2026070101
--
-- Numericamente, 20260710.0001 é executada antes de 2026070101.
-- A migration institucional mais antiga referencia charges e students,
-- enquanto essas tabelas só são criadas depois pela migration académica.
--
-- Este callback executa antes do migrate e cria apenas o núcleo mínimo,
-- de forma idempotente. Em bancos institucionais já existentes, todos os
-- comandos abaixo são no-op por causa de IF NOT EXISTS.

CREATE TABLE IF NOT EXISTS institutions (
    id UUID PRIMARY KEY,
    name VARCHAR(180) NOT NULL,
    legal_name VARCHAR(220),
    nif VARCHAR(40),
    email VARCHAR(180),
    phone VARCHAR(40),
    whatsapp VARCHAR(40),
    address TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_institutions_nif
    ON institutions (nif) WHERE nif IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_institutions_active
    ON institutions (active);

CREATE TABLE IF NOT EXISTS courses (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL,
    name VARCHAR(180) NOT NULL,
    code VARCHAR(80),
    faculty VARCHAR(120),
    duration_years INTEGER,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_courses_institution
        FOREIGN KEY (institution_id) REFERENCES institutions (id)
);

CREATE INDEX IF NOT EXISTS idx_courses_institution_id
    ON courses (institution_id);
CREATE INDEX IF NOT EXISTS idx_courses_active
    ON courses (active);

CREATE TABLE IF NOT EXISTS academic_classes (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    year_level INTEGER,
    shift VARCHAR(30) NOT NULL DEFAULT 'NIGHT',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_academic_classes_course
        FOREIGN KEY (course_id) REFERENCES courses (id)
);

CREATE INDEX IF NOT EXISTS idx_academic_classes_course_id
    ON academic_classes (course_id);
CREATE INDEX IF NOT EXISTS idx_academic_classes_academic_year
    ON academic_classes (academic_year);
CREATE INDEX IF NOT EXISTS idx_academic_classes_active
    ON academic_classes (active);

CREATE TABLE IF NOT EXISTS students (
    id UUID PRIMARY KEY,
    academic_class_id UUID NOT NULL,
    student_number VARCHAR(60) NOT NULL,
    full_name VARCHAR(180) NOT NULL,
    document_type VARCHAR(30),
    document_number VARCHAR(60),
    email VARCHAR(180),
    phone VARCHAR(40),
    whatsapp VARCHAR(40),
    birth_date DATE,
    guardian_name VARCHAR(180),
    guardian_phone VARCHAR(40),
    guardian_email VARCHAR(180),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    financially_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    blocked_reason TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_students_academic_class
        FOREIGN KEY (academic_class_id) REFERENCES academic_classes (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_students_student_number
    ON students (student_number);
CREATE INDEX IF NOT EXISTS idx_students_academic_class_id
    ON students (academic_class_id);
CREATE INDEX IF NOT EXISTS idx_students_status
    ON students (status);
CREATE INDEX IF NOT EXISTS idx_students_financially_blocked
    ON students (financially_blocked);

CREATE TABLE IF NOT EXISTS charges (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL,
    charge_code VARCHAR(60) NOT NULL,
    description VARCHAR(120) NOT NULL,
    reference_month VARCHAR(20),
    due_date DATE NOT NULL,
    amount NUMERIC(14,2) NOT NULL,
    fine_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    interest_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    discount_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(14,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'AOA',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    paid_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_charges_student
        FOREIGN KEY (student_id) REFERENCES students (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_charges_charge_code
    ON charges (charge_code);
CREATE INDEX IF NOT EXISTS idx_charges_student_id
    ON charges (student_id);
CREATE INDEX IF NOT EXISTS idx_charges_status
    ON charges (status);
CREATE INDEX IF NOT EXISTS idx_charges_due_date
    ON charges (due_date);
