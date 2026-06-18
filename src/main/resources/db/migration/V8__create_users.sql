CREATE TABLE users (
    id UUID PRIMARY KEY,
    full_name VARCHAR(160) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'ADMIN',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);