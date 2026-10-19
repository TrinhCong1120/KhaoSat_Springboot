-- Run once before enabling the Telegram webhook. Safe to rerun.
CREATE TABLE IF NOT EXISTS telegram_chat_messages (
    id bigserial PRIMARY KEY,
    update_id bigint NOT NULL,
    chat_id bigint NOT NULL,
    account_user_id uuid,
    direction varchar(3) NOT NULL CHECK (direction IN ('IN', 'OUT')),
    action varchar(16) NOT NULL,
    content text NOT NULL,
    status varchar(16) NOT NULL,
    error_note varchar(500),
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (update_id, direction)
);
CREATE INDEX IF NOT EXISTS ix_telegram_chat_messages_created
    ON telegram_chat_messages(created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS ix_telegram_chat_messages_chat
    ON telegram_chat_messages(chat_id, created_at DESC);
