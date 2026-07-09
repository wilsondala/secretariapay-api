-- ==========================================================
-- SecretáriaPay Académico / IMETRO
-- Operação institucional: notificações, auditoria e conciliação
-- ==========================================================

CREATE TABLE IF NOT EXISTS notification_logs (
    id UUID PRIMARY KEY,
    charge_id UUID REFERENCES charges(id) ON DELETE SET NULL,
    student_id UUID REFERENCES students(id) ON DELETE SET NULL,
    notification_type VARCHAR(40) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    business_date DATE NOT NULL,
    message TEXT,
    provider_message_id VARCHAR(180),
    error_message TEXT,
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_notification_once_per_day UNIQUE (charge_id, notification_type, channel, business_date)
);

CREATE INDEX IF NOT EXISTS idx_notification_logs_charge ON notification_logs(charge_id);
CREATE INDEX IF NOT EXISTS idx_notification_logs_student ON notification_logs(student_id);
CREATE INDEX IF NOT EXISTS idx_notification_logs_business_date ON notification_logs(business_date);
CREATE INDEX IF NOT EXISTS idx_notification_logs_status ON notification_logs(status);

CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY,
    actor VARCHAR(120) NOT NULL,
    action VARCHAR(120) NOT NULL,
    entity_type VARCHAR(80),
    entity_id VARCHAR(120),
    details TEXT,
    ip_address VARCHAR(80),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at);

CREATE TABLE IF NOT EXISTS payment_transactions (
    id UUID PRIMARY KEY,
    charge_id UUID REFERENCES charges(id) ON DELETE SET NULL,
    student_id UUID REFERENCES students(id) ON DELETE SET NULL,
    provider VARCHAR(40) NOT NULL,
    provider_transaction_id VARCHAR(160),
    merchant_transaction_id VARCHAR(80),
    payment_method VARCHAR(80),
    amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    currency VARCHAR(10) NOT NULL DEFAULT 'AOA',
    status VARCHAR(40) NOT NULL,
    raw_payload TEXT,
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_payment_provider_transaction UNIQUE (provider, provider_transaction_id)
);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_charge ON payment_transactions(charge_id);
CREATE INDEX IF NOT EXISTS idx_payment_transactions_student ON payment_transactions(student_id);
CREATE INDEX IF NOT EXISTS idx_payment_transactions_status ON payment_transactions(status);
CREATE INDEX IF NOT EXISTS idx_payment_transactions_merchant ON payment_transactions(merchant_transaction_id);
