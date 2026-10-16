-- Run after scripts/migrate-media.ps1 has copied and verified every object.
\set ON_ERROR_STOP on
BEGIN;
CREATE TEMP TABLE _media_key_changes ON COMMIT DROP AS
SELECT f.id,
       f.object_key AS old_key,
       lower(f.owner_type) || '/' || f.owner_id::text || '/' ||
           substring(f.object_key FROM length(lower(f.owner_type) || '/' || m.old_id::text || '/') + 1) AS new_key,
       f.object_url AS old_url,
       replace(f.object_url, f.object_key,
           lower(f.owner_type) || '/' || f.owner_id::text || '/' ||
           substring(f.object_key FROM length(lower(f.owner_type) || '/' || m.old_id::text || '/') + 1)) AS new_url
FROM media_files f
JOIN uuid_migration.id_map m ON m.new_id = f.owner_id
    AND m.table_name = CASE f.owner_type
        WHEN 'SURVEY' THEN 'surveys' WHEN 'PAGE' THEN 'pages'
        WHEN 'QUESTION' THEN 'questions' WHEN 'OPTION' THEN 'options' END
WHERE f.object_key LIKE lower(f.owner_type) || '/' || m.old_id::text || '/%';

DO $$ BEGIN
    IF (SELECT count(*) FROM _media_key_changes) <> (SELECT count(*) FROM media_files) THEN
        RAISE EXCEPTION 'Not all media rows have a known old-to-new owner mapping';
    END IF;
END $$;

UPDATE media_files f SET object_key = c.new_key, object_url = c.new_url
FROM _media_key_changes c WHERE f.id = c.id;
COMMIT;
