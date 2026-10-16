-- Deploy survey-service without the URL fields before running this migration.
-- Refuse to discard any nonempty URL that is not already represented in media_files.
BEGIN;
DO $$
DECLARE
    owner_table text;
    media_kind text;
    url_column text;
    present_count integer;
    unmatched_count bigint;
BEGIN
    SELECT count(*) INTO present_count
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name IN ('surveys', 'pages', 'questions', 'options')
      AND column_name IN ('image_url', 'video_url', 'audio_url');

    IF present_count = 0 THEN
        RAISE NOTICE 'Media URL columns already removed';
        RETURN;
    END IF;
    IF present_count <> 12 THEN
        RAISE EXCEPTION 'Expected 12 media URL columns, found %; migration aborted', present_count;
    END IF;

    FOREACH owner_table IN ARRAY ARRAY['surveys', 'pages', 'questions', 'options'] LOOP
        FOREACH media_kind IN ARRAY ARRAY['image', 'video', 'audio'] LOOP
            url_column := media_kind || '_url';
            EXECUTE format(
                'SELECT count(*) FROM public.%I t
                 WHERE nullif(btrim(t.%I), '''') IS NOT NULL
                   AND NOT EXISTS (
                     SELECT 1 FROM public.media_files m
                     WHERE m.owner_type = %L
                       AND m.owner_id = t.id
                       AND m.media_type = %L
                       AND m.object_url = t.%I
                   )',
                owner_table, url_column,
                upper(left(owner_table, length(owner_table) - 1)),
                upper(media_kind), url_column
            ) INTO unmatched_count;
            IF unmatched_count > 0 THEN
                RAISE EXCEPTION '%.% has % URL(s) absent from media_files; migration aborted',
                    owner_table, url_column, unmatched_count;
            END IF;
        END LOOP;
    END LOOP;

    FOREACH owner_table IN ARRAY ARRAY['surveys', 'pages', 'questions', 'options'] LOOP
        EXECUTE format(
            'ALTER TABLE public.%I DROP COLUMN image_url, DROP COLUMN video_url, DROP COLUMN audio_url',
            owner_table
        );
    END LOOP;
END $$;
COMMIT;
