ALTER TABLE users
    ADD COLUMN IF NOT EXISTS institution_id UUID;

ALTER TABLE users
    ADD CONSTRAINT fk_users_institution
    FOREIGN KEY (institution_id)
    REFERENCES institutions(id);
