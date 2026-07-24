-- SecretáriaPay Académico / IMETRO
-- Outbox auditável para alertas operacionais da primeira fase de admissões.

CREATE TABLE IF NOT EXISTS admission_operational_notifications (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL
        REFERENCES admission_applications(id) ON DELETE CASCADE,
    event_type VARCHAR(80) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    recipient VARCHAR(60) NOT NULL,
    message_body TEXT NOT NULL,
    idempotency_key VARCHAR(220) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    provider_message_id VARCHAR(220),
    last_error TEXT,
    next_attempt_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_admission_operational_notification_idempotency
        UNIQUE (idempotency_key),
    CONSTRAINT ck_admission_operational_notification_channel
        CHECK (channel IN ('WHATSAPP')),
    CONSTRAINT ck_admission_operational_notification_status
        CHECK (status IN ('PENDING','SENT','FAILED','EXHAUSTED')),
    CONSTRAINT ck_admission_operational_notification_attempts
        CHECK (attempts >= 0)
);

CREATE INDEX IF NOT EXISTS idx_admission_operational_notifications_dispatch
    ON admission_operational_notifications(status, next_attempt_at, created_at);

CREATE INDEX IF NOT EXISTS idx_admission_operational_notifications_application
    ON admission_operational_notifications(application_id, event_type, created_at DESC);
