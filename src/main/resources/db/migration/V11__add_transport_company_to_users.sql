ALTER TABLE users
ADD COLUMN IF NOT EXISTS transport_company_id UUID NULL;

ALTER TABLE users
ADD CONSTRAINT fk_users_transport_company
FOREIGN KEY (transport_company_id)
REFERENCES transport_companies (id)
ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_users_transport_company_id
ON users (transport_company_id);