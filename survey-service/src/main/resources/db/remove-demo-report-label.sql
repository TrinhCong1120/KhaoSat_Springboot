BEGIN;

UPDATE surveys
SET description = replace(description, ' Dữ liệu mẫu để kiểm tra báo cáo.', '')
WHERE creator_user = 'demo_report'
  AND description LIKE '%Dữ liệu mẫu để kiểm tra báo cáo.';

COMMIT;
