CREATE TABLE tickets (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL UNIQUE,
    ticket_code VARCHAR(50) NOT NULL UNIQUE,
    qr_code_url TEXT NOT NULL,
    validation_url TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'VALID',
    issued_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_tickets_booking
        FOREIGN KEY (booking_id)
        REFERENCES bookings(id)
);

CREATE INDEX idx_tickets_booking_id ON tickets(booking_id);
CREATE INDEX idx_tickets_ticket_code ON tickets(ticket_code);
CREATE INDEX idx_tickets_status ON tickets(status);