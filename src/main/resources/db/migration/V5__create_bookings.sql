CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL,
    passenger_id UUID NOT NULL,
    seat_number INTEGER NOT NULL,
    booking_code VARCHAR(40) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT',
    amount NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'BRL',
    expires_at TIMESTAMP,
    paid_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_bookings_trip
        FOREIGN KEY (trip_id)
        REFERENCES trips(id),

    CONSTRAINT fk_bookings_passenger
        FOREIGN KEY (passenger_id)
        REFERENCES passengers(id)
);

CREATE INDEX idx_bookings_trip_id ON bookings(trip_id);
CREATE INDEX idx_bookings_passenger_id ON bookings(passenger_id);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_booking_code ON bookings(booking_code);

CREATE UNIQUE INDEX ux_bookings_trip_seat_active
ON bookings(trip_id, seat_number)
WHERE status IN ('PENDING_PAYMENT', 'PAID', 'TICKET_ISSUED');

