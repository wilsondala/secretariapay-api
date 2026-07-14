ALTER TABLE charges
    ADD COLUMN IF NOT EXISTS charge_category VARCHAR(40),
    ADD COLUMN IF NOT EXISTS service_code VARCHAR(80);

UPDATE charges
SET charge_category = 'TUITION',
    service_code = COALESCE(NULLIF(service_code, ''), 'TUITION')
WHERE charge_category IS NULL
  AND (
      UPPER(COALESCE(charge_code, '')) LIKE 'IMT-PROPINA-%'
      OR LOWER(COALESCE(description, '')) LIKE '%propina%'
      OR LOWER(COALESCE(reference_month, '')) LIKE '%propina%'
  );

UPDATE charges
SET charge_category = 'ACADEMIC_SERVICE',
    service_code = CASE
        WHEN LOWER(COALESCE(description, '') || ' ' || COALESCE(reference_month, '') || ' ' || COALESCE(charge_code, '')) LIKE '%confirm%matric%' THEN 'ENROLLMENT_CONFIRMATION'
        WHEN LOWER(COALESCE(description, '') || ' ' || COALESCE(reference_month, '') || ' ' || COALESCE(charge_code, '')) LIKE '%matric%' THEN 'ENROLLMENT'
        WHEN LOWER(COALESCE(description, '') || ' ' || COALESCE(reference_month, '') || ' ' || COALESCE(charge_code, '')) LIKE '%inscri%' THEN 'REGISTRATION'
        WHEN LOWER(COALESCE(description, '') || ' ' || COALESCE(reference_month, '') || ' ' || COALESCE(charge_code, '')) LIKE '%recurso%' THEN 'RESIT_EXAM'
        WHEN LOWER(COALESCE(description, '') || ' ' || COALESCE(reference_month, '') || ' ' || COALESCE(charge_code, '')) LIKE '%exame especial%' THEN 'SPECIAL_EXAM'
        WHEN LOWER(COALESCE(description, '') || ' ' || COALESCE(reference_month, '') || ' ' || COALESCE(charge_code, '')) LIKE '%declara%com nota%' THEN 'DECLARATION_WITH_GRADES'
        WHEN LOWER(COALESCE(description, '') || ' ' || COALESCE(reference_month, '') || ' ' || COALESCE(charge_code, '')) LIKE '%declara%sem nota%' THEN 'DECLARATION_WITHOUT_GRADES'
        WHEN LOWER(COALESCE(description, '') || ' ' || COALESCE(reference_month, '') || ' ' || COALESCE(charge_code, '')) LIKE '%declara%' THEN 'DECLARATION_WITHOUT_GRADES'
        WHEN LOWER(COALESCE(description, '') || ' ' || COALESCE(reference_month, '') || ' ' || COALESCE(charge_code, '')) LIKE '%certificado%' THEN 'CERTIFICATE'
        WHEN LOWER(COALESCE(description, '') || ' ' || COALESCE(reference_month, '') || ' ' || COALESCE(charge_code, '')) LIKE '%diploma%' THEN 'DIPLOMA'
        ELSE COALESCE(NULLIF(service_code, ''), 'OTHER_SERVICE')
    END
WHERE charge_category IS NULL
  AND (
      UPPER(COALESCE(charge_code, '')) LIKE 'IMT-MATRICULA-%'
      OR UPPER(COALESCE(charge_code, '')) LIKE 'IMT-RECURSO-%'
      OR UPPER(COALESCE(charge_code, '')) LIKE 'IMT-DECLARACAO-%'
      OR UPPER(COALESCE(charge_code, '')) LIKE 'IMT-SERVICO-%'
      OR LOWER(COALESCE(description, '') || ' ' || COALESCE(reference_month, '')) ~ '(matric|inscri|recurso|exame especial|declara|certificado|diploma)'
  );

UPDATE charges
SET charge_category = 'OTHER',
    service_code = COALESCE(NULLIF(service_code, ''), 'OTHER')
WHERE charge_category IS NULL;

ALTER TABLE charges
    ALTER COLUMN charge_category SET DEFAULT 'OTHER',
    ALTER COLUMN charge_category SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_charges_student_category
    ON charges(student_id, charge_category, due_date DESC);

CREATE INDEX IF NOT EXISTS idx_charges_service_code
    ON charges(service_code);
