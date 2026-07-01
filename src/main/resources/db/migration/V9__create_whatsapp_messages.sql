CREATE TABLE whatsapp_messages (
    id UUID PRIMARY KEY,
    booking_id UUID,
    ticket_id UUID,
    message_type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    to_phone VARCHAR(40) NOT NULL,
    passenger_name VARCHAR(160),
    reference_code VARCHAR(80),
    message_body TEXT NOT NULL,
    provider_name VARCHAR(80),
    provider_message_id VARCHAR(160),
    error_message TEXT,
    sent_at TIMESTAMP,
    failed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_whatsapp_messages_booking
        FOREIGN KEY (booking_id)
        REFERENCES bookings(id),

    CONSTRAINT fk_whatsapp_messages_ticket
        FOREIGN KEY (ticket_id)
        REFERENCES tickets(id)
);

CREATE INDEX idx_whatsapp_messages_booking_id ON whatsapp_messages(booking_id);
CREATE INDEX idx_whatsapp_messages_ticket_id ON whatsapp_messages(ticket_id);
CREATE INDEX idx_whatsapp_messages_status ON whatsapp_messages(status);
CREATE INDEX idx_whatsapp_messages_message_type ON whatsapp_messages(message_type);
CREATE INDEX idx_whatsapp_messages_to_phone ON whatsapp_messages(to_phone);

