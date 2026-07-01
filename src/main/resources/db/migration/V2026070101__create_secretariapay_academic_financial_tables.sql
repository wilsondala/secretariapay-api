-- SecretáriaPay Académico - Núcleo académico-financeiro

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

CREATE UNIQUE INDEX IF NOT EXISTS uk_institutions_nif ON institutions (nif) WHERE nif IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_institutions_active ON institutions (active);

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
    CONSTRAINT fk_courses_institution FOREIGN KEY (institution_id) REFERENCES institutions (id)
);

CREATE INDEX IF NOT EXISTS idx_courses_institution_id ON courses (institution_id);
CREATE INDEX IF NOT EXISTS idx_courses_active ON courses (active);

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
    CONSTRAINT fk_academic_classes_course FOREIGN KEY (course_id) REFERENCES courses (id)
);

CREATE INDEX IF NOT EXISTS idx_academic_classes_course_id ON academic_classes (course_id);
CREATE INDEX IF NOT EXISTS idx_academic_classes_academic_year ON academic_classes (academic_year);
CREATE INDEX IF NOT EXISTS idx_academic_classes_active ON academic_classes (active);

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
    CONSTRAINT fk_students_academic_class FOREIGN KEY (academic_class_id) REFERENCES academic_classes (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_students_student_number ON students (student_number);
CREATE INDEX IF NOT EXISTS idx_students_academic_class_id ON students (academic_class_id);
CREATE INDEX IF NOT EXISTS idx_students_status ON students (status);
CREATE INDEX IF NOT EXISTS idx_students_financially_blocked ON students (financially_blocked);

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
    CONSTRAINT fk_charges_student FOREIGN KEY (student_id) REFERENCES students (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_charges_charge_code ON charges (charge_code);
CREATE INDEX IF NOT EXISTS idx_charges_student_id ON charges (student_id);
CREATE INDEX IF NOT EXISTS idx_charges_status ON charges (status);
CREATE INDEX IF NOT EXISTS idx_charges_due_date ON charges (due_date);

CREATE TABLE IF NOT EXISTS payment_proofs (
    id UUID PRIMARY KEY,
    charge_id UUID NOT NULL,
    file_url TEXT NOT NULL,
    file_name VARCHAR(180),
    mime_type VARCHAR(120),
    submitted_by_phone VARCHAR(40),
    submitted_at TIMESTAMP NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING_REVIEW',
    reviewed_by_user_id UUID,
    review_note TEXT,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_payment_proofs_charge FOREIGN KEY (charge_id) REFERENCES charges (id),
    CONSTRAINT fk_payment_proofs_reviewed_by_user FOREIGN KEY (reviewed_by_user_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_payment_proofs_charge_id ON payment_proofs (charge_id);
CREATE INDEX IF NOT EXISTS idx_payment_proofs_status ON payment_proofs (status);

CREATE TABLE IF NOT EXISTS receipts (
    id UUID PRIMARY KEY,
    charge_id UUID NOT NULL UNIQUE,
    receipt_code VARCHAR(80) NOT NULL,
    pdf_url TEXT,
    qr_code_url TEXT,
    validation_url TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'VALID',
    issued_at TIMESTAMP NOT NULL,
    cancelled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_receipts_charge FOREIGN KEY (charge_id) REFERENCES charges (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_receipts_receipt_code ON receipts (receipt_code);
CREATE INDEX IF NOT EXISTS idx_receipts_status ON receipts (status);

CREATE TABLE IF NOT EXISTS academic_blocks (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL,
    charge_id UUID,
    blocked_service VARCHAR(120) NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    blocked_by_user_id UUID,
    blocked_at TIMESTAMP NOT NULL,
    released_by_user_id UUID,
    released_at TIMESTAMP,
    release_note TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_academic_blocks_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_academic_blocks_charge FOREIGN KEY (charge_id) REFERENCES charges (id),
    CONSTRAINT fk_academic_blocks_blocked_by_user FOREIGN KEY (blocked_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_academic_blocks_released_by_user FOREIGN KEY (released_by_user_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_academic_blocks_student_id ON academic_blocks (student_id);
CREATE INDEX IF NOT EXISTS idx_academic_blocks_charge_id ON academic_blocks (charge_id);
CREATE INDEX IF NOT EXISTS idx_academic_blocks_status ON academic_blocks (status);
