-- Apply to the shared PostgreSQL database before deploying survey-service.
BEGIN;

ALTER TABLE surveys ADD COLUMN IF NOT EXISTS open_at timestamptz;
ALTER TABLE surveys ADD COLUMN IF NOT EXISTS close_at timestamptz;
ALTER TABLE surveys ADD COLUMN IF NOT EXISTS published_at timestamptz;
ALTER TABLE surveys ADD COLUMN IF NOT EXISTS manually_locked boolean NOT NULL DEFAULT false;
ALTER TABLE surveys ADD COLUMN IF NOT EXISTS requires_unlock boolean NOT NULL DEFAULT false;
ALTER TABLE pages ADD COLUMN IF NOT EXISTS open_at timestamptz;
ALTER TABLE pages ADD COLUMN IF NOT EXISTS close_at timestamptz;
ALTER TABLE responses ADD COLUMN IF NOT EXISTS request_hash varchar(64);

UPDATE surveys SET published_at = COALESCE(created_at AT TIME ZONE 'Asia/Ho_Chi_Minh', now())
WHERE is_active IS TRUE AND published_at IS NULL
  AND EXISTS (SELECT 1 FROM pages WHERE pages.survey_id = surveys.id);

-- Stop rather than silently merge potentially different historical submissions.
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM responses GROUP BY request_id HAVING count(*) > 1) THEN
    RAISE EXCEPTION 'Duplicate responses.request_id found; resolve duplicates before applying survey-schedule.sql';
  END IF;
END $$;
CREATE UNIQUE INDEX IF NOT EXISTS uq_responses_request_id ON responses(request_id);

ALTER TABLE surveys DROP CONSTRAINT IF EXISTS chk_surveys_schedule;
ALTER TABLE surveys ADD CONSTRAINT chk_surveys_schedule
  CHECK (open_at IS NULL OR close_at IS NULL OR open_at < close_at);
ALTER TABLE pages DROP CONSTRAINT IF EXISTS chk_pages_schedule;
ALTER TABLE pages ADD CONSTRAINT chk_pages_schedule
  CHECK (open_at IS NULL OR close_at IS NULL OR open_at < close_at);

ALTER TABLE surveys DROP COLUMN IF EXISTS is_active;
COMMIT;
