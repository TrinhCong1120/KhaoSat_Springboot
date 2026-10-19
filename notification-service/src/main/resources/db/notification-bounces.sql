-- Apply after notification-management.sql. Safe to run again.
BEGIN;

ALTER TABLE notification_deliveries
    ADD COLUMN IF NOT EXISTS bounced_at timestamptz;

ALTER TABLE notification_deliveries
    DROP CONSTRAINT IF EXISTS notification_deliveries_status_check;
ALTER TABLE notification_deliveries
    ADD CONSTRAINT notification_deliveries_status_check
    CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'BOUNCED'));

CREATE TABLE IF NOT EXISTS notification_bounce_cursor (
    mailbox varchar(320) PRIMARY KEY,
    uid_validity bigint NOT NULL,
    last_uid bigint NOT NULL DEFAULT 0,
    updated_at timestamptz NOT NULL DEFAULT now()
);

COMMIT;
