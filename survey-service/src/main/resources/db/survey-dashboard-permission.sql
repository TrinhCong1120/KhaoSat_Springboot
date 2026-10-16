-- Apply once before deploying dashboard_view authorization. Safe to rerun.
BEGIN;

INSERT INTO functions(id, code, name)
SELECT gen_random_uuid(), 'survey_dashboard', 'Dashboard khảo sát'
WHERE NOT EXISTS (SELECT 1 FROM functions WHERE code = 'survey_dashboard');

INSERT INTO permissions(id, code, name, http_method, is_active, function_id, type)
SELECT gen_random_uuid(), 'dashboard_view', 'Xem Dashboard khảo sát', 'GET', true, f.id, 'VIEW'
FROM functions f
WHERE f.code = 'survey_dashboard'
  AND NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'dashboard_view')
LIMIT 1;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE lower(r.name) = 'admin' AND p.code = 'dashboard_view'
ON CONFLICT DO NOTHING;

COMMIT;
