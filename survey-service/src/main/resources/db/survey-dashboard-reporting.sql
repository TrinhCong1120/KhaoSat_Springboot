-- Apply after survey-reporting.sql. No historical traffic is fabricated.
BEGIN;
-- SQL owns the question-type catalog; the service only reads it.
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
COMMIT;
