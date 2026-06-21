ALTER TABLE passengers
    ADD COLUMN IF NOT EXISTS document_type VARCHAR(20);

UPDATE passengers
SET document_type = 'CPF'
WHERE document_type IS NULL;

ALTER TABLE passengers
    ALTER COLUMN document_type SET NOT NULL;