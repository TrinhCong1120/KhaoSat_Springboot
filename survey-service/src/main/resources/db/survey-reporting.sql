-- Apply after schema-uuid.sql for a new database. Survey reporting and observed traffic only.
BEGIN;
DO $$
BEGIN
  IF (SELECT data_type FROM information_schema.columns
      WHERE table_schema = current_schema() AND table_name = 'responses'
        AND column_name = 'submitted_at') = 'timestamp without time zone' THEN
    ALTER TABLE responses ALTER COLUMN submitted_at TYPE timestamptz
      USING submitted_at AT TIME ZONE 'UTC';
  END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_responses_survey_submitted_at
  ON responses (survey_id, submitted_at);

CREATE TABLE IF NOT EXISTS survey_sessions (
  id uuid PRIMARY KEY,
  survey_id uuid NOT NULL REFERENCES surveys(id) ON DELETE CASCADE,
  creation_request_id uuid UNIQUE,
  visitor_id uuid,
  first_viewed_at timestamptz NOT NULL DEFAULT now(),
  started_at timestamptz,
  last_activity_at timestamptz NOT NULL DEFAULT now(),
  completed_at timestamptz,
  expires_at timestamptz,
  status varchar(20) NOT NULL DEFAULT 'VIEWED'
    CHECK (status IN ('VIEWED','STARTED','IN_PROGRESS','COMPLETED','ABANDONED','EXPIRED')),
  active_duration_ms bigint NOT NULL DEFAULT 0 CHECK (active_duration_ms >= 0),
  idle_duration_ms bigint NOT NULL DEFAULT 0 CHECK (idle_duration_ms >= 0),
  device_type varchar(20),
  browser varchar(50),
  os varchar(50),
  language varchar(20),
  source_id uuid,
  utm_source varchar(100),
  utm_medium varchar(100),
  utm_campaign varchar(150),
  referrer_host varchar(255)
);
CREATE INDEX IF NOT EXISTS idx_sessions_survey_viewed ON survey_sessions(survey_id, first_viewed_at);
CREATE INDEX IF NOT EXISTS idx_sessions_survey_status ON survey_sessions(survey_id, status);

CREATE TABLE IF NOT EXISTS survey_events (
  id uuid PRIMARY KEY,
  event_id uuid NOT NULL UNIQUE,
  session_id uuid REFERENCES survey_sessions(id) ON DELETE SET NULL,
  survey_id uuid NOT NULL REFERENCES surveys(id) ON DELETE CASCADE,
  event_type varchar(60) NOT NULL,
  page_id uuid REFERENCES pages(id) ON DELETE SET NULL,
  question_id uuid REFERENCES questions(id) ON DELETE SET NULL,
  client_occurred_at timestamptz,
  received_at timestamptz NOT NULL DEFAULT now(),
  event_source varchar(20) NOT NULL CHECK (event_source IN ('CLIENT','SERVER')),
  metadata jsonb
);
CREATE INDEX IF NOT EXISTS idx_events_survey_received ON survey_events(survey_id, received_at);
CREATE INDEX IF NOT EXISTS idx_events_session_received ON survey_events(session_id, received_at);

CREATE TABLE IF NOT EXISTS survey_page_visits (
  id uuid PRIMARY KEY,
  session_id uuid NOT NULL REFERENCES survey_sessions(id) ON DELETE CASCADE,
  survey_id uuid NOT NULL REFERENCES surveys(id) ON DELETE CASCADE,
  page_id uuid NOT NULL REFERENCES pages(id) ON DELETE CASCADE,
  entered_at timestamptz NOT NULL DEFAULT now(),
  left_at timestamptz,
  completed_at timestamptz,
  active_duration_ms bigint NOT NULL DEFAULT 0 CHECK (active_duration_ms >= 0),
  idle_duration_ms bigint NOT NULL DEFAULT 0 CHECK (idle_duration_ms >= 0),
  exit_reason varchar(30),
  validation_error_count integer NOT NULL DEFAULT 0 CHECK (validation_error_count >= 0)
);
CREATE INDEX IF NOT EXISTS idx_page_visits_page_entered ON survey_page_visits(page_id, entered_at);
CREATE INDEX IF NOT EXISTS idx_page_visits_session_entered ON survey_page_visits(session_id, entered_at);

ALTER TABLE responses ADD COLUMN IF NOT EXISTS session_id uuid;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_responses_session') THEN
    ALTER TABLE responses ADD CONSTRAINT fk_responses_session
      FOREIGN KEY (session_id) REFERENCES survey_sessions(id) ON DELETE SET NULL;
  END IF;
END $$;

-- A successful Response insert is the authoritative submit event, in the same transaction.
CREATE OR REPLACE FUNCTION record_confirmed_response() RETURNS trigger AS $$
BEGIN
  IF NEW.session_id IS NOT NULL THEN
    IF NOT EXISTS (SELECT 1 FROM survey_sessions
        WHERE id=NEW.session_id AND survey_id=NEW.survey_id) THEN
      RAISE EXCEPTION 'Session does not belong to response Survey';
    END IF;
    UPDATE survey_sessions SET status='COMPLETED', completed_at=NEW.submitted_at,
      last_activity_at=NEW.submitted_at WHERE id=NEW.session_id;
  END IF;
  INSERT INTO survey_events(id,event_id,session_id,survey_id,event_type,received_at,event_source)
    VALUES(gen_random_uuid(),gen_random_uuid(),NEW.session_id,NEW.survey_id,
      'SURVEY_SUBMITTED',NEW.submitted_at,'SERVER');
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS trg_response_confirmed ON responses;
CREATE TRIGGER trg_response_confirmed AFTER INSERT ON responses
  FOR EACH ROW EXECUTE FUNCTION record_confirmed_response();

ALTER TABLE survey_sessions ADD COLUMN IF NOT EXISTS abandoned_at timestamptz;
CREATE INDEX IF NOT EXISTS idx_events_survey_view_received_session
  ON survey_events(survey_id, received_at, session_id)
  WHERE event_type = 'SURVEY_VIEWED' AND event_source = 'CLIENT';

CREATE INDEX IF NOT EXISTS idx_responses_survey_session_submitted
  ON responses(survey_id, session_id, submitted_at)
  WHERE session_id IS NOT NULL;

COMMIT;
