-- Extend notification channels with ZALO. Safe to rerun.
BEGIN;

ALTER TABLE notification_deliveries DROP CONSTRAINT IF EXISTS notification_deliveries_channel_check;
ALTER TABLE notification_deliveries
    ADD CONSTRAINT notification_deliveries_channel_check
    CHECK (channel IN ('EMAIL', 'TELEGRAM', 'ZALO'));

ALTER TABLE notification_inbox_messages DROP CONSTRAINT IF EXISTS notification_inbox_messages_channel_check;
ALTER TABLE notification_inbox_messages
    ADD CONSTRAINT notification_inbox_messages_channel_check
    CHECK (channel IN ('EMAIL', 'TELEGRAM', 'ZALO'));

COMMIT;
