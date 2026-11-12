-- Run once before enabling the Zalo webhook. Safe to rerun.
CREATE TABLE IF NOT EXISTS zalo_chat_messages (
    id bigserial PRIMARY KEY,
    message_key varchar(200) NOT NULL,
    chat_id varchar(100) NOT NULL,
    account_user_id uuid,
    direction varchar(3) NOT NULL CHECK (direction IN ('IN', 'OUT')),
    action varchar(16) NOT NULL,
    content text NOT NULL,
    status varchar(16) NOT NULL,
    error_note varchar(500),
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (message_key, direction)
);
CREATE INDEX IF NOT EXISTS ix_zalo_chat_messages_created
    ON zalo_chat_messages(created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS ix_zalo_chat_messages_chat
    ON zalo_chat_messages(chat_id, created_at DESC);
