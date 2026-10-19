-- Run after notification-management.sql and notification-bounces.sql, before deploying services.
BEGIN;
CREATE TABLE IF NOT EXISTS notification_inbox_messages (
    id uuid PRIMARY KEY,
    channel varchar(16) NOT NULL CHECK (channel IN ('EMAIL', 'TELEGRAM')),
    provider_key varchar(400) NOT NULL UNIQUE,
    provider_message_id varchar(500),
    sender_address varchar(500) NOT NULL,
    reply_destination varchar(500),
    recipient_address varchar(500) NOT NULL,
    sender_user_id uuid,
    matched_delivery_id uuid REFERENCES notification_deliveries(id),
    subject varchar(500),
    content text NOT NULL DEFAULT '',
    kind varchar(16) NOT NULL DEFAULT 'MESSAGE' CHECK (kind IN ('MESSAGE', 'BOUNCE', 'COMMAND')),
    handling_status varchar(16) NOT NULL DEFAULT 'NEW' CHECK (handling_status IN ('NEW', 'RESOLVED')),
    received_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS notification_inbox_received_idx ON notification_inbox_messages(received_at DESC);
CREATE INDEX IF NOT EXISTS notification_inbox_match_idx ON notification_inbox_messages(matched_delivery_id);
ALTER TABLE notification_inbox_messages ADD COLUMN IF NOT EXISTS reply_destination varchar(500);

CREATE TABLE IF NOT EXISTS notification_inbox_attachments (
    id uuid PRIMARY KEY,
    inbox_id uuid NOT NULL REFERENCES notification_inbox_messages(id) ON DELETE CASCADE,
    attachment_key varchar(100),
    file_name varchar(500) NOT NULL,
    content_type varchar(200),
    size_bytes bigint,
    storage_key varchar(500),
    status varchar(16) NOT NULL CHECK (status IN ('STORED', 'OVERSIZE', 'FAILED')),
    error_note varchar(500),
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS notification_inbox_attachment_idx ON notification_inbox_attachments(inbox_id);
ALTER TABLE notification_inbox_attachments ADD COLUMN IF NOT EXISTS attachment_key varchar(100);
CREATE UNIQUE INDEX IF NOT EXISTS notification_inbox_attachment_key_idx
    ON notification_inbox_attachments(inbox_id, attachment_key) WHERE attachment_key IS NOT NULL;

ALTER TABLE notification_deliveries ADD COLUMN IF NOT EXISTS provider_message_id varchar(500);
ALTER TABLE notification_deliveries ADD COLUMN IF NOT EXISTS in_reply_to_inbound_id uuid REFERENCES notification_inbox_messages(id);
CREATE INDEX IF NOT EXISTS notification_delivery_provider_idx
    ON notification_deliveries(channel, destination, provider_message_id)
    WHERE provider_message_id IS NOT NULL;
COMMIT;
