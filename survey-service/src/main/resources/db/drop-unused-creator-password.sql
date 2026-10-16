-- Apply only after deploying survey-service without the creator_password mapping.
-- Idempotent and refuses to discard any non-NULL value.
BEGIN;
DO $$
DECLARE
  non_null_count bigint;
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'surveys'
      AND column_name = 'creator_password'
  ) THEN
    EXECUTE 'SELECT count(*) FROM public.surveys WHERE creator_password IS NOT NULL'
      INTO non_null_count;
    IF non_null_count > 0 THEN
      RAISE EXCEPTION 'creator_password contains % values; column not dropped', non_null_count;
    END IF;
    EXECUTE 'ALTER TABLE public.surveys DROP COLUMN creator_password';
  END IF;
END $$;
COMMIT;
