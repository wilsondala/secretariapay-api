CREATE TABLE trips (
    id UUID PRIMARY KEY,
    transport_company_id UUID NOT NULL,
    route_id UUID NOT NULL,
    departure_at TIMESTAMP NOT NULL,
    arrival_at TIMESTAMP NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'BRL',
    total_seats INTEGER NOT NULL,
    available_seats INTEGER NOT NULL,
    bus_plate VARCHAR(30),
    vehicle_description VARCHAR(160),
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_trips_transport_company
        FOREIGN KEY (transport_company_id)
        REFERENCES transport_companies(id),

    CONSTRAINT fk_trips_route
        FOREIGN KEY (route_id)
        REFERENCES routes(id)
);

CREATE INDEX idx_trips_transport_company_id ON trips(transport_company_id);
CREATE INDEX idx_trips_route_id ON trips(route_id);
CREATE INDEX idx_trips_departure_at ON trips(departure_at);
CREATE INDEX idx_trips_status ON trips(status);

