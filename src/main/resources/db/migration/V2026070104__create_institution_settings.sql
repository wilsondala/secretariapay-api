CREATE TABLE IF NOT EXISTS institution_settings (
    id UUID PRIMARY KEY,
    institution_id UUID NOT NULL UNIQUE,
    public_slug VARCHAR(120) NOT NULL UNIQUE,
    official_whatsapp VARCHAR(40),
    support_email VARCHAR(180),
    timezone VARCHAR(80) NOT NULL DEFAULT 'Africa/Luanda',
    country VARCHAR(5) NOT NULL DEFAULT 'AO',
    currency VARCHAR(10) NOT NULL DEFAULT 'AOA',
    subscription_plan VARCHAR(30) NOT NULL DEFAULT 'PILOT',
    subscription_status VARCHAR(30) NOT NULL DEFAULT 'TRIAL',
    academic_portal_base_url TEXT,
    allow_academic_blocking BOOLEAN NOT NULL DEFAULT FALSE,
    auto_unblock_after_payment BOOLEAN NOT NULL DEFAULT TRUE,
    payment_grace_days INTEGER NOT NULL DEFAULT 5,
    monthly_due_day INTEGER NOT NULL DEFAULT 10,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_institution_settings_institution
        FOREIGN KEY (institution_id)
        REFERENCES institutions(id)
);

CREATE INDEX IF NOT EXISTS idx_institution_settings_public_slug
    ON institution_settings(public_slug);

CREATE INDEX IF NOT EXISTS idx_institution_settings_subscription_status
    ON institution_settings(subscription_status);

CREATE INDEX IF NOT EXISTS idx_institution_settings_active
    ON institution_settings(active);
