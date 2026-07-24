-- SecretáriaPay Académico / IMETRO
-- Checklist oficial dos documentos exigidos para matrícula de novos alunos.

CREATE TABLE IF NOT EXISTS admission_enrollment_document_reviews (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL
        REFERENCES admission_applications(id) ON DELETE CASCADE,
    two_passport_photos BOOLEAN NOT NULL DEFAULT FALSE,
    authenticated_certificate_copy BOOLEAN NOT NULL DEFAULT FALSE,
    identity_document_copy BOOLEAN NOT NULL DEFAULT FALSE,
    studied_abroad BOOLEAN NOT NULL DEFAULT FALSE,
    education_equivalence_copy BOOLEAN NOT NULL DEFAULT FALSE,
    secondary_education_completed BOOLEAN NOT NULL DEFAULT FALSE,
    age_eligible BOOLEAN NOT NULL DEFAULT FALSE,
    documents_complete BOOLEAN NOT NULL DEFAULT FALSE,
    reviewed_by VARCHAR(180) NOT NULL,
    notes TEXT,
    reviewed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_admission_enrollment_document_review_application
        UNIQUE (application_id),
    CONSTRAINT ck_admission_enrollment_equivalence
        CHECK (studied_abroad = FALSE OR education_equivalence_copy = TRUE OR documents_complete = FALSE)
);

CREATE INDEX IF NOT EXISTS idx_admission_enrollment_document_reviews_complete
    ON admission_enrollment_document_reviews(documents_complete, reviewed_at DESC);
