ALTER TABLE users
    ADD COLUMN IF NOT EXISTS telegram_chat_id BIGINT;

CREATE UNIQUE INDEX IF NOT EXISTS users_telegram_chat_id_unique
    ON users (telegram_chat_id)
    WHERE telegram_chat_id IS NOT NULL;
