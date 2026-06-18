CREATE TABLE transport_companies (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    trade_name VARCHAR(160),
    document_number VARCHAR(30) NOT NULL UNIQUE,
    email VARCHAR(160),
    phone VARCHAR(40),
    whatsapp VARCHAR(40),
    logo_url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);