-- Apply before starting a survey-service version that requires share_code.
BEGIN;

ALTER TABLE public.surveys ADD COLUMN IF NOT EXISTS share_code varchar(24);

DO $$
DECLARE
  survey_row record;
  candidate text;
  attempt integer;
BEGIN
  FOR survey_row IN SELECT id FROM public.surveys WHERE share_code IS NULL ORDER BY id LOOP
    attempt := 0;
    LOOP
      candidate := substr(md5(survey_row.id::text || ':' || attempt::text), 1, 8);
      EXIT WHEN NOT EXISTS (SELECT 1 FROM public.surveys WHERE share_code = candidate);
      attempt := attempt + 1;
    END LOOP;
    UPDATE public.surveys SET share_code = candidate WHERE id = survey_row.id;
  END LOOP;
END $$;

ALTER TABLE public.surveys ALTER COLUMN share_code SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_surveys_share_code ON public.surveys(share_code);
ALTER TABLE public.surveys DROP CONSTRAINT IF EXISTS chk_surveys_share_code;
ALTER TABLE public.surveys ADD CONSTRAINT chk_surveys_share_code
  CHECK (share_code ~ '^[A-Za-z0-9]{4,24}$');

COMMIT;
