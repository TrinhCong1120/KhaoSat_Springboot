-- PostgreSQL 16. Run once, with application writers stopped, after a verified pg_dump.
-- The id_map schema remains in the converted backup for audit and rollback analysis.
\set ON_ERROR_STOP on
BEGIN;

CREATE SCHEMA uuid_migration;
CREATE TABLE uuid_migration.id_map (
    table_name text NOT NULL,
    old_id bigint NOT NULL,
    new_id uuid NOT NULL DEFAULT gen_random_uuid(),
    PRIMARY KEY (table_name, old_id),
    UNIQUE (table_name, new_id)
);

CREATE TEMP TABLE _uuid_id_tables (table_name text PRIMARY KEY) ON COMMIT DROP;
INSERT INTO _uuid_id_tables VALUES
    ('answer_options'), ('answers'), ('conditions'), ('functions'),
    ('media_files'), ('menus'), ('options'), ('pages'), ('permissions'),
    ('question_types'), ('questions'), ('responses'), ('roles'),
    ('surveys'), ('users');

CREATE TEMP TABLE _uuid_refs (
    table_name text NOT NULL,
    column_name text NOT NULL,
    parent_table text NOT NULL,
    PRIMARY KEY (table_name, column_name)
) ON COMMIT DROP;
INSERT INTO _uuid_refs VALUES
    ('answer_options', 'answer_id', 'answers'),
    ('answer_options', 'option_id', 'options'),
    ('answers', 'question_id', 'questions'),
    ('answers', 'response_id', 'responses'),
    ('conditions', 'source_question_id', 'questions'),
    ('conditions', 'target_question_id', 'questions'),
    ('media_files', 'created_by_user_id', 'users'),
    ('menus', 'function_id', 'functions'),
    ('menus', 'parent_id', 'menus'),
    ('options', 'question_id', 'questions'),
    ('pages', 'survey_id', 'surveys'),
    ('permissions', 'function_id', 'functions'),
    ('question_validation_rule_values', 'question_id', 'questions'),
    ('question_validation_rule_values', 'option_id', 'options'),
    ('question_validation_rules', 'question_id', 'questions'),
    ('questions', 'page_id', 'pages'),
    ('questions', 'question_type_id', 'question_types'),
    ('responses', 'survey_id', 'surveys'),
    ('role_permissions', 'role_id', 'roles'),
    ('role_permissions', 'permission_id', 'permissions'),
    ('user_roles', 'user_id', 'users'),
    ('user_roles', 'role_id', 'roles');

CREATE TEMP TABLE _uuid_constraints ON COMMIT DROP AS
SELECT c.conrelid::regclass::text AS table_name, c.conname, c.contype,
       pg_get_constraintdef(c.oid) AS definition
FROM pg_constraint c
JOIN pg_namespace n ON n.oid = c.connamespace
WHERE n.nspname = 'public' AND c.contype IN ('f', 'p', 'u', 'c');

CREATE TEMP TABLE _uuid_indexes ON COMMIT DROP AS
SELECT i.indexrelid::regclass::text AS index_name,
       pg_get_indexdef(i.indexrelid) AS definition
FROM pg_index i
JOIN pg_class t ON t.oid = i.indrelid
JOIN pg_namespace n ON n.oid = t.relnamespace
LEFT JOIN pg_constraint c ON c.conindid = i.indexrelid
WHERE n.nspname = 'public' AND c.oid IS NULL;

DO $$
DECLARE r record;
BEGIN
    FOR r IN SELECT table_name FROM _uuid_id_tables LOOP
        EXECUTE format('LOCK TABLE public.%I IN ACCESS EXCLUSIVE MODE', r.table_name);
        EXECUTE format('INSERT INTO uuid_migration.id_map(table_name, old_id) SELECT %L, id FROM public.%I', r.table_name, r.table_name);
    END LOOP;

    -- Choice conditions store comma-separated option IDs outside FK columns.
    IF EXISTS (
        SELECT 1 FROM conditions c
        JOIN questions q ON q.id = c.source_question_id
        JOIN question_types t ON t.id = q.question_type_id
        CROSS JOIN LATERAL regexp_split_to_table(c.source_value, ',') AS token(value)
        LEFT JOIN uuid_migration.id_map m ON m.table_name = 'options'
            AND m.old_id = trim(token.value)::bigint
        WHERE t.code IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE') AND m.new_id IS NULL
    ) THEN RAISE EXCEPTION 'A choice condition references an unknown option'; END IF;

    UPDATE conditions c SET source_value = (
        SELECT string_agg(m.new_id::text, ',' ORDER BY token.position)
        FROM regexp_split_to_table(c.source_value, ',') WITH ORDINALITY AS token(value, position)
        JOIN uuid_migration.id_map m ON m.table_name = 'options'
            AND m.old_id = trim(token.value)::bigint
    )
    FROM questions q JOIN question_types t ON t.id = q.question_type_id
    WHERE q.id = c.source_question_id
      AND t.code IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE');

    FOR r IN SELECT table_name, conname FROM _uuid_constraints WHERE contype = 'f' LOOP
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', r.table_name, r.conname);
    END LOOP;
    FOR r IN SELECT table_name, conname FROM _uuid_constraints WHERE contype <> 'f' LOOP
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', r.table_name, r.conname);
    END LOOP;
    FOR r IN SELECT index_name FROM _uuid_indexes LOOP
        EXECUTE format('DROP INDEX %s', r.index_name);
    END LOOP;
END $$;

DO $$
DECLARE r record; unmatched bigint; required boolean;
BEGIN
    FOR r IN SELECT table_name FROM _uuid_id_tables LOOP
        EXECUTE format('ALTER TABLE public.%I ADD COLUMN id_uuid uuid', r.table_name);
        EXECUTE format('UPDATE public.%I t SET id_uuid = m.new_id FROM uuid_migration.id_map m WHERE m.table_name = %L AND m.old_id = t.id', r.table_name, r.table_name);
    END LOOP;

    FOR r IN SELECT * FROM _uuid_refs LOOP
        SELECT c.is_nullable = 'NO' INTO required
        FROM information_schema.columns c
        WHERE c.table_schema = 'public' AND c.table_name = r.table_name
          AND c.column_name = r.column_name;
        EXECUTE format('ALTER TABLE public.%I ADD COLUMN %I uuid', r.table_name, r.column_name || '_uuid');
        EXECUTE format('UPDATE public.%I t SET %I = m.new_id FROM uuid_migration.id_map m WHERE m.table_name = %L AND m.old_id = t.%I', r.table_name, r.column_name || '_uuid', r.parent_table, r.column_name);
        EXECUTE format('SELECT count(*) FROM public.%I WHERE %I IS NOT NULL AND %I IS NULL', r.table_name, r.column_name, r.column_name || '_uuid') INTO unmatched;
        IF unmatched <> 0 THEN RAISE EXCEPTION 'Unmapped references in %.%: %', r.table_name, r.column_name, unmatched; END IF;
        EXECUTE format('ALTER TABLE public.%I DROP COLUMN %I', r.table_name, r.column_name);
        EXECUTE format('ALTER TABLE public.%I RENAME COLUMN %I TO %I', r.table_name, r.column_name || '_uuid', r.column_name);
        IF required THEN EXECUTE format('ALTER TABLE public.%I ALTER COLUMN %I SET NOT NULL', r.table_name, r.column_name); END IF;
    END LOOP;

    ALTER TABLE public.media_files ADD COLUMN owner_id_uuid uuid;
    UPDATE public.media_files f SET owner_id_uuid = m.new_id
    FROM uuid_migration.id_map m
    WHERE m.old_id = f.owner_id AND m.table_name = CASE f.owner_type
        WHEN 'SURVEY' THEN 'surveys' WHEN 'PAGE' THEN 'pages'
        WHEN 'QUESTION' THEN 'questions' WHEN 'OPTION' THEN 'options' END;
    IF EXISTS (SELECT 1 FROM public.media_files WHERE owner_id_uuid IS NULL) THEN
        RAISE EXCEPTION 'Unmapped media owner';
    END IF;
    ALTER TABLE public.media_files DROP COLUMN owner_id;
    ALTER TABLE public.media_files RENAME COLUMN owner_id_uuid TO owner_id;
    ALTER TABLE public.media_files ALTER COLUMN owner_id SET NOT NULL;

    FOR r IN SELECT table_name FROM _uuid_id_tables LOOP
        EXECUTE format('ALTER TABLE public.%I DROP COLUMN id', r.table_name);
        EXECUTE format('ALTER TABLE public.%I RENAME COLUMN id_uuid TO id', r.table_name);
        EXECUTE format('ALTER TABLE public.%I ALTER COLUMN id SET NOT NULL', r.table_name);
        EXECUTE format('ALTER TABLE public.%I ALTER COLUMN id SET DEFAULT gen_random_uuid()', r.table_name);
    END LOOP;

    FOR r IN SELECT * FROM _uuid_constraints WHERE contype IN ('p', 'u', 'c') ORDER BY CASE contype WHEN 'p' THEN 1 WHEN 'u' THEN 2 ELSE 3 END LOOP
        EXECUTE format('ALTER TABLE %s ADD CONSTRAINT %I %s', r.table_name, r.conname, r.definition);
    END LOOP;
    FOR r IN SELECT definition FROM _uuid_indexes LOOP EXECUTE r.definition; END LOOP;
    FOR r IN SELECT * FROM _uuid_constraints WHERE contype = 'f' LOOP
        EXECUTE format('ALTER TABLE %s ADD CONSTRAINT %I %s', r.table_name, r.conname, r.definition);
    END LOOP;
END $$;

COMMIT;
