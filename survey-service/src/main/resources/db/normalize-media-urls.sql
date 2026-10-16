-- Run once for media records created while MinIO was exposed at localhost:9000.
-- Relative URLs keep working when the public ngrok hostname changes.
BEGIN;

UPDATE media_files
SET object_url = regexp_replace(object_url, '^(http://localhost:9000)?/survey-media/', '/api/survey-media/')
WHERE object_url ~ '^(http://localhost:9000)?/survey-media/';

COMMIT;
