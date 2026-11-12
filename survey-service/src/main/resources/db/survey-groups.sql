-- Apply after survey-access-permissions.sql, before deploying group-aware services.
BEGIN;

CREATE TABLE IF NOT EXISTS survey_groups (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name varchar(255) NOT NULL,
  parent_id uuid REFERENCES survey_groups(id) ON DELETE RESTRICT,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_survey_groups_parent ON survey_groups(parent_id);

CREATE TABLE IF NOT EXISTS survey_group_access (
  group_id uuid NOT NULL REFERENCES survey_groups(id) ON DELETE CASCADE,
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  permission varchar(40) NOT NULL,
  PRIMARY KEY (group_id, user_id, permission),
  CONSTRAINT chk_survey_group_permission CHECK (permission IN (
    'VIEW','CREATE_CHILD','CREATE_SURVEY','EDIT','VIEW_RESPONSES',
    'EXPORT_DATA','MANAGE_PUBLICATION','DELETE','MANAGE_ACCESS','MANAGE_GROUP'))
);
CREATE INDEX IF NOT EXISTS ix_survey_group_access_user ON survey_group_access(user_id, group_id);

ALTER TABLE surveys ADD COLUMN IF NOT EXISTS group_id uuid REFERENCES survey_groups(id) ON DELETE RESTRICT;

-- A private child per creator preserves access without exposing other creators' surveys.
INSERT INTO survey_groups(id, name)
SELECT '00000000-0000-0000-0000-000000000001'::uuid, 'Khảo sát hiện có'
WHERE NOT EXISTS (SELECT 1 FROM survey_groups WHERE id='00000000-0000-0000-0000-000000000001'::uuid);
INSERT INTO survey_groups(id, name, parent_id)
SELECT u.id, 'Khảo sát của ' || u.username, '00000000-0000-0000-0000-000000000001'::uuid
FROM users u WHERE EXISTS (SELECT 1 FROM surveys s WHERE s.creator_user_id=u.id AND s.group_id IS NULL)
ON CONFLICT (id) DO NOTHING;
UPDATE surveys s SET group_id=s.creator_user_id
WHERE s.group_id IS NULL AND s.creator_user_id IS NOT NULL
  AND EXISTS (SELECT 1 FROM survey_groups g WHERE g.id=s.creator_user_id);
UPDATE surveys SET group_id='00000000-0000-0000-0000-000000000001'::uuid WHERE group_id IS NULL;

INSERT INTO survey_group_access(group_id,user_id,permission)
SELECT DISTINCT s.group_id,s.creator_user_id,p.permission
FROM surveys s CROSS JOIN (VALUES
  ('VIEW'),('CREATE_CHILD'),('CREATE_SURVEY'),('EDIT'),('VIEW_RESPONSES'),
  ('EXPORT_DATA'),('MANAGE_PUBLICATION'),('DELETE'),('MANAGE_ACCESS'),('MANAGE_GROUP')
) p(permission)
WHERE s.creator_user_id IS NOT NULL AND s.group_id=s.creator_user_id
ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS survey_access_permissions (
  survey_id uuid NOT NULL REFERENCES surveys(id) ON DELETE CASCADE,
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  permission varchar(40) NOT NULL,
  PRIMARY KEY(survey_id,user_id,permission),
  CONSTRAINT chk_survey_access_permission CHECK (permission IN (
    'VIEW','EDIT','VIEW_RESPONSES','EXPORT_DATA','MANAGE_PUBLICATION','DELETE','MANAGE_ACCESS'))
);
INSERT INTO survey_access_permissions(survey_id,user_id,permission)
SELECT survey_id,user_id,'VIEW' FROM survey_access WHERE can_view IS TRUE ON CONFLICT DO NOTHING;
INSERT INTO survey_access_permissions(survey_id,user_id,permission)
SELECT a.survey_id,a.user_id,p.permission FROM survey_access a
CROSS JOIN (VALUES ('VIEW_RESPONSES'),('EXPORT_DATA')) p(permission)
WHERE a.can_view IS TRUE ON CONFLICT DO NOTHING;
INSERT INTO survey_access_permissions(survey_id,user_id,permission)
SELECT survey_id,user_id,'EDIT' FROM survey_access WHERE can_edit IS TRUE ON CONFLICT DO NOTHING;
INSERT INTO survey_access_permissions(survey_id,user_id,permission)
SELECT survey_id,user_id,'MANAGE_PUBLICATION' FROM survey_access WHERE can_edit IS TRUE ON CONFLICT DO NOTHING;
DELETE FROM survey_access;

ALTER TABLE surveys ALTER COLUMN group_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS ix_surveys_group_id ON surveys(group_id);

-- Managers scoped to groups must not retain a global delete bypass.
DELETE FROM role_permissions rp USING roles r, permissions p
WHERE rp.role_id=r.id AND rp.permission_id=p.id
  AND lower(r.name)='manager' AND p.code='survey_delete_all';
COMMIT;
