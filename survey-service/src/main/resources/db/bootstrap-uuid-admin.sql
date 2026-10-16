-- One-time local Docker administrator bootstrap after UUID conversion.
-- Capture the final ADMIN_CREDENTIALS line securely; it is shown only once.
\set ON_ERROR_STOP on
BEGIN;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM users WHERE username = 'uuid-migration-admin') THEN
        RAISE EXCEPTION 'uuid-migration-admin already exists';
    END IF;
END $$;

CREATE TEMP TABLE _uuid_admin_secret AS
SELECT 'uuid-migration-admin'::text AS username,
       translate(trim(trailing '=' FROM encode(gen_random_bytes(24), 'base64')), '+/', 'AZ') AS password;

INSERT INTO roles(name) VALUES ('UUID_MIGRATION_ADMIN');
INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'UUID_MIGRATION_ADMIN' AND p.is_active IS TRUE;

INSERT INTO users(username, password_hash, is_active)
SELECT username, crypt(password, gen_salt('bf')), TRUE FROM _uuid_admin_secret;
INSERT INTO user_roles(user_id, role_id)
SELECT u.id, r.id FROM users u CROSS JOIN roles r
WHERE u.username = 'uuid-migration-admin' AND r.name = 'UUID_MIGRATION_ADMIN';

SELECT 'ADMIN_CREDENTIALS|' || username || '|' || password FROM _uuid_admin_secret;
COMMIT;
