-- Apply before deploying the failed-submission case API. Safe to rerun.
BEGIN;

WITH inserted AS (
  INSERT INTO permissions(id, code, name, function_id, http_method, is_active, type)
  SELECT gen_random_uuid(), 'survey_failed_manage', 'Quản lý phản hồi gửi lỗi', f.id, 'POST', true, 'OTHER'
  FROM functions f WHERE f.code = 'survey'
    AND NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'survey_failed_manage')
  RETURNING id
)
INSERT INTO role_permissions(role_id, permission_id)
SELECT rp.role_id, target.id FROM role_permissions rp
JOIN permissions source ON source.id = rp.permission_id AND source.code = 'survey_update_all'
JOIN inserted target ON true
ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS failed_survey_cases (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  file_name varchar(255) NOT NULL UNIQUE,
  original_file_name varchar(255) NOT NULL,
  object_key varchar(255),
  checksum_sha256 char(64) NOT NULL,
  file_size bigint NOT NULL,
  status varchar(20) NOT NULL DEFAULT 'NEW' CHECK (status IN ('NEW','IN_PROGRESS','RESOLVED','DELETED')),
  assigned_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
  created_by_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT chk_failed_case_object CHECK ((status = 'DELETED') = (object_key IS NULL))
);
CREATE INDEX IF NOT EXISTS ix_failed_cases_status_created ON failed_survey_cases(status, created_at DESC);

CREATE TABLE IF NOT EXISTS failed_survey_case_events (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  case_id uuid NOT NULL REFERENCES failed_survey_cases(id) ON DELETE CASCADE,
  actor_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
  action varchar(32) NOT NULL,
  note text,
  details jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_failed_case_events_case ON failed_survey_case_events(case_id, created_at, id);
COMMIT;
