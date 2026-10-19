-- Apply to the shared PostgreSQL database before deploying the new services.
-- Safe to run more than once.
BEGIN;

CREATE TABLE IF NOT EXISTS notification_deliveries (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    origin_key varchar(160) UNIQUE,
    source varchar(16) NOT NULL CHECK (source IN ('SYSTEM', 'MANUAL')),
    actor_user_id uuid,
    actor_name varchar(255),
    recipient_user_id uuid,
    recipient_name varchar(255),
    destination varchar(320) NOT NULL,
    channel varchar(16) NOT NULL CHECK (channel IN ('EMAIL', 'TELEGRAM')),
    subject varchar(500) NOT NULL,
    content text NOT NULL,
    event_type varchar(100),
    reference_id uuid,
    retry_of uuid REFERENCES notification_deliveries(id),
    status varchar(16) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED')),
    attempts integer NOT NULL DEFAULT 0,
    next_attempt_at timestamptz NOT NULL DEFAULT now(),
    lease_until timestamptz,
    error_note varchar(500),
    created_at timestamptz NOT NULL DEFAULT now(),
    sent_at timestamptz
);
CREATE INDEX IF NOT EXISTS ix_notification_due
    ON notification_deliveries(next_attempt_at, created_at)
    WHERE status = 'PENDING';
CREATE INDEX IF NOT EXISTS ix_notification_processing
    ON notification_deliveries(lease_until)
    WHERE status = 'PROCESSING';
CREATE INDEX IF NOT EXISTS ix_notification_history
    ON notification_deliveries(created_at DESC);

CREATE TABLE IF NOT EXISTS notification_attempts (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    delivery_id uuid NOT NULL REFERENCES notification_deliveries(id),
    attempt_no integer NOT NULL,
    started_at timestamptz NOT NULL DEFAULT now(),
    finished_at timestamptz,
    status varchar(16) NOT NULL CHECK (status IN ('STARTED', 'SENT', 'FAILED')),
    error_note varchar(500),
    UNIQUE(delivery_id, attempt_no)
);

INSERT INTO functions(id, code, name)
SELECT gen_random_uuid(), 'notifications', 'Quản lý thông báo'
WHERE NOT EXISTS (SELECT 1 FROM functions WHERE code = 'notifications');

INSERT INTO permissions(id, code, name, http_method, is_active, function_id, type)
SELECT gen_random_uuid(), p.code, p.name, p.method, true, f.id, p.type
FROM functions f CROSS JOIN (VALUES
    ('notification_view', 'Xem lịch sử thông báo', 'GET', 'VIEW'),
    ('notification_send', 'Gửi và gửi lại thông báo', 'POST', 'CREATE')
) AS p(code, name, method, type)
WHERE f.code = 'notifications'
  AND NOT EXISTS (SELECT 1 FROM permissions existing WHERE existing.code = p.code);

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE lower(r.name) = 'admin'
  AND p.code IN ('notification_view', 'notification_send')
ON CONFLICT DO NOTHING;

INSERT INTO menus(id, name, icon, path, order_index, created_at, function_id)
SELECT gen_random_uuid(), 'Thông báo', 'BellIcon', '/notifications',
       COALESCE((SELECT max(order_index) + 1 FROM menus WHERE parent_id IS NULL), 100),
       now(), f.id
FROM functions f WHERE f.code = 'notifications'
  AND NOT EXISTS (SELECT 1 FROM menus WHERE path = '/notifications');

COMMIT;
