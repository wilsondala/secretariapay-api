CREATE TABLE IF NOT EXISTS academic_document_requests (
    id UUID PRIMARY KEY,
    document_code VARCHAR(80) NOT NULL UNIQUE,
    student_id UUID NOT NULL REFERENCES students(id),
    service_code VARCHAR(80) NOT NULL,
    document_type VARCHAR(60) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
    purpose VARCHAR(240),
    declaration_text TEXT NOT NULL,
    signatory_name VARCHAR(180) NOT NULL DEFAULT 'Zakeu António Zengo',
    signatory_role VARCHAR(180) NOT NULL DEFAULT 'Presidente da Instituição',
    signature_method VARCHAR(80),
    document_hash VARCHAR(128),
    version_number INTEGER NOT NULL DEFAULT 1,
    demo_mode BOOLEAN NOT NULL DEFAULT TRUE,
    issued_at TIMESTAMP,
    signed_at TIMESTAMP,
    sent_at TIMESTAMP,
    created_by VARCHAR(180),
    signed_by VARCHAR(180),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_academic_document_requests_status CHECK (
        status IN ('DRAFT','READY_FOR_SIGNATURE','SIGNED','SENT','CANCELLED')
    )
);

CREATE INDEX IF NOT EXISTS idx_academic_document_requests_student
    ON academic_document_requests(student_id);

CREATE INDEX IF NOT EXISTS idx_academic_document_requests_status
    ON academic_document_requests(status);

CREATE INDEX IF NOT EXISTS idx_academic_document_requests_type
    ON academic_document_requests(document_type);
