CREATE TABLE routes (
    id UUID PRIMARY KEY,
    origin_city VARCHAR(120) NOT NULL,
    origin_state VARCHAR(80),
    origin_terminal VARCHAR(160),
    destination_city VARCHAR(120) NOT NULL,
    destination_state VARCHAR(80),
    destination_terminal VARCHAR(160),
    distance_km NUMERIC(10, 2),
    estimated_duration_minutes INTEGER,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_routes_origin_city ON routes(origin_city);
CREATE INDEX idx_routes_destination_city ON routes(destination_city);
CREATE INDEX idx_routes_status ON routes(status);