-- Run against the shared PostgreSQL database before deploying the services. Safe to rerun.
BEGIN;

ALTER TABLE surveys ADD COLUMN IF NOT EXISTS creator_user_id uuid;
UPDATE surveys s SET creator_user_id = matches.id
FROM (
  SELECT username, min(id::text)::uuid AS id
  FROM users
  GROUP BY username
  HAVING count(*) = 1
) matches
WHERE s.creator_user_id IS NULL AND s.creator_user = matches.username;

CREATE INDEX IF NOT EXISTS ix_surveys_creator_user_id ON surveys(creator_user_id);
ALTER TABLE surveys DROP CONSTRAINT IF EXISTS fk_surveys_creator_user_id;
ALTER TABLE surveys ADD CONSTRAINT fk_surveys_creator_user_id
  FOREIGN KEY (creator_user_id) REFERENCES users(id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS survey_access (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  survey_id uuid NOT NULL REFERENCES surveys(id) ON DELETE CASCADE,
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  can_view boolean NOT NULL DEFAULT true,
  can_edit boolean NOT NULL DEFAULT false,
  CONSTRAINT uq_survey_access_user UNIQUE (survey_id, user_id),
  CONSTRAINT chk_survey_access_view CHECK (can_view)
);
CREATE INDEX IF NOT EXISTS ix_survey_access_user ON survey_access(user_id, survey_id);

ALTER TABLE permissions ADD COLUMN IF NOT EXISTS type varchar(16);
UPDATE permissions SET type = CASE
  WHEN code LIKE '%_view' OR code LIKE '%_view_all' THEN 'VIEW'
  WHEN code LIKE '%_create' THEN 'CREATE'
  WHEN code LIKE '%_update' OR code LIKE '%_update_all' THEN 'UPDATE'
  WHEN code LIKE '%_delete' THEN 'DELETE'
  ELSE 'OTHER' END
WHERE type IS NULL;
ALTER TABLE permissions ALTER COLUMN type SET NOT NULL;
ALTER TABLE permissions DROP CONSTRAINT IF EXISTS chk_permission_type;
ALTER TABLE permissions ADD CONSTRAINT chk_permission_type
  CHECK (type IN ('VIEW', 'CREATE', 'UPDATE', 'DELETE', 'OTHER'));

INSERT INTO permissions(id, code, name, function_id, http_method, is_active, type)
SELECT gen_random_uuid(), 'survey_view_all', 'Xem tat ca khao sat', p.function_id, 'GET', true, 'VIEW'
FROM permissions p WHERE p.code = 'survey_view'
  AND NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'survey_view_all')
LIMIT 1;
INSERT INTO permissions(id, code, name, function_id, http_method, is_active, type)
SELECT gen_random_uuid(), 'survey_update_all', 'Sua tat ca khao sat', p.function_id, 'PUT', true, 'UPDATE'
FROM permissions p WHERE p.code = 'survey_update'
  AND NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'survey_update_all')
LIMIT 1;
INSERT INTO permissions(id, code, name, function_id, http_method, is_active, type)
SELECT gen_random_uuid(), 'survey_delete_all', 'Xoa tat ca khao sat', p.function_id, 'DELETE', true, 'DELETE'
FROM permissions p WHERE p.code = 'survey_update_all'
  AND NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'survey_delete_all')
LIMIT 1;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE lower(r.name) = 'admin'
  AND p.code IN ('survey_view_all', 'survey_update_all')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE lower(r.name) IN ('admin', 'manager')
  AND p.code = 'survey_delete_all'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT rp_update.role_id, p_delete_all.id
FROM role_permissions rp_update
JOIN permissions p_update ON p_update.id = rp_update.permission_id AND p_update.code = 'survey_update_all'
JOIN role_permissions rp_delete ON rp_delete.role_id = rp_update.role_id
JOIN permissions p_delete ON p_delete.id = rp_delete.permission_id AND p_delete.code = 'survey_delete'
JOIN permissions p_delete_all ON p_delete_all.code = 'survey_delete_all'
ON CONFLICT DO NOTHING;

COMMIT;
