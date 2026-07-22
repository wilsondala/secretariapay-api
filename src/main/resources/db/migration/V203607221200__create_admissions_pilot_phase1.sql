CREATE TABLE IF NOT EXISTS admission_leads (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL REFERENCES institutions(id),
    desired_course_id UUID REFERENCES courses(id),
    full_name VARCHAR(180) NOT NULL,
    phone VARCHAR(40),
    whatsapp VARCHAR(40),
    email VARCHAR(180),
    document_number VARCHAR(80),
    desired_shift VARCHAR(40),
    province VARCHAR(100),
    municipality VARCHAR(100),
    lead_source VARCHAR(80),
    consent_given BOOLEAN NOT NULL DEFAULT FALSE,
    consent_at TIMESTAMP,
    status VARCHAR(40) NOT NULL DEFAULT 'NEW',
    last_contact_at TIMESTAMP,
    converted_at TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_admission_leads_status CHECK (status IN (
        'NEW','CONTACTED','APPLICATION_STARTED','APPLICATION_SUBMITTED','CONVERTED_TO_APPLICANT',
        'NO_RESPONSE','WITHDREW','NOT_ELIGIBLE','OPTED_OUT'
    ))
);

CREATE INDEX IF NOT EXISTS idx_admission_leads_institution_status
    ON admission_leads(institution_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_admission_leads_whatsapp
    ON admission_leads(institution_id, whatsapp) WHERE whatsapp IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_admission_leads_document
    ON admission_leads(institution_id, document_number) WHERE document_number IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_admission_leads_course_shift
    ON admission_leads(institution_id, desired_course_id, desired_shift);

CREATE TABLE IF NOT EXISTS admission_applications (
    id UUID PRIMARY KEY,
    application_code VARCHAR(80) NOT NULL UNIQUE,
    institution_id UUID NOT NULL REFERENCES institutions(id),
    lead_id UUID REFERENCES admission_leads(id),
    desired_course_id UUID NOT NULL REFERENCES courses(id),
    desired_shift VARCHAR(40) NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    full_name VARCHAR(180) NOT NULL,
    document_type VARCHAR(30),
    document_number VARCHAR(80) NOT NULL,
    birth_date DATE,
    phone VARCHAR(40),
    whatsapp VARCHAR(40),
    email VARCHAR(180),
    previous_school VARCHAR(180),
    province VARCHAR(100),
    municipality VARCHAR(100),
    documents_complete BOOLEAN NOT NULL DEFAULT FALSE,
    terms_accepted BOOLEAN NOT NULL DEFAULT FALSE,
    terms_accepted_at TIMESTAMP,
    status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
    notes TEXT,
    submitted_at TIMESTAMP,
    confirmed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_admission_applications_status CHECK (status IN (
        'DRAFT','SUBMITTED','DOCUMENTATION_PENDING','AWAITING_PAYMENT','PAYMENT_UNDER_REVIEW',
        'PAID','CONFIRMED','REJECTED','CANCELLED','EXPIRED'
    ))
);

CREATE INDEX IF NOT EXISTS idx_admission_applications_institution_status
    ON admission_applications(institution_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_admission_applications_course_shift
    ON admission_applications(institution_id, desired_course_id, desired_shift, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_admission_applications_document
    ON admission_applications(institution_id, document_number);
CREATE INDEX IF NOT EXISTS idx_admission_applications_lead
    ON admission_applications(lead_id) WHERE lead_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS admission_invoices (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL UNIQUE REFERENCES admission_applications(id),
    invoice_code VARCHAR(80) NOT NULL UNIQUE,
    amount NUMERIC(14,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'AOA',
    due_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(80),
    payment_reference VARCHAR(120),
    provider VARCHAR(80),
    external_transaction_id VARCHAR(160),
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_admission_invoices_status CHECK (status IN ('PENDING','UNDER_REVIEW','PAID','CANCELLED','EXPIRED')),
    CONSTRAINT ck_admission_invoices_amount CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_admission_invoices_status_due_date
    ON admission_invoices(status, due_date, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_admission_invoices_payment_reference
    ON admission_invoices(payment_reference) WHERE payment_reference IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_admission_invoices_external_transaction
    ON admission_invoices(external_transaction_id) WHERE external_transaction_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS admission_payment_proofs (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES admission_invoices(id),
    file_url VARCHAR(500) NOT NULL,
    file_name VARCHAR(180),
    mime_type VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_REVIEW',
    reviewed_by VARCHAR(180),
    review_note VARCHAR(500),
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_admission_payment_proofs_status CHECK (status IN ('PENDING_REVIEW','APPROVED','REJECTED'))
);

CREATE INDEX IF NOT EXISTS idx_admission_payment_proofs_invoice
    ON admission_payment_proofs(invoice_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_admission_payment_proofs_status
    ON admission_payment_proofs(status, submitted_at DESC);
