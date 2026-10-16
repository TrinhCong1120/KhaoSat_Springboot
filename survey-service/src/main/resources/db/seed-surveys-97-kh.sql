-- Seed 2 surveys from PDF "97/KH-UBND" - Da Nang social housing demand survey.
-- Target DB: PostgreSQL. Run this after Hibernate has created the tables.

BEGIN;

INSERT INTO question_types (code)
SELECT 'TEXT'
WHERE NOT EXISTS (SELECT 1 FROM question_types WHERE code = 'TEXT');

INSERT INTO question_types (code)
SELECT 'SINGLE_CHOICE'
WHERE NOT EXISTS (SELECT 1 FROM question_types WHERE code = 'SINGLE_CHOICE');

INSERT INTO question_types (code)
SELECT 'MULTIPLE_CHOICE'
WHERE NOT EXISTS (SELECT 1 FROM question_types WHERE code = 'MULTIPLE_CHOICE');

CREATE TEMP TABLE _seed_surveys (
  survey_key TEXT PRIMARY KEY,
  id UUID NOT NULL
) ON COMMIT DROP;

CREATE TEMP TABLE _seed_pages (
  survey_key TEXT NOT NULL,
  page_key TEXT NOT NULL,
  id UUID NOT NULL,
  PRIMARY KEY (survey_key, page_key)
) ON COMMIT DROP;

CREATE TEMP TABLE _seed_questions (
  survey_key TEXT NOT NULL,
  question_key TEXT NOT NULL,
  id UUID NOT NULL,
  PRIMARY KEY (survey_key, question_key)
) ON COMMIT DROP;

CREATE TEMP TABLE _seed_options (
  survey_key TEXT NOT NULL,
  option_key TEXT NOT NULL,
  id UUID NOT NULL,
  PRIMARY KEY (survey_key, option_key)
) ON COMMIT DROP;

WITH data(survey_key, title, description) AS (
  VALUES
    ('S1_BUY',
     'Phiếu khảo sát nhu cầu mua nhà ở xã hội trên địa bàn thành phố Đà Nẵng',
     'Biểu mẫu 1 ban hành kèm Kế hoạch số 97/KH-UBND ngày 09/3/2026.'),
    ('S2_RENT',
     'Phiếu khảo sát nhu cầu thuê nhà ở xã hội, nhà lưu trú công nhân trong khu công nghiệp trên địa bàn thành phố Đà Nẵng',
     'Biểu mẫu 2 ban hành kèm Kế hoạch số 97/KH-UBND ngày 09/3/2026.')
),
inserted AS (
  INSERT INTO surveys (title, description, creator_user, created_at, published_at)
  SELECT d.title, d.description, 'system', NOW(), NOW()
  FROM data d
  WHERE NOT EXISTS (SELECT 1 FROM surveys s WHERE s.title = d.title)
  RETURNING id, title
)
INSERT INTO _seed_surveys (survey_key, id)
SELECT d.survey_key, s.id
FROM data d
JOIN surveys s ON s.title = d.title;

WITH data(survey_key, page_key, title, order_index) AS (
  VALUES
    ('S1_BUY', 'PERSONAL', 'Thông tin cá nhân của người khảo sát', 1),
    ('S1_BUY', 'CONTENT', 'Nội dung khảo sát', 2),
    ('S2_RENT', 'PERSONAL', 'Thông tin cá nhân của người khảo sát', 1),
    ('S2_RENT', 'CONTENT', 'Nội dung khảo sát', 2)
),
inserted AS (
  INSERT INTO pages (survey_id, title, order_index)
  SELECT ss.id, d.title, d.order_index
  FROM data d
  JOIN _seed_surveys ss ON ss.survey_key = d.survey_key
  WHERE NOT EXISTS (
    SELECT 1 FROM pages p
    WHERE p.survey_id = ss.id AND p.order_index = d.order_index
  )
  RETURNING id, survey_id, title, order_index
)
INSERT INTO _seed_pages (survey_key, page_key, id)
SELECT d.survey_key, d.page_key, p.id
FROM data d
JOIN _seed_surveys ss ON ss.survey_key = d.survey_key
JOIN pages p ON p.survey_id = ss.id AND p.order_index = d.order_index;

WITH data(survey_key, page_key, question_key, question_text, type_code, is_required, order_index, description) AS (
  VALUES
    ('S1_BUY','PERSONAL','S1_NAME','Họ và tên','TEXT',TRUE,1,NULL),
    ('S1_BUY','PERSONAL','S1_PHONE','Số điện thoại','TEXT',TRUE,2,NULL),
    ('S1_BUY','PERSONAL','S1_EMAIL','Email','TEXT',FALSE,3,NULL),

    ('S1_BUY','CONTENT','S1_Q1','Câu 1. Ông (bà) thuộc nhóm đối tượng nào?','SINGLE_CHOICE',TRUE,1,NULL),
    ('S1_BUY','CONTENT','S1_Q2','Câu 2. Ông (bà) thuộc độ tuổi nào?','SINGLE_CHOICE',TRUE,2,NULL),
    ('S1_BUY','CONTENT','S1_Q3','Câu 3. Ông (bà) độc thân hay đã kết hôn?','SINGLE_CHOICE',TRUE,3,NULL),
    ('S1_BUY','CONTENT','S1_Q4A','Câu 4a. Ông (bà) có mức thu nhập như thế nào?','SINGLE_CHOICE',TRUE,4,'Hiển thị khi Câu 3 chọn Độc thân; ẩn với các nhóm không phải trả lời Câu 4a, 4b.'),
    ('S1_BUY','CONTENT','S1_Q4B','Câu 4b. Hộ gia đình Ông (bà) có mức thu nhập như thế nào?','SINGLE_CHOICE',TRUE,5,'Hiển thị khi Câu 3 chọn Kết hôn; ẩn với các nhóm không phải trả lời Câu 4a, 4b.'),
    ('S1_BUY','CONTENT','S1_Q5','Câu 5. Ông (bà) đang cư trú tại khu vực nào?','SINGLE_CHOICE',TRUE,6,NULL),
    ('S1_BUY','CONTENT','S1_Q6A','Câu 6a. Tình trạng nhà ở của Ông (bà) tại khu vực đang cư trú như thế nào?','SINGLE_CHOICE',TRUE,7,'Hiển thị khi Câu 5 chọn khu vực thành phố Đà Nẵng hoặc tỉnh Quảng Nam trước hợp nhất.'),
    ('S1_BUY','CONTENT','S1_Q6B','Câu 6b. Tình trạng nhà ở của Ông (bà) tại thành phố Đà Nẵng như thế nào?','SINGLE_CHOICE',TRUE,8,'Hiển thị khi Câu 5 chọn Địa phương khác.'),
    ('S1_BUY','CONTENT','S1_Q7','Câu 7. Nhu cầu khu vực mua nhà ở xã hội?','MULTIPLE_CHOICE',TRUE,9,'Chọn 03 khu vực theo thứ tự ưu tiên: Ưu tiên 1, Ưu tiên 2, Ưu tiên 3.'),
    ('S1_BUY','CONTENT','S1_Q8','Câu 8. Nhu cầu về diện tích căn hộ','SINGLE_CHOICE',TRUE,10,NULL),
    ('S1_BUY','CONTENT','S1_Q9','Câu 9. Nhu cầu về giá mua nhà ở xã hội?','SINGLE_CHOICE',TRUE,11,NULL),
    ('S1_BUY','CONTENT','S1_Q10','Câu 10. Ông (bà) có nhu cầu vay vốn để mua nhà ở xã hội không?','SINGLE_CHOICE',TRUE,12,'Vay tại Ngân hàng Chính sách xã hội: lãi suất cho vay 5,4%/năm, thời hạn vay tối đa không quá 25 năm.'),

    ('S2_RENT','PERSONAL','S2_NAME','Họ và tên','TEXT',TRUE,1,NULL),
    ('S2_RENT','PERSONAL','S2_PHONE','Số điện thoại','TEXT',TRUE,2,NULL),
    ('S2_RENT','PERSONAL','S2_EMAIL','Email','TEXT',FALSE,3,NULL),

    ('S2_RENT','CONTENT','S2_Q1','Câu 1. Ông (bà) thuộc nhóm đối tượng nào?','SINGLE_CHOICE',TRUE,1,NULL),
    ('S2_RENT','CONTENT','S2_Q2','Câu 2. Ông (bà) thuộc độ tuổi nào?','SINGLE_CHOICE',TRUE,2,NULL),
    ('S2_RENT','CONTENT','S2_Q3','Câu 3. Ông (bà) độc thân hay đã kết hôn?','SINGLE_CHOICE',TRUE,3,NULL),
    ('S2_RENT','CONTENT','S2_Q4','Câu 4. Ông (bà) đang cư trú tại khu vực nào?','SINGLE_CHOICE',TRUE,4,NULL),
    ('S2_RENT','CONTENT','S2_Q5','Câu 5. Ông (bà) mong muốn thuê loại nhà ở nào?','SINGLE_CHOICE',TRUE,5,NULL),
    ('S2_RENT','CONTENT','S2_Q6','Câu 6. Nhu cầu khu vực thuê nhà ở xã hội, nhà lưu trú công nhân trong khu công nghiệp?','MULTIPLE_CHOICE',TRUE,6,'Chọn 03 khu vực theo thứ tự ưu tiên: Ưu tiên 1, Ưu tiên 2, Ưu tiên 3.'),
    ('S2_RENT','CONTENT','S2_Q7','Câu 7. Nhu cầu về diện tích căn hộ','SINGLE_CHOICE',TRUE,7,NULL),
    ('S2_RENT','CONTENT','S2_Q8','Câu 8. Nhu cầu về giá thuê nhà ở xã hội, nhà lưu trú công nhân trong khu công nghiệp?','SINGLE_CHOICE',TRUE,8,'Chưa bao gồm chi phí quản lý vận hành nhà ở.')
),
inserted AS (
  INSERT INTO questions (page_id, question_text, question_type_id, is_required, order_index, description)
  SELECT sp.id, d.question_text, qt.id, d.is_required, d.order_index, d.description
  FROM data d
  JOIN _seed_pages sp ON sp.survey_key = d.survey_key AND sp.page_key = d.page_key
  JOIN question_types qt ON qt.code = d.type_code
  WHERE NOT EXISTS (
    SELECT 1 FROM questions q
    WHERE q.page_id = sp.id AND q.order_index = d.order_index
  )
  RETURNING id, page_id, question_text, order_index
)
INSERT INTO _seed_questions (survey_key, question_key, id)
SELECT d.survey_key, d.question_key, q.id
FROM data d
JOIN _seed_pages sp ON sp.survey_key = d.survey_key AND sp.page_key = d.page_key
JOIN questions q ON q.page_id = sp.id AND q.order_index = d.order_index;

WITH data(survey_key, question_key, option_key, option_text, order_index) AS (
  VALUES
    ('S1_BUY','S1_Q1','S1_Q1_O1','Người có công với cách mạng, thân nhân liệt sĩ',1),
    ('S1_BUY','S1_Q1','S1_Q1_O2','Hộ gia đình nghèo, cận nghèo',2),
    ('S1_BUY','S1_Q1','S1_Q1_O3','Người thu nhập thấp khu vực đô thị',3),
    ('S1_BUY','S1_Q1','S1_Q1_O4','Công nhân, người lao động đang làm việc trong và ngoài khu công nghiệp',4),
    ('S1_BUY','S1_Q1','S1_Q1_O5','Cán bộ, chiến sỹ lực lượng vũ trang',5),
    ('S1_BUY','S1_Q1','S1_Q1_O6','Cán bộ, công chức, viên chức',6),
    ('S1_BUY','S1_Q1','S1_Q1_O7','Hộ gia đình, cá nhân thuộc trường hợp bị thu hồi đất và phải giải tỏa, phá dỡ nhà ở nhưng chưa được bồi thường bằng nhà ở, đất ở',7),
    ('S1_BUY','S1_Q2','S1_Q2_O1','Dưới 35 tuổi',1),
    ('S1_BUY','S1_Q2','S1_Q2_O2','Từ 35 đến 50 tuổi',2),
    ('S1_BUY','S1_Q2','S1_Q2_O3','Trên 50 tuổi',3),
    ('S1_BUY','S1_Q3','S1_Q3_O1','Độc thân',1),
    ('S1_BUY','S1_Q3','S1_Q3_O2','Kết hôn',2),
    ('S1_BUY','S1_Q4A','S1_Q4A_O1','Dưới 10 triệu đồng/tháng',1),
    ('S1_BUY','S1_Q4A','S1_Q4A_O2','Từ 10 đến dưới 15 triệu đồng/tháng',2),
    ('S1_BUY','S1_Q4A','S1_Q4A_O3','Từ 15 triệu đến 20 triệu đồng/tháng',3),
    ('S1_BUY','S1_Q4A','S1_Q4A_O4','Từ 20 đến 30 triệu đồng/tháng và đang nuôi con dưới tuổi thành niên',4),
    ('S1_BUY','S1_Q4A','S1_Q4A_O5','Là cán bộ, chiến sỹ lực lượng vũ trang và thu nhập bình quân/tháng không quá tổng thu nhập của sỹ quan có cấp bậc hàm Đại tá',5),
    ('S1_BUY','S1_Q4B','S1_Q4B_O1','Tổng thu nhập vợ, chồng dưới 20 triệu đồng/tháng',1),
    ('S1_BUY','S1_Q4B','S1_Q4B_O2','Tổng thu nhập vợ, chồng từ 20 đến dưới 30 triệu đồng/tháng',2),
    ('S1_BUY','S1_Q4B','S1_Q4B_O3','Tổng thu nhập vợ, chồng từ 30 đến 40 triệu đồng/tháng',3),
    ('S1_BUY','S1_Q4B','S1_Q4B_O4','Cả vợ và chồng đều là cán bộ, chiến sỹ lực lượng vũ trang và tổng thu nhập/tháng không quá 2,0 lần tổng thu nhập của sỹ quan có cấp bậc hàm Đại tá',4),
    ('S1_BUY','S1_Q4B','S1_Q4B_O5','Chỉ có vợ hoặc chồng là cán bộ, chiến sỹ lực lượng vũ trang và tổng thu nhập/tháng không quá 1,5 lần tổng thu nhập của sỹ quan có cấp bậc hàm Đại tá',5),
    ('S1_BUY','S1_Q5','S1_Q5_O1','Khu vực thành phố Đà Nẵng (trước hợp nhất)',1),
    ('S1_BUY','S1_Q5','S1_Q5_O2','Khu vực tỉnh Quảng Nam (trước hợp nhất)',2),
    ('S1_BUY','S1_Q5','S1_Q5_O3','Địa phương khác',3),
    ('S1_BUY','S1_Q6A','S1_Q6A_O1','Chưa có nhà ở thuộc sở hữu của mình',1),
    ('S1_BUY','S1_Q6A','S1_Q6A_O2','Có nhà ở thuộc sở hữu của mình nhưng diện tích nhà ở bình quân đầu người thấp hơn 15m2 sàn/người',2),
    ('S1_BUY','S1_Q6A','S1_Q6A_O3','Có nhà ở thuộc sở hữu của mình nhưng cách xa địa điểm làm việc',3),
    ('S1_BUY','S1_Q6A','S1_Q6A_O4','Có nhà ở thuộc sở hữu của mình tại khu vực thành phố Đà Nẵng (trước hợp nhất) và muốn mua nhà ở xã hội tại khu vực tỉnh Quảng Nam (trước hợp nhất)',4),
    ('S1_BUY','S1_Q6A','S1_Q6A_O5','Có nhà ở thuộc sở hữu của mình tại khu vực tỉnh Quảng Nam (trước hợp nhất) và muốn mua nhà ở xã hội tại khu vực thành phố Đà Nẵng (trước hợp nhất)',5),
    ('S1_BUY','S1_Q6B','S1_Q6B_O1','Chưa có nhà ở thuộc sở hữu của mình tại thành phố Đà Nẵng',1),
    ('S1_BUY','S1_Q6B','S1_Q6B_O2','Có nhà ở thuộc sở hữu của mình tại khu vực thành phố Đà Nẵng (trước hợp nhất) và muốn mua nhà ở xã hội tại khu vực tỉnh Quảng Nam (trước hợp nhất)',2),
    ('S1_BUY','S1_Q6B','S1_Q6B_O3','Có nhà ở thuộc sở hữu của mình tại khu vực tỉnh Quảng Nam (trước hợp nhất) và muốn mua nhà ở xã hội tại khu vực thành phố Đà Nẵng (trước hợp nhất)',3),
    ('S1_BUY','S1_Q7','AREA_1','Khu vực các phường: Liên Chiểu, Hòa Khánh, Hải Vân và lân cận',1),
    ('S1_BUY','S1_Q7','AREA_2','Khu vực các phường: An Khê, Cẩm Lệ, Hòa Xuân và lân cận',2),
    ('S1_BUY','S1_Q7','AREA_3','Khu vực các phường: An Hải, Sơn Trà, Ngũ Hành Sơn và lân cận',3),
    ('S1_BUY','S1_Q7','AREA_4','Khu vực các xã: Hòa Vang, Hòa Tiến, Bà Nà và lân cận',4),
    ('S1_BUY','S1_Q7','AREA_5','Khu vực Khu kinh tế mở Chu Lai (thuộc địa bàn các phường, xã: Núi Thành, Tam Anh và lân cận)',5),
    ('S1_BUY','S1_Q7','AREA_6','Khu vực các phường: Tam Kỳ, Quảng Phú, Bàn Thạch, Hương Trà',6),
    ('S1_BUY','S1_Q7','AREA_7','Khu vực các phường: Hội An, Hội An Đông, Hội An Tây',7),
    ('S1_BUY','S1_Q7','AREA_8','Khu vực Khu công nghiệp Điện Nam - Điện Ngọc (thuộc địa bàn các phường, xã: Điện Bàn, Điện Bàn Đông, An Thắng và lân cận)',8),
    ('S1_BUY','S1_Q7','AREA_9','Khu vực khác (đề nghị ghi rõ tên phường, xã)',9),
    ('S1_BUY','S1_Q8','S1_Q8_O1','Căn hộ 01 phòng ngủ từ 35 đến dưới 50 m2',1),
    ('S1_BUY','S1_Q8','S1_Q8_O2','Căn hộ 02 hoặc 03 phòng ngủ từ 50 đến 77 m2',2),
    ('S1_BUY','S1_Q9','S1_Q9_O1','Nhỏ hơn 15 triệu đồng/m2',1),
    ('S1_BUY','S1_Q9','S1_Q9_O2','Từ 15 triệu đồng đến dưới 20 triệu đồng/m2',2),
    ('S1_BUY','S1_Q9','S1_Q9_O3','Từ 20 triệu đồng đến 25 triệu đồng/m2',3),
    ('S1_BUY','S1_Q10','S1_Q10_O1','Vay dưới 30% giá trị hợp đồng mua, bán nhà ở xã hội',1),
    ('S1_BUY','S1_Q10','S1_Q10_O2','Vay từ 30% - dưới 50% giá trị hợp đồng mua, bán nhà ở xã hội',2),
    ('S1_BUY','S1_Q10','S1_Q10_O3','Vay từ 50% - tối đa 80% giá trị hợp đồng mua, bán nhà ở xã hội',3),
    ('S1_BUY','S1_Q10','S1_Q10_O4','Không có nhu cầu',4),

    ('S2_RENT','S2_Q1','S2_Q1_O1','Người có công với cách mạng, thân nhân liệt sĩ',1),
    ('S2_RENT','S2_Q1','S2_Q1_O2','Hộ gia đình nghèo, cận nghèo',2),
    ('S2_RENT','S2_Q1','S2_Q1_O3','Người thu nhập thấp khu vực đô thị',3),
    ('S2_RENT','S2_Q1','S2_Q1_O4','Công nhân, người lao động đang làm việc trong khu công nghiệp',4),
    ('S2_RENT','S2_Q1','S2_Q1_O5','Công nhân, người lao động đang làm việc ngoài khu công nghiệp',5),
    ('S2_RENT','S2_Q1','S2_Q1_O6','Cán bộ, chiến sỹ lực lượng vũ trang',6),
    ('S2_RENT','S2_Q1','S2_Q1_O7','Cán bộ, công chức, viên chức',7),
    ('S2_RENT','S2_Q1','S2_Q1_O8','Hộ gia đình, cá nhân thuộc trường hợp bị thu hồi đất và phải giải tỏa, phá dỡ nhà ở nhưng chưa được bồi thường bằng nhà ở, đất ở',8),
    ('S2_RENT','S2_Q1','S2_Q1_O9','Học sinh, sinh viên',9),
    ('S2_RENT','S2_Q2','S2_Q2_O1','Dưới 35 tuổi',1),
    ('S2_RENT','S2_Q2','S2_Q2_O2','Từ 35 đến 50 tuổi',2),
    ('S2_RENT','S2_Q2','S2_Q2_O3','Trên 50 tuổi',3),
    ('S2_RENT','S2_Q3','S2_Q3_O1','Độc thân',1),
    ('S2_RENT','S2_Q3','S2_Q3_O2','Kết hôn',2),
    ('S2_RENT','S2_Q4','S2_Q4_O1','Khu vực thành phố Đà Nẵng (trước hợp nhất)',1),
    ('S2_RENT','S2_Q4','S2_Q4_O2','Khu vực tỉnh Quảng Nam (trước hợp nhất)',2),
    ('S2_RENT','S2_Q4','S2_Q4_O3','Địa phương khác',3),
    ('S2_RENT','S2_Q5','S2_Q5_O1','Thuê nhà ở xã hội',1),
    ('S2_RENT','S2_Q5','S2_Q5_O2','Thuê nhà lưu trú công nhân trong khu công nghiệp',2),
    ('S2_RENT','S2_Q6','S2_AREA_1','Khu vực các phường: Liên Chiểu, Hòa Khánh, Hải Vân và lân cận',1),
    ('S2_RENT','S2_Q6','S2_AREA_2','Khu vực các phường: An Khê, Cẩm Lệ, Hòa Xuân và lân cận',2),
    ('S2_RENT','S2_Q6','S2_AREA_3','Khu vực các phường: An Hải, Sơn Trà, Ngũ Hành Sơn và lân cận',3),
    ('S2_RENT','S2_Q6','S2_AREA_4','Khu vực các xã: Hòa Vang, Hòa Tiến, Bà Nà và lân cận',4),
    ('S2_RENT','S2_Q6','S2_AREA_5','Khu vực Khu kinh tế mở Chu Lai (thuộc địa bàn các phường, xã: Núi Thành, Tam Anh và lân cận)',5),
    ('S2_RENT','S2_Q6','S2_AREA_6','Khu vực các phường: Tam Kỳ, Quảng Phú, Bàn Thạch, Hương Trà',6),
    ('S2_RENT','S2_Q6','S2_AREA_7','Khu vực các phường: Hội An, Hội An Đông, Hội An Tây',7),
    ('S2_RENT','S2_Q6','S2_AREA_8','Khu vực Khu công nghiệp Điện Nam - Điện Ngọc (thuộc địa bàn các phường, xã: Điện Bàn, Điện Bàn Đông, An Thắng và lân cận)',8),
    ('S2_RENT','S2_Q6','S2_AREA_9','Khu vực khác (đề nghị ghi rõ tên phường, xã)',9),
    ('S2_RENT','S2_Q7','S2_Q7_O1','Căn hộ 01 phòng ngủ từ 35 đến dưới 50 m2',1),
    ('S2_RENT','S2_Q7','S2_Q7_O2','Căn hộ 02 hoặc 03 phòng ngủ từ 50 đến 77 m2',2),
    ('S2_RENT','S2_Q8','S2_Q8_O1','Từ 54.000 - 100.000 đồng/m2/tháng',1),
    ('S2_RENT','S2_Q8','S2_Q8_O2','Từ 100.000 - 160.000 đồng/m2/tháng',2)
),
inserted AS (
  INSERT INTO options (question_id, option_text, order_index)
  SELECT sq.id, d.option_text, d.order_index
  FROM data d
  JOIN _seed_questions sq ON sq.survey_key = d.survey_key AND sq.question_key = d.question_key
  WHERE NOT EXISTS (
    SELECT 1 FROM options o
    WHERE o.question_id = sq.id AND o.order_index = d.order_index
  )
  RETURNING id, question_id, option_text, order_index
)
INSERT INTO _seed_options (survey_key, option_key, id)
SELECT d.survey_key, d.option_key, o.id
FROM data d
JOIN _seed_questions sq ON sq.survey_key = d.survey_key AND sq.question_key = d.question_key
JOIN options o ON o.question_id = sq.id AND o.order_index = d.order_index;

-- Branching conditions for survey 1.
-- Q1 options 1, 2 and 7 in the PDF say "không trả lời Câu 4a, 4b".
INSERT INTO conditions (source_question_id, source_value, target_question_id, action)
SELECT source_q.id, string_agg(source_o.id::TEXT, ',' ORDER BY source_o.id), target_q.id, 'HIDE'
FROM _seed_questions source_q
JOIN _seed_options source_o ON source_o.survey_key = 'S1_BUY'
  AND source_o.option_key IN ('S1_Q1_O1','S1_Q1_O2','S1_Q1_O7')
JOIN _seed_questions target_q ON target_q.survey_key = 'S1_BUY'
  AND target_q.question_key IN ('S1_Q4A','S1_Q4B')
WHERE source_q.survey_key = 'S1_BUY' AND source_q.question_key = 'S1_Q1'
GROUP BY source_q.id, target_q.id
HAVING NOT EXISTS (
  SELECT 1 FROM conditions c
  WHERE c.source_question_id = source_q.id
    AND c.target_question_id = target_q.id
    AND c.action = 'HIDE'
);

-- Q3: Độc thân -> Q4a, Kết hôn -> Q4b.
INSERT INTO conditions (source_question_id, source_value, target_question_id, action)
SELECT q3.id, o.id::TEXT, q4a.id, 'SHOW'
FROM _seed_questions q3
JOIN _seed_options o ON o.survey_key = 'S1_BUY' AND o.option_key = 'S1_Q3_O1'
JOIN _seed_questions q4a ON q4a.survey_key = 'S1_BUY' AND q4a.question_key = 'S1_Q4A'
WHERE q3.survey_key = 'S1_BUY' AND q3.question_key = 'S1_Q3'
  AND NOT EXISTS (
    SELECT 1 FROM conditions c
    WHERE c.source_question_id = q3.id AND c.target_question_id = q4a.id AND c.action = 'SHOW'
  );

INSERT INTO conditions (source_question_id, source_value, target_question_id, action)
SELECT q3.id, o.id::TEXT, q4b.id, 'SHOW'
FROM _seed_questions q3
JOIN _seed_options o ON o.survey_key = 'S1_BUY' AND o.option_key = 'S1_Q3_O2'
JOIN _seed_questions q4b ON q4b.survey_key = 'S1_BUY' AND q4b.question_key = 'S1_Q4B'
WHERE q3.survey_key = 'S1_BUY' AND q3.question_key = 'S1_Q3'
  AND NOT EXISTS (
    SELECT 1 FROM conditions c
    WHERE c.source_question_id = q3.id AND c.target_question_id = q4b.id AND c.action = 'SHOW'
  );

-- Q5: Da Nang/Quang Nam -> Q6a, other locality -> Q6b.
INSERT INTO conditions (source_question_id, source_value, target_question_id, action)
SELECT q5.id, string_agg(o.id::TEXT, ',' ORDER BY o.id), q6a.id, 'SHOW'
FROM _seed_questions q5
JOIN _seed_options o ON o.survey_key = 'S1_BUY' AND o.option_key IN ('S1_Q5_O1','S1_Q5_O2')
JOIN _seed_questions q6a ON q6a.survey_key = 'S1_BUY' AND q6a.question_key = 'S1_Q6A'
WHERE q5.survey_key = 'S1_BUY' AND q5.question_key = 'S1_Q5'
GROUP BY q5.id, q6a.id
HAVING NOT EXISTS (
  SELECT 1 FROM conditions c
  WHERE c.source_question_id = q5.id AND c.target_question_id = q6a.id AND c.action = 'SHOW'
);

INSERT INTO conditions (source_question_id, source_value, target_question_id, action)
SELECT q5.id, o.id::TEXT, q6b.id, 'SHOW'
FROM _seed_questions q5
JOIN _seed_options o ON o.survey_key = 'S1_BUY' AND o.option_key = 'S1_Q5_O3'
JOIN _seed_questions q6b ON q6b.survey_key = 'S1_BUY' AND q6b.question_key = 'S1_Q6B'
WHERE q5.survey_key = 'S1_BUY' AND q5.question_key = 'S1_Q5'
  AND NOT EXISTS (
    SELECT 1 FROM conditions c
    WHERE c.source_question_id = q5.id AND c.target_question_id = q6b.id AND c.action = 'SHOW'
  );

COMMIT;
