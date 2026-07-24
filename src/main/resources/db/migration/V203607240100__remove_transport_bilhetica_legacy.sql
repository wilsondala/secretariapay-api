-- Desacopla definitivamente o SecretáriaPay do domínio de transporte/bilhética.
-- As migrations V1 a V12 permanecem imutáveis para preservar o histórico Flyway.

-- Converte perfis antigos antes de remover os valores do código.
UPDATE users
SET role = CASE role
    WHEN 'ADMIN' THEN 'ADMIN_GLOBAL'
    WHEN 'COMPANY_ADMIN' THEN 'OPERADOR_ATENDIMENTO'
    WHEN 'OPERATOR' THEN 'OPERADOR_ATENDIMENTO'
    ELSE role
END
WHERE role IN ('ADMIN', 'COMPANY_ADMIN', 'OPERATOR');

ALTER TABLE users
    ALTER COLUMN role SET DEFAULT 'ADMIN_GLOBAL';

-- Remove o vínculo dos utilizadores com empresas de transporte.
ALTER TABLE users
    DROP CONSTRAINT IF EXISTS fk_users_transport_company;

DROP INDEX IF EXISTS idx_users_transport_company_id;

ALTER TABLE users
    DROP COLUMN IF EXISTS transport_company_id;

-- Mantém as sessões do WhatsApp, agora exclusivamente académicas.
UPDATE whatsapp_sessions
SET session_type = 'SECRETARIAPAY_ACADEMICO'
WHERE session_type IS DISTINCT FROM 'SECRETARIAPAY_ACADEMICO';

UPDATE whatsapp_sessions
SET current_step = 'SECRETARIAPAY_START'
WHERE current_step NOT IN (
    'SECRETARIAPAY_START',
    'SECRETARIAPAY_WAITING_IDENTIFIER',
    'SECRETARIAPAY_STUDENT_FOUND',
    'SECRETARIAPAY_CHARGE_FOUND',
    'SECRETARIAPAY_WAITING_PAYMENT_PROOF',
    'SECRETARIAPAY_WAITING_HUMAN_SUPPORT',
    'SECRETARIAPAY_FINISHED'
);

ALTER TABLE whatsapp_sessions
    DROP CONSTRAINT IF EXISTS fk_whatsapp_sessions_passenger;

ALTER TABLE whatsapp_sessions
    DROP COLUMN IF EXISTS passenger_id;

-- Elimina as estruturas exclusivas de reservas, viagens e bilhetes.
DROP TABLE IF EXISTS whatsapp_messages CASCADE;
DROP TABLE IF EXISTS ticket_audit_logs CASCADE;
DROP TABLE IF EXISTS tickets CASCADE;
DROP TABLE IF EXISTS payments CASCADE;
DROP TABLE IF EXISTS bookings CASCADE;
DROP TABLE IF EXISTS trips CASCADE;
DROP TABLE IF EXISTS passengers CASCADE;
DROP TABLE IF EXISTS routes CASCADE;
DROP TABLE IF EXISTS transport_companies CASCADE;
