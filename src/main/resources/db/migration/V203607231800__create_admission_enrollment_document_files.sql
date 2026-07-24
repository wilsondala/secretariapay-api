-- SecretáriaPay Académico / IMETRO
-- Anexos privados utilizados pela Secretaria/Admissões na validação documental.

CREATE TABLE IF NOT EXISTS admission_enrollment_document_files (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL
        REFERENCES admission_applications(id) ON DELETE CASCADE,
    document_type VARCHAR(60) NOT NULL,
    stored_name VARCHAR(180) NOT NULL,
    original_file_name VARCHAR(180) NOT NULL,
    mime_type VARCHAR(80) NOT NULL,
    file_size BIGINT NOT NULL,
    uploaded_by VARCHAR(180) NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_admission_enrollment_document_file_stored_name UNIQUE (stored_name),
    CONSTRAINT ck_admission_enrollment_document_file_type CHECK (
        document_type IN (
            'PASSPORT_PHOTO',
            'AUTHENTICATED_CERTIFICATE',
            'IDENTITY_DOCUMENT',
            'EDUCATION_EQUIVALENCE'
        )
    ),
    CONSTRAINT ck_admission_enrollment_document_file_size CHECK (file_size > 0)
);

CREATE INDEX IF NOT EXISTS idx_admission_enrollment_document_files_application
    ON admission_enrollment_document_files(application_id, document_type, uploaded_at);
