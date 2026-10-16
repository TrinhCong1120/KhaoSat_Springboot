-- Synthetic local dashboard data. Apply only to a development database.
-- Requires survey-reporting.sql and survey-dashboard-reporting.sql.
-- Stable UUIDs and ON CONFLICT make reruns safe; do not use this in production.
BEGIN;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM roles WHERE name='user')
      OR NOT EXISTS (SELECT 1 FROM roles WHERE name='Manager')
      OR (SELECT count(*) FROM question_types WHERE code IN
          ('ADDRESS','DATE','MULTIPLE_CHOICE','NUMBER','SINGLE_CHOICE','TEXT')) <> 6 THEN
    RAISE EXCEPTION 'Seed requires user/Manager roles and all six question types';
  END IF;
END $$;

CREATE TEMP TABLE demo_specs (
  code text PRIMARY KEY, title text NOT NULL, summary text NOT NULL,
  state text NOT NULL, sessions integer NOT NULL, submissions integer NOT NULL,
  positive text NOT NULL, suggestion text NOT NULL
) ON COMMIT DROP;

INSERT INTO demo_specs VALUES
('citizen_service','Đánh giá dịch vụ hành chính một cửa','Thời gian chờ, thái độ phục vụ và hướng dẫn hồ sơ','OPEN',110,62,'Nhân viên hướng dẫn rõ ràng, dễ hiểu','Rút ngắn thời gian chờ vào giờ cao điểm'),
('online_learning','Trải nghiệm học trực tuyến của sinh viên','Chất lượng bài giảng, nền tảng học và hỗ trợ giảng viên','OPEN',95,54,'Bài giảng dễ theo dõi và tài liệu đầy đủ','Cải thiện chất lượng âm thanh khi học trực tuyến'),
('library_service','Chất lượng dịch vụ thư viện','Không gian học tập, tài liệu và dịch vụ mượn trả','OPEN',85,42,'Không gian yên tĩnh và sách dễ tìm','Bổ sung thêm chỗ ngồi và ổ cắm điện'),
('public_transport','Nhu cầu sử dụng xe buýt đô thị','Tuyến đường, tần suất và trải nghiệm đi lại','CLOSED',70,30,'Giá vé phù hợp và trạm dừng thuận tiện','Tăng chuyến vào buổi sáng và giờ tan học'),
('career_fair','Phản hồi ngày hội việc làm','Thông tin tuyển dụng và trải nghiệm tại sự kiện','CLOSED',65,35,'Nhiều doanh nghiệp phù hợp với chuyên ngành','Bố trí thêm khu vực tư vấn riêng'),
('digital_skills','Nhu cầu đào tạo kỹ năng số năm tới','Chủ đề học tập và hình thức tổ chức mong muốn','SCHEDULED',0,0,'Nội dung đào tạo thiết thực','Thêm lớp học ngoài giờ hành chính'),
('housing_priority','Ưu tiên cải thiện nhà ở xã hội','Nhu cầu và tiêu chí lựa chọn nhà ở','SCHEDULED',0,0,'Thông tin dự án minh bạch','Công bố lịch đăng ký sớm hơn'),
('mental_health','Sức khỏe tinh thần sinh viên','Nhu cầu hỗ trợ và môi trường học tập','DRAFT',0,0,'Dịch vụ tư vấn thân thiện','Bổ sung kênh tư vấn trực tuyến'),
('canteen','Chất lượng bữa ăn bán trú','Thực đơn, an toàn thực phẩm và mức giá','DRAFT',0,0,'Thực đơn đa dạng và phục vụ nhanh','Bổ sung món ít đường và rau xanh');

INSERT INTO users(id, username, email, password_hash, is_active)
SELECT md5('demo-report-user-'||n)::uuid,
  'demo_report_'||lpad(n::text,2,'0'),
  'demo_report_'||lpad(n::text,2,'0')||'@example.invalid',
  '!demo-account-no-login!', n <= 10
FROM generate_series(1,12) n
ON CONFLICT (id) DO NOTHING;

INSERT INTO user_roles(user_id, role_id)
SELECT md5('demo-report-user-'||n)::uuid, r.id
FROM generate_series(1,12) n
JOIN roles r ON r.name = 'user' OR (n % 4 = 0 AND r.name = 'Manager')
ON CONFLICT DO NOTHING;

INSERT INTO surveys(id,title,description,created_at,creator_user,creator_user_id,
                    published_at,open_at,close_at,share_code)
SELECT md5('demo-report-survey-'||d.code)::uuid, d.title,
  d.summary||'.',
  (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')
    - CASE d.state WHEN 'DRAFT' THEN interval '4 days'
      WHEN 'SCHEDULED' THEN interval '7 days'
      WHEN 'OPEN' THEN interval '45 days' ELSE interval '55 days' END,
  'demo_report', md5('demo-report-user-'||(1+length(d.code)%8))::uuid,
  CASE d.state WHEN 'DRAFT' THEN NULL
    WHEN 'SCHEDULED' THEN now()-interval '2 days'
    WHEN 'OPEN' THEN now()-interval '44 days'
    ELSE now()-interval '54 days' END,
  CASE d.state WHEN 'DRAFT' THEN NULL
    WHEN 'SCHEDULED' THEN now()+interval '5 days'
    WHEN 'OPEN' THEN now()-interval '43 days'
    ELSE now()-interval '53 days' END,
  CASE d.state WHEN 'SCHEDULED' THEN now()+interval '35 days'
    WHEN 'CLOSED' THEN now()-interval '2 days' ELSE NULL END,
  'D'||substring(md5(d.code),1,9)
FROM demo_specs d
ON CONFLICT (id) DO NOTHING;

INSERT INTO pages(id,survey_id,title,description,order_index)
SELECT md5('demo-report-page-'||d.code||'-'||n)::uuid,
  md5('demo-report-survey-'||d.code)::uuid,
  CASE n WHEN 1 THEN 'Trải nghiệm chung' ELSE 'Ý kiến cải thiện' END,
  CASE n WHEN 1 THEN 'Đánh giá tổng quan' ELSE 'Góp ý chi tiết' END, n
FROM demo_specs d CROSS JOIN generate_series(1,2) n
ON CONFLICT (id) DO NOTHING;

CREATE TEMP TABLE demo_question_specs (
  idx integer PRIMARY KEY, page_number integer NOT NULL,
  question text NOT NULL, type_code text NOT NULL, required boolean NOT NULL
) ON COMMIT DROP;
INSERT INTO demo_question_specs VALUES
(1,1,'Bạn đánh giá trải nghiệm tổng thể ở mức nào? (1–5)','NUMBER',true),
(2,1,'Điều gì đang hoạt động tốt?','TEXT',false),
(3,2,'Bạn đề xuất thay đổi nào?','TEXT',false),
(4,1,'Bạn có sẵn sàng giới thiệu trải nghiệm này cho người khác không?','SINGLE_CHOICE',true),
(5,1,'Những yếu tố nào ảnh hưởng nhiều nhất đến đánh giá của bạn?','MULTIPLE_CHOICE',false),
(6,2,'Lần gần nhất bạn sử dụng dịch vụ hoặc tham gia hoạt động này là ngày nào?','DATE',false),
(7,2,'Bạn hiện sinh sống ở địa phương nào?','ADDRESS',false);

INSERT INTO questions(id,page_id,question_type_id,question_text,description,is_required,order_index)
SELECT md5('demo-report-question-'||d.code||'-'||q.idx)::uuid,
  md5('demo-report-page-'||d.code||'-'||q.page_number)::uuid,
  qt.id,q.question,'Câu hỏi trong bộ dữ liệu mẫu',q.required,q.idx
FROM demo_specs d CROSS JOIN demo_question_specs q
JOIN question_types qt ON qt.code=q.type_code
ON CONFLICT (id) DO NOTHING;

CREATE TEMP TABLE demo_option_specs (
  question_idx integer NOT NULL, option_idx integer NOT NULL, label text NOT NULL,
  PRIMARY KEY (question_idx, option_idx)
) ON COMMIT DROP;
INSERT INTO demo_option_specs VALUES
(4,1,'Chắc chắn có'),(4,2,'Có thể'),(4,3,'Chưa chắc'),(4,4,'Không'),
(5,1,'Chất lượng'),(5,2,'Thời gian'),(5,3,'Chi phí'),(5,4,'Hỗ trợ');

INSERT INTO options(id,question_id,option_text,order_index)
SELECT md5('demo-report-option-'||d.code||'-'||o.question_idx||'-'||o.option_idx)::uuid,
  md5('demo-report-question-'||d.code||'-'||o.question_idx)::uuid,
  o.label,o.option_idx
FROM demo_specs d CROSS JOIN demo_option_specs o
ON CONFLICT (id) DO NOTHING;

CREATE TEMP TABLE demo_sessions ON COMMIT DROP AS
SELECT d.*, g AS sequence,
  md5('demo-report-session-'||d.code||'-'||g)::uuid AS session_id,
  md5('demo-report-visitor-'||(1+g%140))::uuid AS visitor_id,
  CASE WHEN d.state='OPEN' AND g>d.submissions AND g%40=0
    THEN now()-interval '10 minutes'
    WHEN d.state='CLOSED' THEN now()-(3+(g*7)%25)*interval '1 day'
      -(g%7)*interval '1 hour'
    ELSE now()-((g*7)%30)*interval '1 day'-(g%7)*interval '1 hour'
  END AS viewed_at
FROM demo_specs d CROSS JOIN LATERAL generate_series(1,d.sessions) g;

INSERT INTO survey_sessions(id,survey_id,creation_request_id,visitor_id,first_viewed_at,
  started_at,last_activity_at,expires_at,status,active_duration_ms,device_type,
  utm_source,utm_medium,utm_campaign,referrer_host,abandoned_at)
SELECT s.session_id,md5('demo-report-survey-'||s.code)::uuid,
  md5('demo-report-session-request-'||s.code||'-'||s.sequence)::uuid,
  s.visitor_id,s.viewed_at,
  CASE WHEN s.sequence<=s.submissions OR s.sequence%5<>0
      OR (s.state='OPEN' AND s.sequence%40=0)
    THEN s.viewed_at+interval '2 minutes' ELSE NULL END,
  s.viewed_at+interval '5 minutes',
  CASE WHEN s.state='OPEN' AND s.sequence>s.submissions AND s.sequence%40=0
    THEN now()+interval '20 minutes' ELSE s.viewed_at+interval '30 minutes' END,
  CASE WHEN s.sequence<=s.submissions THEN 'STARTED'
    WHEN s.state='OPEN' AND s.sequence%40=0 THEN 'IN_PROGRESS'
    WHEN s.sequence%5=0 THEN 'EXPIRED' ELSE 'ABANDONED' END,
  CASE WHEN s.sequence<=s.submissions THEN 240000+(s.sequence%16)*57000 ELSE NULL END,
  CASE WHEN s.sequence%5=0 THEN 'TABLET'
    WHEN s.sequence%3=0 THEN 'MOBILE' ELSE 'DESKTOP' END,
  CASE WHEN s.sequence%4=0 THEN 'facebook'
    WHEN s.sequence%4=1 THEN 'google' ELSE NULL END,
  CASE WHEN s.sequence%4=0 THEN 'social'
    WHEN s.sequence%4=1 THEN 'organic' ELSE NULL END,
  CASE WHEN s.sequence%4=0 THEN 'survey_demo' ELSE NULL END,
  CASE WHEN s.sequence%4=2 THEN 'example.org' ELSE NULL END,
  CASE WHEN s.sequence>s.submissions
      AND (s.state<>'OPEN' OR s.sequence%40<>0)
      AND s.sequence%5<>0 THEN s.viewed_at+interval '30 minutes' ELSE NULL END
FROM demo_sessions s
ON CONFLICT (id) DO NOTHING;

-- Correct sessions from older versions of this seed without touching real data.
UPDATE survey_sessions target
SET started_at=target.first_viewed_at+interval '2 minutes'
FROM demo_sessions s
WHERE target.id=s.session_id AND s.state='OPEN'
  AND s.sequence>s.submissions AND s.sequence%40=0
  AND target.started_at IS NULL;
UPDATE survey_sessions target
SET first_viewed_at=s.viewed_at,started_at=NULL,
  last_activity_at=s.viewed_at+interval '5 minutes',
  expires_at=s.viewed_at+interval '30 minutes',
  status='EXPIRED',active_duration_ms=NULL,abandoned_at=NULL
FROM demo_sessions s
WHERE target.id=s.session_id AND s.state='CLOSED'
  AND s.sequence>s.submissions AND s.sequence%40=0
  AND target.status='IN_PROGRESS';
UPDATE survey_events e
SET received_at=s.first_viewed_at,client_occurred_at=s.first_viewed_at
FROM survey_sessions s JOIN demo_sessions d ON d.session_id=s.id
WHERE e.session_id=s.id AND e.event_type='SURVEY_VIEWED'
  AND d.state='CLOSED' AND d.sequence>d.submissions
  AND d.sequence%40=0;

INSERT INTO survey_events(id,event_id,session_id,survey_id,event_type,
  client_occurred_at,received_at,event_source)
SELECT md5('demo-report-event-view-'||s.code||'-'||s.sequence)::uuid,
  md5('demo-report-client-view-'||s.code||'-'||s.sequence)::uuid,
  s.session_id,md5('demo-report-survey-'||s.code)::uuid,
  'SURVEY_VIEWED',s.viewed_at,s.viewed_at,'CLIENT'
FROM demo_sessions s
ON CONFLICT (id) DO NOTHING;

INSERT INTO survey_events(id,event_id,session_id,survey_id,event_type,
  client_occurred_at,received_at,event_source)
SELECT md5('demo-report-event-start-'||s.code||'-'||s.sequence)::uuid,
  md5('demo-report-client-start-'||s.code||'-'||s.sequence)::uuid,
  s.session_id,md5('demo-report-survey-'||s.code)::uuid,
  'SURVEY_STARTED',s.viewed_at+interval '2 minutes',
  s.viewed_at+interval '2 minutes','CLIENT'
FROM demo_sessions s
WHERE s.sequence<=s.submissions OR s.sequence%5<>0
  OR (s.state='OPEN' AND s.sequence%40=0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO survey_page_visits(id,session_id,survey_id,page_id,entered_at,
  left_at,completed_at,active_duration_ms)
SELECT md5('demo-report-page-visit-'||s.code||'-'||s.sequence||'-'||p)::uuid,
  s.session_id,md5('demo-report-survey-'||s.code)::uuid,
  md5('demo-report-page-'||s.code||'-'||p)::uuid,
  s.viewed_at + (p*2)*interval '1 minute',
  CASE WHEN s.sequence<=s.submissions
    THEN s.viewed_at + (p*5+2)*interval '1 minute' ELSE NULL END,
  CASE WHEN s.sequence<=s.submissions
    THEN s.viewed_at + (p*5+2)*interval '1 minute' ELSE NULL END,
  CASE WHEN s.sequence<=s.submissions THEN 120000+(s.sequence%9)*20000 ELSE 0 END
FROM demo_sessions s CROSS JOIN generate_series(1,2) p
WHERE (s.sequence<=s.submissions OR s.sequence%5<>0
  OR (s.state='OPEN' AND s.sequence%40=0))
  AND (p=1 OR s.sequence<=s.submissions OR s.sequence%3=0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO survey_events(id,event_id,session_id,survey_id,event_type,
  page_id,client_occurred_at,received_at,event_source)
SELECT md5('demo-report-event-page-'||s.code||'-'||s.sequence||'-'||p)::uuid,
  md5('demo-report-client-page-'||s.code||'-'||s.sequence||'-'||p)::uuid,
  s.session_id,md5('demo-report-survey-'||s.code)::uuid,
  'PAGE_VIEWED',md5('demo-report-page-'||s.code||'-'||p)::uuid,
  s.viewed_at+(p*2)*interval '1 minute',
  s.viewed_at+(p*2)*interval '1 minute','CLIENT'
FROM demo_sessions s CROSS JOIN generate_series(1,2) p
WHERE (s.sequence<=s.submissions OR s.sequence%5<>0
  OR (s.state='OPEN' AND s.sequence%40=0))
  AND (p=1 OR s.sequence<=s.submissions OR s.sequence%3=0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO responses(id,request_id,survey_id,session_id,submitted_at,token,
  applicable_question_count,answered_question_count)
SELECT md5('demo-report-response-'||s.code||'-'||s.sequence)::uuid,
  md5('demo-report-submit-request-'||s.code||'-'||s.sequence)::uuid,
  md5('demo-report-survey-'||s.code)::uuid,s.session_id,
  s.viewed_at+(8+s.sequence%15)*interval '1 minute',
  'DEMO_REPORTING_20261014',7,
  5+CASE WHEN s.sequence%4=0 THEN 0 ELSE 1 END
    +CASE WHEN s.sequence%2=0 THEN 1 ELSE 0 END
FROM demo_sessions s WHERE s.sequence<=s.submissions
ON CONFLICT (id) DO NOTHING;

-- Reruns also bring response completeness counts from an earlier seed up to date.
UPDATE responses r
SET applicable_question_count=7,
  answered_question_count=5+CASE WHEN s.sequence%4=0 THEN 0 ELSE 1 END
    +CASE WHEN s.sequence%2=0 THEN 1 ELSE 0 END
FROM demo_sessions s
WHERE r.id=md5('demo-report-response-'||s.code||'-'||s.sequence)::uuid
  AND s.sequence<=s.submissions;

INSERT INTO answers(id,response_id,question_id,answer_number,answer_text,
                    answer_date,province_code,ward_code,province,ward,address_detail)
SELECT md5('demo-report-answer-'||s.code||'-'||s.sequence||'-'||q.idx)::uuid,
  md5('demo-report-response-'||s.code||'-'||s.sequence)::uuid,
  md5('demo-report-question-'||s.code||'-'||q.idx)::uuid,
  CASE WHEN q.idx=1 THEN (1+(s.sequence*3)%5)::numeric ELSE NULL END,
  CASE q.idx WHEN 2 THEN s.positive WHEN 3 THEN s.suggestion ELSE NULL END,
  CASE WHEN q.idx=6 THEN (s.viewed_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date
    -(s.sequence%14) ELSE NULL END,
  CASE WHEN q.idx=7 THEN '48' ELSE NULL END,
  CASE WHEN q.idx=7 THEN '1' ELSE NULL END,
  CASE WHEN q.idx=7 THEN 'Thành phố Đà Nẵng' ELSE NULL END,
  CASE WHEN q.idx=7 THEN 'Phường Hải Châu' ELSE NULL END,
  CASE WHEN q.idx=7 THEN 'Khu vực trung tâm' ELSE NULL END
FROM demo_sessions s CROSS JOIN demo_question_specs q
WHERE s.sequence<=s.submissions AND (q.idx<>3 OR s.sequence%4<>0)
  AND (q.idx<>7 OR s.sequence%2=0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO answer_options(id,answer_id,option_id)
SELECT md5('demo-report-answer-option-'||s.code||'-'||s.sequence||'-'||o.question_idx||'-'||o.option_idx)::uuid,
  md5('demo-report-answer-'||s.code||'-'||s.sequence||'-'||o.question_idx)::uuid,
  md5('demo-report-option-'||s.code||'-'||o.question_idx||'-'||o.option_idx)::uuid
FROM demo_sessions s CROSS JOIN demo_option_specs o
WHERE s.sequence<=s.submissions AND (
  (o.question_idx=4 AND o.option_idx=1+s.sequence%4) OR
  (o.question_idx=5 AND (o.option_idx=1+s.sequence%4
    OR (s.sequence%3=0 AND o.option_idx=1+(s.sequence+1)%4)))
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO site_daily_visitors(visit_date,visitor_id,first_seen_at)
SELECT ((now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date - d),
  md5('demo-report-visitor-'||(n+(d%4)*20))::uuid,
  (((now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date-d)::timestamp
    AT TIME ZONE 'Asia/Ho_Chi_Minh') + interval '1 hour'
FROM generate_series(0,29) d
CROSS JOIN LATERAL generate_series(1,35+(d%6)*5) n
ON CONFLICT DO NOTHING;

INSERT INTO site_daily_visitors(visit_date,visitor_id,first_seen_at)
SELECT (s.viewed_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date,
  s.visitor_id,min(s.viewed_at)
FROM demo_sessions s GROUP BY 1,2
ON CONFLICT DO NOTHING;

COMMIT;

SELECT 'demo_surveys' AS metric,count(*) AS total FROM surveys
  WHERE creator_user='demo_report'
UNION ALL SELECT 'demo_responses',count(*) FROM responses
  WHERE token='DEMO_REPORTING_20261014'
UNION ALL SELECT 'demo_sessions',count(*) FROM survey_sessions s
  JOIN surveys v ON v.id=s.survey_id
  WHERE v.creator_user='demo_report'
UNION ALL SELECT 'site_daily_visitors',count(*) FROM site_daily_visitors;
