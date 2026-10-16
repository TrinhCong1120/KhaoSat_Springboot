-- Run with psql -v ON_ERROR_STOP=1 after a verified pg_dump backup and after stopping writers.
-- Archived tracking must be restored and checked before this script runs.
BEGIN;

DO $$
DECLARE archived_rows bigint;
BEGIN
  IF to_regclass('public.analytics_tracking_archives') IS NOT NULL THEN
    EXECUTE 'select count(*) from public.analytics_tracking_archives' INTO archived_rows;
    IF archived_rows > 0 THEN
      RAISE EXCEPTION 'Tracking archives exist (% manifests); restore and verify them before cleanup', archived_rows;
    END IF;
  END IF;
END $$;

DROP TRIGGER IF EXISTS trg_survey_lifecycle_change ON surveys;
DROP TRIGGER IF EXISTS trg_page_schedule_change ON pages;
DROP FUNCTION IF EXISTS record_survey_lifecycle_change();
DROP FUNCTION IF EXISTS record_page_schedule_change();

DROP TABLE IF EXISTS analytics_forecast_evaluations;
DROP TABLE IF EXISTS analytics_forecast_points;
DROP TABLE IF EXISTS analytics_forecast_runs;
DROP TABLE IF EXISTS analytics_alerts;
DROP TABLE IF EXISTS survey_goals;
DROP TABLE IF EXISTS system_alerts;
DROP TABLE IF EXISTS auth_login_events;
DROP TABLE IF EXISTS survey_lifecycle_history;
DROP TABLE IF EXISTS page_schedule_history;
DROP TABLE IF EXISTS survey_analytics_hourly;
DROP TABLE IF EXISTS survey_analytics_daily;
DROP TABLE IF EXISTS analytics_tracking_archives;

DELETE FROM role_permissions WHERE permission_id IN
  (SELECT id FROM permissions WHERE code = 'system_analytics_view');
DELETE FROM permissions WHERE code = 'system_analytics_view';
DELETE FROM functions WHERE code = 'system_analytics';
ALTER TABLE users DROP COLUMN IF EXISTS created_at;
DROP INDEX IF EXISTS idx_responses_submitted_at;

COMMIT;
