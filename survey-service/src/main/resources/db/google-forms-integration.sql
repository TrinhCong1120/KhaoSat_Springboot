-- =============================================================================
-- Google Forms Integration Migration (Idempotent & Safe to rerun)
-- =============================================================================

BEGIN;

-- 1. Quản lý kết nối Google của người dùng
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

-- 2. Tích hợp khảo sát nội bộ với Google Form
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

-- 3. Ánh xạ câu hỏi Google Form với câu hỏi nội bộ (hỗ trợ hiển thị kết quả)
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

-- 4. Lưu trữ phản hồi gốc từ Google Forms (Raw & Idempotency)
CREATE TABLE IF NOT EXISTS external_survey_responses (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    survey_id uuid NOT NULL REFERENCES surveys(id) ON DELETE CASCADE,
    google_form_id varchar(255) NOT NULL,
    google_response_id varchar(255) NOT NULL,
    submitted_at timestamptz NOT NULL,
    answers_json text NOT NULL DEFAULT '{}',
    internal_response_id uuid REFERENCES responses(id) ON DELETE SET NULL,
    sync_status varchar(32) NOT NULL DEFAULT 'SYNCED' CHECK (sync_status IN ('SYNCED', 'FAILED')),
    synced_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_external_survey_responses_form_resp UNIQUE (google_form_id, google_response_id)
);
CREATE INDEX IF NOT EXISTS ix_external_survey_responses_survey ON external_survey_responses(survey_id);

-- 5. Lịch sử cấp quyền writer Google Drive
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
