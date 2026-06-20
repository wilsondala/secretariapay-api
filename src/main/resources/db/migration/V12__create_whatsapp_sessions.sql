ALTER TABLE users
ADD COLUMN IF NOT EXISTS whatsapp VARCHAR(40);

ALTER TABLE users
ADD COLUMN IF NOT EXISTS whatsapp_verified BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users
ADD COLUMN IF NOT EXISTS last_whatsapp_login_at TIMESTAMP NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_whatsapp_unique
ON users (whatsapp)
WHERE whatsapp IS NOT NULL;

CREATE TABLE IF NOT EXISTS whatsapp_sessions (
    id UUID PRIMARY KEY,
    phone_number VARCHAR(40) NOT NULL,
    session_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    current_step VARCHAR(80) NOT NULL,
    user_id UUID NULL,
    passenger_id UUID NULL,
    last_message_text TEXT NULL,
    metadata TEXT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_whatsapp_sessions_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE SET NULL,

    CONSTRAINT fk_whatsapp_sessions_passenger
        FOREIGN KEY (passenger_id)
        REFERENCES passengers (id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_whatsapp_sessions_phone_number
ON whatsapp_sessions (phone_number);

CREATE INDEX IF NOT EXISTS idx_whatsapp_sessions_status
ON whatsapp_sessions (status);

CREATE INDEX IF NOT EXISTS idx_whatsapp_sessions_type
ON whatsapp_sessions (session_type);

CREATE INDEX IF NOT EXISTS idx_whatsapp_sessions_expires_at
ON whatsapp_sessions (expires_at);