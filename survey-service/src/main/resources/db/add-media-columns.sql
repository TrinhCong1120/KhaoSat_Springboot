ALTER TABLE pages
    ADD COLUMN IF NOT EXISTS description TEXT;

CREATE TABLE IF NOT EXISTS media_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_type VARCHAR(50) NOT NULL,
    owner_id UUID NOT NULL,
    media_type VARCHAR(50) NOT NULL,
    original_filename VARCHAR(500),
    content_type VARCHAR(255),
    size_bytes BIGINT,
    bucket_name VARCHAR(255) NOT NULL,
    object_key VARCHAR(1000) NOT NULL,
    object_url TEXT NOT NULL,
    created_by_user_id UUID,
    uploaded_at TIMESTAMP NOT NULL
);

ALTER TABLE media_files
    ADD COLUMN IF NOT EXISTS created_by_user_id UUID;

CREATE INDEX IF NOT EXISTS idx_media_files_owner
    ON media_files(owner_type, owner_id);

CREATE INDEX IF NOT EXISTS idx_media_files_created_by_user_id
    ON media_files(created_by_user_id);
