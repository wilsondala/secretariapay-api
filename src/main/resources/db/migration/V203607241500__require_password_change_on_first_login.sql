ALTER TABLE users
    ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMP;

UPDATE users
SET must_change_password = TRUE,
    password_changed_at = NULL
WHERE LOWER(email) IN (
    'presidencia@imetroangola.com',
    'info.academico@imetroangola.com',
    'deodosio.kakinda@imetroangola.com',
    'dyabanza.ernesto@imetroangola.com',
    'eugenia.quibambo@imetroangola.com',
    'dalawilson1244@gmail.com'
);
