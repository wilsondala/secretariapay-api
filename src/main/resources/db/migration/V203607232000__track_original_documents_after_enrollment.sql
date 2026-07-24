-- SecretáriaPay Académico / IMETRO
-- A matrícula pode avançar com a documentação digital completa.
-- Os originais permanecem como obrigação posterior, sujeita a bloqueio após o prazo.

ALTER TABLE admission_enrollment_document_reviews
    ADD COLUMN IF NOT EXISTS originals_due_date DATE,
    ADD COLUMN IF NOT EXISTS originals_block_active BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS originals_blocked_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_admission_original_documents_compliance
    ON admission_enrollment_document_reviews(
        originals_verified,
        originals_due_date,
        originals_block_active
    );
