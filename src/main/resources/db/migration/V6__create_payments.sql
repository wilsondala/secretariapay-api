CREATE TABLE payments (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    payment_code VARCHAR(50) NOT NULL UNIQUE,
    method VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    amount NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'BRL',
    pix_copy_paste TEXT,
    pix_qr_code_url TEXT,
    gateway_name VARCHAR(80),
    gateway_transaction_id VARCHAR(120),
    expires_at TIMESTAMP,
    paid_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_payments_booking
        FOREIGN KEY (booking_id)
        REFERENCES bookings(id)
);

CREATE INDEX idx_payments_booking_id ON payments(booking_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_method ON payments(method);
CREATE INDEX idx_payments_payment_code ON payments(payment_code);