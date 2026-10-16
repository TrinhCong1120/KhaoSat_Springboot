-- Localize only the known legacy test survey's labels and question metadata.
-- Never rewrite submitted answer text. Safe to run more than once.
BEGIN;
UPDATE surveys
SET title='Khảo sát trải nghiệm khách hàng',
    description='Bộ dữ liệu mẫu dùng để kiểm tra đầy đủ survey-service'
WHERE id='be189111-693b-4fe8-a500-be837695b14e'
  AND title='Khao sat trai nghiem khach hang'
  AND description='Bo du lieu mau dung de test day du survey-service';

UPDATE pages p SET title=labels.new_title
FROM (VALUES
  ('Thong tin chung','Thông tin chung'),
  ('Danh gia chi tiet va gop y','Đánh giá chi tiết và góp ý')
) AS labels(old_title,new_title)
WHERE p.survey_id='be189111-693b-4fe8-a500-be837695b14e'
  AND p.title=labels.old_title;

UPDATE questions q SET question_text=labels.new_text,
  description='Câu hỏi trong bộ dữ liệu mẫu'
FROM pages p,
  (VALUES
    ('Ban biet den san pham qua kenh nao?','Bạn biết đến sản phẩm qua kênh nào?'),
    ('Tinh nang nao ban quan tam?','Tính năng nào bạn quan tâm?'),
    ('Ban co gop y gi de chung toi cai thien?','Bạn có góp ý gì để chúng tôi cải thiện?'),
    ('Ban danh gia tong the bao nhieu diem?','Bạn đánh giá tổng thể bao nhiêu điểm?'),
    ('Ngay ban su dung dich vu gan nhat?','Ngày bạn sử dụng dịch vụ gần nhất?'),
    ('Khu vuc cua ban?','Khu vực của bạn?')
  ) AS labels(old_text,new_text)
WHERE q.page_id=p.id
  AND p.survey_id='be189111-693b-4fe8-a500-be837695b14e'
  AND q.question_text=labels.old_text;

UPDATE options o SET option_text=labels.new_text
FROM questions q JOIN pages p ON p.id=q.page_id,
  (VALUES
    ('Toc do','Tốc độ'),('Bao mat','Bảo mật'),
    ('Giao dien','Giao diện'),('Ban be gioi thieu','Bạn bè giới thiệu'),
    ('Khac','Khác'),('Bao cao','Báo cáo')
  ) AS labels(old_text,new_text)
WHERE o.question_id=q.id
  AND p.survey_id='be189111-693b-4fe8-a500-be837695b14e'
  AND o.option_text=labels.old_text;
COMMIT;
