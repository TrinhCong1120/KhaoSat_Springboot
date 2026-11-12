-- =============================================================================
-- Consolidated Database Migrations (Idempotent & Safe to rerun)
-- Gộp toàn bộ các bảng, cột và quyền bổ sung cho PostgreSQL.
-- =============================================================================

BEGIN;

-- 1. USER EXTENSIONS (Telegram & Zalo)
ALTER TABLE users ADD COLUMN IF NOT EXISTS telegram_chat_id BIGINT;
CREATE UNIQUE INDEX IF NOT EXISTS users_telegram_chat_id_unique
    ON users (telegram_chat_id) WHERE telegram_chat_id IS NOT NULL;

ALTER TABLE users ADD COLUMN IF NOT EXISTS zalo_chat_id varchar(100);
CREATE UNIQUE INDEX IF NOT EXISTS users_zalo_chat_id_unique
    ON users (zalo_chat_id) WHERE zalo_chat_id IS NOT NULL;

-- 1.0 USER PROFILE EXTENSIONS (Họ tên, SĐT, Avatar, Tùy chọn thông báo)
ALTER TABLE users ADD COLUMN IF NOT EXISTS full_name varchar(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_number varchar(50);
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url text;
ALTER TABLE users ADD COLUMN IF NOT EXISTS bot_notification_enabled boolean DEFAULT true;


-- 1.1 BOT LINK CODES (Mã liên kết tài khoản tự động)
CREATE TABLE IF NOT EXISTS user_bot_link_codes (
    code varchar(16) PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    channel varchar(16) NOT NULL CHECK (channel IN ('TELEGRAM', 'ZALO')),
    expires_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_user_bot_link_codes_user ON user_bot_link_codes(user_id);

-- 2. NOTIFICATION MANAGEMENT & DELIVERIES
CREATE TABLE IF NOT EXISTS notification_deliveries (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    origin_key varchar(160) UNIQUE,
    source varchar(16) NOT NULL CHECK (source IN ('SYSTEM', 'MANUAL')),
    actor_user_id uuid,
    actor_name varchar(255),
    recipient_user_id uuid,
    recipient_name varchar(255),
    destination varchar(320) NOT NULL,
    channel varchar(16) NOT NULL CHECK (channel IN ('EMAIL', 'TELEGRAM', 'ZALO')),
    subject varchar(500) NOT NULL,
    content text NOT NULL,
    event_type varchar(100),
    reference_id uuid,
    retry_of uuid REFERENCES notification_deliveries(id),
    status varchar(16) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'BOUNCED')),
    attempts integer NOT NULL DEFAULT 0,
    next_attempt_at timestamptz NOT NULL DEFAULT now(),
    lease_until timestamptz,
    error_note varchar(500),
    created_at timestamptz NOT NULL DEFAULT now(),
    sent_at timestamptz,
    bounced_at timestamptz,
    provider_message_id varchar(500)
);

CREATE INDEX IF NOT EXISTS ix_notification_due
    ON notification_deliveries(next_attempt_at, created_at) WHERE status = 'PENDING';
CREATE INDEX IF NOT EXISTS ix_notification_processing
    ON notification_deliveries(lease_until) WHERE status = 'PROCESSING';
CREATE INDEX IF NOT EXISTS ix_notification_history
    ON notification_deliveries(created_at DESC);

ALTER TABLE notification_deliveries ADD COLUMN IF NOT EXISTS bounced_at timestamptz;
ALTER TABLE notification_deliveries ADD COLUMN IF NOT EXISTS provider_message_id varchar(500);

ALTER TABLE notification_deliveries DROP CONSTRAINT IF EXISTS notification_deliveries_status_check;
ALTER TABLE notification_deliveries ADD CONSTRAINT notification_deliveries_status_check
    CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'BOUNCED'));

ALTER TABLE notification_deliveries DROP CONSTRAINT IF EXISTS notification_deliveries_channel_check;
ALTER TABLE notification_deliveries ADD CONSTRAINT notification_deliveries_channel_check
    CHECK (channel IN ('EMAIL', 'TELEGRAM', 'ZALO'));

CREATE TABLE IF NOT EXISTS notification_attempts (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    delivery_id uuid NOT NULL REFERENCES notification_deliveries(id),
    attempt_no integer NOT NULL,
    started_at timestamptz NOT NULL DEFAULT now(),
    finished_at timestamptz,
    status varchar(16) NOT NULL CHECK (status IN ('STARTED', 'SENT', 'FAILED')),
    error_note varchar(500),
    UNIQUE(delivery_id, attempt_no)
);

CREATE TABLE IF NOT EXISTS notification_bounce_cursor (
    mailbox varchar(320) PRIMARY KEY,
    uid_validity bigint NOT NULL,
    last_uid bigint NOT NULL DEFAULT 0,
    updated_at timestamptz NOT NULL DEFAULT now()
);

-- Functions, Permissions & Menus for Notifications
INSERT INTO functions(id, code, name)
SELECT gen_random_uuid(), 'notifications', 'Quản lý thông báo'
WHERE NOT EXISTS (SELECT 1 FROM functions WHERE code = 'notifications');

INSERT INTO permissions(id, code, name, http_method, is_active, function_id, type)
SELECT gen_random_uuid(), p.code, p.name, p.method, true, f.id, p.type
FROM functions f CROSS JOIN (VALUES
    ('notification_view', 'Xem lịch sử thông báo', 'GET', 'VIEW'),
    ('notification_send', 'Gửi và gửi lại thông báo', 'POST', 'CREATE')
) AS p(code, name, method, type)
WHERE f.code = 'notifications'
  AND NOT EXISTS (SELECT 1 FROM permissions existing WHERE existing.code = p.code);

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE lower(r.name) = 'admin'
  AND p.code IN ('notification_view', 'notification_send')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO menus(id, name, icon, path, order_index, created_at, function_id)
SELECT
    gen_random_uuid(),
    'Lịch sử thông báo',
    'Bell',
    '/admin/notifications',
    COALESCE((SELECT MAX(order_index) + 1 FROM menus WHERE parent_id IS NULL), 10),
    now(),
    f.id
FROM functions f
WHERE f.code = 'notifications'
  AND NOT EXISTS (SELECT 1 FROM menus WHERE path = '/admin/notifications');

-- 3. CHAT MESSAGES (Telegram & Zalo)
CREATE TABLE IF NOT EXISTS telegram_chat_messages (
    id bigserial PRIMARY KEY,
    update_id bigint NOT NULL,
    chat_id bigint NOT NULL,
    account_user_id uuid,
    direction varchar(3) NOT NULL CHECK (direction IN ('IN', 'OUT')),
    action varchar(16) NOT NULL,
    content text NOT NULL,
    status varchar(16) NOT NULL,
    error_note varchar(500),
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (update_id, direction)
);
CREATE INDEX IF NOT EXISTS ix_telegram_chat_messages_created
    ON telegram_chat_messages(created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS ix_telegram_chat_messages_chat
    ON telegram_chat_messages(chat_id, created_at DESC);

CREATE TABLE IF NOT EXISTS zalo_chat_messages (
    id bigserial PRIMARY KEY,
    message_key varchar(200) NOT NULL,
    chat_id varchar(100) NOT NULL,
    account_user_id uuid,
    direction varchar(3) NOT NULL CHECK (direction IN ('IN', 'OUT')),
    action varchar(16) NOT NULL,
    content text NOT NULL,
    status varchar(16) NOT NULL,
    error_note varchar(500),
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (message_key, direction)
);
CREATE INDEX IF NOT EXISTS ix_zalo_chat_messages_created
    ON zalo_chat_messages(created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS ix_zalo_chat_messages_chat
    ON zalo_chat_messages(chat_id, created_at DESC);

-- 4. NOTIFICATION INBOX & ATTACHMENTS
CREATE TABLE IF NOT EXISTS notification_inbox_messages (
    id uuid PRIMARY KEY,
    channel varchar(16) NOT NULL CHECK (channel IN ('EMAIL', 'TELEGRAM', 'ZALO')),
    provider_key varchar(400) NOT NULL UNIQUE,
    provider_message_id varchar(500),
    sender_address varchar(500) NOT NULL,
    reply_destination varchar(500),
    recipient_address varchar(500) NOT NULL,
    sender_user_id uuid,
    matched_delivery_id uuid REFERENCES notification_deliveries(id),
    subject varchar(500),
    content text NOT NULL DEFAULT '',
    kind varchar(16) NOT NULL DEFAULT 'MESSAGE' CHECK (kind IN ('MESSAGE', 'BOUNCE', 'COMMAND')),
    handling_status varchar(16) NOT NULL DEFAULT 'NEW' CHECK (handling_status IN ('NEW', 'RESOLVED')),
    received_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS notification_inbox_received_idx ON notification_inbox_messages(received_at DESC);
CREATE INDEX IF NOT EXISTS notification_inbox_match_idx ON notification_inbox_messages(matched_delivery_id);

CREATE TABLE IF NOT EXISTS notification_inbox_attachments (
    id uuid PRIMARY KEY,
    inbox_id uuid NOT NULL REFERENCES notification_inbox_messages(id) ON DELETE CASCADE,
    attachment_key varchar(100),
    file_name varchar(500) NOT NULL,
    content_type varchar(200),
    size_bytes bigint,
    storage_key varchar(500),
    status varchar(16) NOT NULL CHECK (status IN ('STORED', 'OVERSIZE', 'FAILED')),
    error_note varchar(500),
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS notification_inbox_attachment_idx ON notification_inbox_attachments(inbox_id);
CREATE UNIQUE INDEX IF NOT EXISTS notification_inbox_attachment_key_idx
    ON notification_inbox_attachments(inbox_id, attachment_key) WHERE attachment_key IS NOT NULL;

ALTER TABLE notification_deliveries ADD COLUMN IF NOT EXISTS in_reply_to_inbound_id uuid REFERENCES notification_inbox_messages(id);
CREATE INDEX IF NOT EXISTS notification_delivery_provider_idx
    ON notification_deliveries(channel, destination, provider_message_id)
    WHERE provider_message_id IS NOT NULL;

-- 5. SURVEY GROUPS & ACCESS
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

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'survey_access') THEN
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
  END IF;
END $$;

ALTER TABLE surveys ALTER COLUMN group_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS ix_surveys_group_id ON surveys(group_id);

DELETE FROM role_permissions rp USING roles r, permissions p
WHERE rp.role_id=r.id AND rp.permission_id=p.id
  AND lower(r.name)='manager' AND p.code='survey_delete_all';

-- 6. FAILED SURVEY CASES
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

-- 7. SURVEY DASHBOARD REPORTING
INSERT INTO question_types (code,name) VALUES
  ('ADDRESS','Địa chỉ'),
  ('DATE','Ngày tháng'),
  ('MULTIPLE_CHOICE','Chọn nhiều đáp án'),
  ('NUMBER','Số'),
  ('SINGLE_CHOICE','Chọn một đáp án'),
  ('TEXT','Văn bản')
ON CONFLICT (code) DO UPDATE SET name=EXCLUDED.name
WHERE question_types.name IS DISTINCT FROM EXCLUDED.name;

CREATE TABLE IF NOT EXISTS site_daily_visitors (
  visit_date date NOT NULL,
  visitor_id uuid NOT NULL,
  first_seen_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (visit_date, visitor_id)
);
CREATE INDEX IF NOT EXISTS idx_site_daily_visitors_first_seen ON site_daily_visitors(first_seen_at);

ALTER TABLE responses ADD COLUMN IF NOT EXISTS applicable_question_count integer;
ALTER TABLE responses ADD COLUMN IF NOT EXISTS answered_question_count integer;
ALTER TABLE survey_sessions ALTER COLUMN active_duration_ms DROP NOT NULL;
ALTER TABLE survey_sessions ALTER COLUMN active_duration_ms DROP DEFAULT;
ALTER TABLE responses DROP CONSTRAINT IF EXISTS chk_response_question_counts;
ALTER TABLE responses ADD CONSTRAINT chk_response_question_counts CHECK (
  applicable_question_count IS NULL AND answered_question_count IS NULL OR
  applicable_question_count >= 0 AND answered_question_count >= 0
  AND answered_question_count <= applicable_question_count
);
CREATE INDEX IF NOT EXISTS idx_survey_sessions_first_viewed ON survey_sessions(first_viewed_at, survey_id);
CREATE INDEX IF NOT EXISTS idx_survey_events_viewed ON survey_events(received_at, survey_id)
  WHERE event_type = 'SURVEY_VIEWED' AND event_source = 'CLIENT';

-- 8. GRANT DASHBOARD_VIEW TO USER ROLE
INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE lower(r.name) = 'user' AND p.code = 'dashboard_view'
ON CONFLICT DO NOTHING;

-- 9. GOOGLE FORMS INTEGRATION
CREATE TABLE IF NOT EXISTS google_connections (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    google_subject varchar(255) NOT NULL,
    google_email varchar(255) NOT NULL,
    refresh_token_encrypted text NOT NULL,
    granted_scopes text,
    status varchar(32) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'REVOKED', 'NEEDS_REAUTH')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_google_connections_user_email UNIQUE (user_id, google_email)
);
CREATE INDEX IF NOT EXISTS ix_google_connections_user ON google_connections(user_id);

CREATE TABLE IF NOT EXISTS survey_integrations (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    survey_id uuid NOT NULL UNIQUE REFERENCES surveys(id) ON DELETE CASCADE,
    provider varchar(32) NOT NULL DEFAULT 'GOOGLE_FORMS' CHECK (provider IN ('GOOGLE_FORMS')),
    google_form_id varchar(255) NOT NULL,
    owner_connection_id uuid REFERENCES google_connections(id) ON DELETE SET NULL,
    edit_url text NOT NULL,
    response_url text NOT NULL,
    spreadsheet_id varchar(255),
    sync_status varchar(32) NOT NULL DEFAULT 'IDLE' CHECK (sync_status IN ('IDLE', 'SYNCING', 'SUCCESS', 'FAILED')),
    last_synced_at timestamptz,
    last_sync_error text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_survey_integrations_form ON survey_integrations(google_form_id);

CREATE TABLE IF NOT EXISTS google_question_mappings (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    survey_id uuid NOT NULL REFERENCES surveys(id) ON DELETE CASCADE,
    internal_question_id uuid NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    google_form_id varchar(255) NOT NULL,
    google_item_id varchar(255) NOT NULL,
    google_question_id varchar(255) NOT NULL,
    question_type varchar(64) NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_google_question_mappings_survey_q UNIQUE (survey_id, google_question_id)
);
CREATE INDEX IF NOT EXISTS ix_google_question_mappings_survey ON google_question_mappings(survey_id);

CREATE TABLE IF NOT EXISTS external_survey_responses (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    survey_id uuid NOT NULL REFERENCES surveys(id) ON DELETE CASCADE,
    google_form_id varchar(255) NOT NULL,
    google_response_id varchar(255) NOT NULL,
    submitted_at timestamptz NOT NULL,
    answers_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    internal_response_id uuid REFERENCES responses(id) ON DELETE SET NULL,
    sync_status varchar(32) NOT NULL DEFAULT 'SYNCED' CHECK (sync_status IN ('SYNCED', 'FAILED')),
    synced_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_external_survey_responses_form_resp UNIQUE (google_form_id, google_response_id)
);
CREATE INDEX IF NOT EXISTS ix_external_survey_responses_survey ON external_survey_responses(survey_id);

CREATE TABLE IF NOT EXISTS google_form_access_grants (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    survey_integration_id uuid NOT NULL REFERENCES survey_integrations(id) ON DELETE CASCADE,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    google_email varchar(255) NOT NULL,
    google_permission_id varchar(255),
    role varchar(32) NOT NULL DEFAULT 'writer',
    status varchar(32) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'GRANTED', 'FAILED', 'REVOKED')),
    granted_by_user_id uuid REFERENCES users(id) ON DELETE SET NULL,
    granted_at timestamptz,
    last_error text,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_google_form_access_grants UNIQUE (survey_integration_id, user_id, google_email)
);
CREATE INDEX IF NOT EXISTS ix_google_form_access_grants_survey ON google_form_access_grants(survey_integration_id);

COMMIT;

