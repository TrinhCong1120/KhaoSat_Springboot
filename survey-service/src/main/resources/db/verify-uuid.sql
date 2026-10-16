\set ON_ERROR_STOP on
DO $$
DECLARE r record; missing_count bigint;
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND (column_name = 'id' OR column_name LIKE '%_id')
          AND data_type <> 'uuid'
    ) THEN RAISE EXCEPTION 'A numeric ID or reference column remains'; END IF;

    IF EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE connamespace = 'public'::regnamespace AND contype = 'f' AND NOT convalidated
    ) THEN RAISE EXCEPTION 'An unvalidated foreign key remains'; END IF;

    IF (SELECT count(*) FROM pg_constraint
        WHERE connamespace = 'public'::regnamespace AND contype = 'f') <> 24 THEN
        RAISE EXCEPTION 'Unexpected foreign-key count';
    END IF;

    FOR r IN SELECT DISTINCT table_name FROM uuid_migration.id_map LOOP
        EXECUTE format(
            'SELECT count(*) FROM uuid_migration.id_map m LEFT JOIN public.%I t ON t.id = m.new_id WHERE m.table_name = %L AND t.id IS NULL',
            r.table_name, r.table_name
        ) INTO missing_count;
        IF missing_count <> 0 THEN
            RAISE EXCEPTION 'Missing mapped rows in %: %', r.table_name, missing_count;
        END IF;
    END LOOP;

    IF EXISTS (
        SELECT 1 FROM public.media_files f
        WHERE NOT EXISTS (
            SELECT 1 FROM public.surveys s WHERE f.owner_type = 'SURVEY' AND s.id = f.owner_id
            UNION ALL SELECT 1 FROM public.pages p WHERE f.owner_type = 'PAGE' AND p.id = f.owner_id
            UNION ALL SELECT 1 FROM public.questions q WHERE f.owner_type = 'QUESTION' AND q.id = f.owner_id
            UNION ALL SELECT 1 FROM public.options o WHERE f.owner_type = 'OPTION' AND o.id = f.owner_id
        )
    ) THEN RAISE EXCEPTION 'Orphaned media owner'; END IF;

    IF EXISTS (
        SELECT 1 FROM public.conditions c
        JOIN public.questions q ON q.id = c.source_question_id
        JOIN public.question_types t ON t.id = q.question_type_id
        CROSS JOIN LATERAL regexp_split_to_table(c.source_value, ',') token(value)
        LEFT JOIN public.options o ON o.id = trim(token.value)::uuid
        WHERE t.code IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE') AND o.id IS NULL
    ) THEN RAISE EXCEPTION 'Condition references an unknown option'; END IF;

    IF EXISTS (SELECT 1 FROM public.media_files WHERE object_url NOT LIKE '%' || object_key)
    THEN RAISE EXCEPTION 'Media URL does not match object key'; END IF;
END $$;

SELECT 'UUID schema and references verified' AS result,
       (SELECT count(*) FROM uuid_migration.id_map) AS migrated_ids,
       (SELECT count(*) FROM public.surveys) AS surveys,
       (SELECT count(*) FROM public.responses) AS responses,
       (SELECT count(*) FROM public.media_files) AS media_files;
