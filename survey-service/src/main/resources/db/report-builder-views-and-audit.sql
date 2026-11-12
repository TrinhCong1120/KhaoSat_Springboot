BEGIN;

CREATE TABLE IF NOT EXISTS report_views (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    survey_id uuid NOT NULL REFERENCES surveys(id) ON DELETE CASCADE,
    name varchar(200) NOT NULL,
    description text,
    scope varchar(20) NOT NULL DEFAULT 'PRIVATE'
        CHECK (scope IN ('PRIVATE', 'SHARED')),
    definition_json jsonb NOT NULL,
    created_by_user_id uuid NOT NULL,
    is_default boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_report_views_survey_scope
    ON report_views (survey_id, scope);

CREATE INDEX IF NOT EXISTS idx_report_views_survey_owner
    ON report_views (survey_id, created_by_user_id);

CREATE TABLE IF NOT EXISTS report_export_logs (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    survey_id uuid NOT NULL REFERENCES surveys(id) ON DELETE CASCADE,
    user_id uuid NOT NULL,
    export_format varchar(20) NOT NULL
        CHECK (export_format IN ('EXCEL', 'CSV')),
    report_name varchar(200) NOT NULL,
    row_count integer NOT NULL DEFAULT 0 CHECK (row_count >= 0),
    included_pii boolean NOT NULL DEFAULT false,
    definition_json jsonb,
    exported_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_report_export_logs_survey_time
    ON report_export_logs (survey_id, exported_at DESC);

COMMIT;

