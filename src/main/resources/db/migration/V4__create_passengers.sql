CREATE TABLE passengers (
    id UUID PRIMARY KEY,
    full_name VARCHAR(160) NOT NULL,
    document_number VARCHAR(30) NOT NULL UNIQUE,
    email VARCHAR(160),
    phone VARCHAR(40),
    whatsapp VARCHAR(40),
    birth_date DATE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_passengers_full_name ON passengers(full_name);
CREATE INDEX idx_passengers_document_number ON passengers(document_number);
CREATE INDEX idx_passengers_whatsapp ON passengers(whatsapp);

