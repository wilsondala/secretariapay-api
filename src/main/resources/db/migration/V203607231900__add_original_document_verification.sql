-- SecretáriaPay Académico / IMETRO
-- A matrícula somente pode avançar depois da conferência presencial dos originais.

ALTER TABLE admission_enrollment_document_reviews
    ADD COLUMN IF NOT EXISTS originals_presented BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS originals_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS originals_verified_by VARCHAR(180),
    ADD COLUMN IF NOT EXISTS originals_verified_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS originals_verification_notes TEXT;

ALTER TABLE admission_enrollment_document_reviews
    DROP CONSTRAINT IF EXISTS ck_admission_enrollment_originals_verified;

ALTER TABLE admission_enrollment_document_reviews
    ADD CONSTRAINT ck_admission_enrollment_originals_verified
        CHECK (NOT originals_verified OR originals_presented);
