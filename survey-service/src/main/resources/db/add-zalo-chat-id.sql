ALTER TABLE users
    ADD COLUMN IF NOT EXISTS zalo_chat_id varchar(100);

CREATE UNIQUE INDEX IF NOT EXISTS users_zalo_chat_id_unique
    ON users (zalo_chat_id)
    WHERE zalo_chat_id IS NOT NULL;
