CREATE TABLE ticket_audit_logs (
    id UUID PRIMARY KEY,
    ticket_id UUID NULL,
    ticket_code VARCHAR(80) NOT NULL,
    action VARCHAR(40) NOT NULL,
    success BOOLEAN NOT NULL,
    message TEXT,
    ticket_status VARCHAR(30),
    booking_status VARCHAR(30),
    ip_address VARCHAR(80),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_ticket_audit_logs_ticket
        FOREIGN KEY (ticket_id)
        REFERENCES tickets (id)
        ON DELETE SET NULL
);

CREATE INDEX idx_ticket_audit_logs_ticket_code
    ON ticket_audit_logs (ticket_code);

CREATE INDEX idx_ticket_audit_logs_action
    ON ticket_audit_logs (action);

CREATE INDEX idx_ticket_audit_logs_created_at
    ON ticket_audit_logs (created_at);