-- SecretáriaPay Académico - Histórico de mensagens institucionais

CREATE TABLE IF NOT EXISTS secretariapay_messages (
    id UUID PRIMARY KEY,
    institution_id UUID,
    institution_name VARCHAR(220),
    student_id UUID,
    student_number VARCHAR(60),
    student_name VARCHAR(180),
    charge_id UUID,
    charge_code VARCHAR(80),
    payment_proof_id UUID,
    receipt_id UUID,
    receipt_code VARCHAR(80),
    type VARCHAR(60) NOT NULL,
    channel VARCHAR(40) NOT NULL DEFAULT 'WHATSAPP',
    language VARCHAR(20) DEFAULT 'pt-AO',
    recipient_phone VARCHAR(40),
    message TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'GENERATED',
    provider_message_id VARCHAR(180),
    failure_reason TEXT,
    sent_at TIMESTAMP,
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_secretariapay_messages_institution_id ON secretariapay_messages (institution_id);
CREATE INDEX IF NOT EXISTS idx_secretariapay_messages_student_id ON secretariapay_messages (student_id);
CREATE INDEX IF NOT EXISTS idx_secretariapay_messages_charge_id ON secretariapay_messages (charge_id);
CREATE INDEX IF NOT EXISTS idx_secretariapay_messages_payment_proof_id ON secretariapay_messages (payment_proof_id);
CREATE INDEX IF NOT EXISTS idx_secretariapay_messages_receipt_id ON secretariapay_messages (receipt_id);
CREATE INDEX IF NOT EXISTS idx_secretariapay_messages_status ON secretariapay_messages (status);
CREATE INDEX IF NOT EXISTS idx_secretariapay_messages_type ON secretariapay_messages (type);
CREATE INDEX IF NOT EXISTS idx_secretariapay_messages_created_at ON secretariapay_messages (created_at);
