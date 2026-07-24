-- SecretáriaPay Académico / IMETRO
-- Primeira fase oficial: matrículas e rematrículas 2026/2027 com financeiro separado.

CREATE TABLE IF NOT EXISTS academic_enrollment_requests (
    id UUID PRIMARY KEY,
    request_code VARCHAR(80) NOT NULL UNIQUE,
    institution_id UUID NOT NULL REFERENCES institutions(id),
    campaign_id UUID NOT NULL REFERENCES admission_campaigns(id),
    request_type VARCHAR(30) NOT NULL,
    admission_application_id UUID REFERENCES admission_applications(id),
    student_id UUID REFERENCES students(id),
    target_course_id UUID NOT NULL REFERENCES courses(id),
    target_shift VARCHAR(20) NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    target_year_level INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(40) NOT NULL DEFAULT 'AWAITING_PAYMENT',
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_academic_enrollment_request_type
        CHECK (request_type IN ('ENROLLMENT','REENROLLMENT')),
    CONSTRAINT ck_academic_enrollment_request_shift
        CHECK (target_shift IN ('MANHA','TARDE','NOITE')),
    CONSTRAINT ck_academic_enrollment_request_status
        CHECK (status IN ('AWAITING_PAYMENT','PAYMENT_UNDER_REVIEW','COMPLETED','CANCELLED')),
    CONSTRAINT ck_academic_enrollment_request_year_level
        CHECK (target_year_level BETWEEN 1 AND 10),
    CONSTRAINT ck_academic_enrollment_request_origin
        CHECK (
            (request_type = 'ENROLLMENT' AND admission_application_id IS NOT NULL)
            OR
            (request_type = 'REENROLLMENT' AND student_id IS NOT NULL)
        )
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_academic_enrollment_application
    ON academic_enrollment_requests(admission_application_id)
    WHERE admission_application_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_academic_reenrollment_student_year
    ON academic_enrollment_requests(student_id, academic_year)
    WHERE request_type = 'REENROLLMENT' AND status <> 'CANCELLED';

CREATE INDEX IF NOT EXISTS idx_academic_enrollment_requests_dashboard
    ON academic_enrollment_requests(institution_id, request_type, status, academic_year, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_academic_enrollment_requests_course_shift
    ON academic_enrollment_requests(target_course_id, target_shift, academic_year, status);

CREATE TABLE IF NOT EXISTS academic_enrollment_invoices (
    id UUID PRIMARY KEY,
    enrollment_request_id UUID NOT NULL UNIQUE
        REFERENCES academic_enrollment_requests(id) ON DELETE CASCADE,
    invoice_code VARCHAR(80) NOT NULL UNIQUE,
    amount NUMERIC(14,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'AOA',
    due_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(60),
    payment_reference VARCHAR(180),
    provider VARCHAR(80),
    external_transaction_id VARCHAR(180),
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_academic_enrollment_invoice_amount CHECK (amount > 0),
    CONSTRAINT ck_academic_enrollment_invoice_status
        CHECK (status IN ('PENDING','UNDER_REVIEW','PAID','CANCELLED','EXPIRED'))
);

CREATE INDEX IF NOT EXISTS idx_academic_enrollment_invoices_status
    ON academic_enrollment_invoices(status, due_date, created_at DESC);

CREATE TABLE IF NOT EXISTS academic_enrollment_payment_proofs (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES academic_enrollment_invoices(id) ON DELETE CASCADE,
    file_url TEXT NOT NULL,
    file_name VARCHAR(220),
    mime_type VARCHAR(120),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_REVIEW',
    reviewed_by VARCHAR(180),
    review_note TEXT,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_academic_enrollment_proof_status
        CHECK (status IN ('PENDING_REVIEW','APPROVED','REJECTED'))
);

CREATE INDEX IF NOT EXISTS idx_academic_enrollment_proofs_invoice_status
    ON academic_enrollment_payment_proofs(invoice_id, status, created_at DESC);
