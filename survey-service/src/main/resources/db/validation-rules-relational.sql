BEGIN;

ALTER TABLE surveys
    ADD COLUMN IF NOT EXISTS validation_revision BIGINT NOT NULL DEFAULT 1;

ALTER TABLE answers
    ALTER COLUMN answer_number TYPE NUMERIC
    USING answer_number::numeric;

CREATE TABLE IF NOT EXISTS validation_rule_types (
    code VARCHAR(64) PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_rule_type_code CHECK (code ~ '^[A-Z][A-Z0-9_]*$')
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_question_types_code
    ON question_types(code);

CREATE TABLE IF NOT EXISTS validation_rule_type_question_types (
    rule_code VARCHAR(64) NOT NULL,
    question_type_code VARCHAR(64) NOT NULL,
    PRIMARY KEY (rule_code, question_type_code),
    CONSTRAINT fk_validation_rule_type_question_types_rule
        FOREIGN KEY (rule_code)
        REFERENCES validation_rule_types(code)
        ON DELETE CASCADE,
    CONSTRAINT fk_validation_rule_type_question_types_question_type
        FOREIGN KEY (question_type_code)
        REFERENCES question_types(code)
        ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_validation_rule_type_question_type
    ON validation_rule_type_question_types(question_type_code);

CREATE TABLE IF NOT EXISTS question_validation_rules (
    question_id UUID NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    error_message TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    order_index INTEGER NOT NULL DEFAULT 0 CHECK (order_index >= 0),
    PRIMARY KEY (question_id, rule_code),
    CONSTRAINT fk_question_validation_rules_question
        FOREIGN KEY (question_id)
        REFERENCES questions(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_question_validation_rules_type
        FOREIGN KEY (rule_code)
        REFERENCES validation_rule_types(code)
        ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_question_validation_rule_code
    ON question_validation_rules(rule_code);

CREATE TABLE IF NOT EXISTS question_validation_rule_values (
    question_id UUID NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    parameter_name VARCHAR(64) NOT NULL DEFAULT 'value',
    group_index INTEGER NOT NULL DEFAULT 0 CHECK (group_index >= 0),
    position INTEGER NOT NULL DEFAULT 0 CHECK (position >= 0),
    text_value TEXT,
    number_value NUMERIC,
    date_value DATE,
    option_id UUID,
    PRIMARY KEY (question_id, rule_code, parameter_name, group_index, position),
    CONSTRAINT fk_validation_rule_values_rule
        FOREIGN KEY (question_id, rule_code)
        REFERENCES question_validation_rules(question_id, rule_code)
        ON DELETE CASCADE,
    CONSTRAINT fk_validation_rule_values_option
        FOREIGN KEY (option_id)
        REFERENCES options(id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_rule_parameter_name
        CHECK (parameter_name ~ '^[a-z][a-z0-9_]*$'),
    CONSTRAINT chk_rule_value_exactly_one CHECK (
        num_nonnulls(text_value, number_value, date_value, option_id) = 1
    )
);

CREATE INDEX IF NOT EXISTS idx_validation_values_option
    ON question_validation_rule_values(option_id)
    WHERE option_id IS NOT NULL;

INSERT INTO validation_rule_types (code, name, description, is_active)
VALUES
    ('MIN', 'Gia tri nho nhat', 'NUMBER: gia tri nho nhat, bao gom bien', TRUE),
    ('MAX', 'Gia tri lon nhat', 'NUMBER: gia tri lon nhat, bao gom bien', TRUE),
    ('GREATER_THAN', 'Lon hon', 'NUMBER: phai lon hon gia tri', TRUE),
    ('LESS_THAN', 'Nho hon', 'NUMBER: phai nho hon gia tri', TRUE),
    ('STEP', 'Buoc gia tri', 'NUMBER: buoc gia tri voi step va base', TRUE),
    ('ALLOWED_VALUES', 'Gia tri duoc phep', 'NUMBER: danh sach gia tri duoc phep', TRUE),
    ('DISALLOWED_VALUES', 'Gia tri bi cam', 'NUMBER: danh sach gia tri bi cam', TRUE),
    ('MIN_LENGTH', 'Do dai toi thieu', 'TEXT: do dai toi thieu', TRUE),
    ('MAX_LENGTH', 'Do dai toi da', 'TEXT: do dai toi da', TRUE),
    ('REGEX', 'Bieu thuc chinh quy', 'TEXT: Java Pattern full match', TRUE),
    ('EMAIL', 'Email', 'TEXT: dinh dang email', TRUE),
    ('PHONE', 'So dien thoai', 'TEXT: so dien thoai theo quoc gia', TRUE),
    ('URL', 'URL', 'TEXT: URL HTTP/HTTPS co host', TRUE),
    ('CHARACTER_SET', 'Bo ky tu', 'TEXT: DIGITS, LETTERS, ALPHANUMERIC, VIETNAMESE_LETTERS', TRUE),
    ('DIACRITICS', 'Dau tieng Viet', 'TEXT: ALLOW, FORBID, REQUIRE', TRUE),
    ('WHITESPACE', 'Khoang trang', 'TEXT: FORBID, SPACE_ONLY, ALLOW', TRUE),
    ('STARTS_WITH', 'Bat dau bang', 'TEXT: bat dau bang chuoi literal', TRUE),
    ('ENDS_WITH', 'Ket thuc bang', 'TEXT: ket thuc bang chuoi literal', TRUE),
    ('MUST_CONTAIN', 'Phai chua', 'TEXT: phai chua tat ca chuoi', TRUE),
    ('NOT_CONTAIN', 'Khong duoc chua', 'TEXT: khong duoc chua chuoi bi cam', TRUE),
    ('FIXED_OPTION', 'Co dinh lua chon', 'SINGLE_CHOICE: khoa mot option', TRUE),
    ('ALLOWED_OPTIONS', 'Lua chon duoc phep', 'CHOICE: danh sach option duoc phep', TRUE),
    ('DISALLOWED_OPTIONS', 'Lua chon bi cam', 'CHOICE: danh sach option bi cam', TRUE),
    ('MIN_SELECTIONS', 'So lua chon toi thieu', 'MULTIPLE_CHOICE: so option toi thieu', TRUE),
    ('MAX_SELECTIONS', 'So lua chon toi da', 'MULTIPLE_CHOICE: so option toi da', TRUE),
    ('REQUIRED_OPTIONS', 'Lua chon bat buoc', 'MULTIPLE_CHOICE: option bat buoc co khi tra loi', TRUE),
    ('AT_MOST_ONE_OF', 'Toi da mot trong nhom', 'MULTIPLE_CHOICE: moi group toi da mot option', TRUE),
    ('EXCLUSIVE_OPTIONS', 'Lua chon doc quyen', 'MULTIPLE_CHOICE: option doc quyen khong chon cung option khac', TRUE),
    ('MIN_DATE', 'Ngay nho nhat', 'DATE: ngay nho nhat', TRUE),
    ('MAX_DATE', 'Ngay lon nhat', 'DATE: ngay lon nhat', TRUE),
    ('BEFORE_TODAY', 'Truoc hom nay', 'DATE: phai truoc hom nay', TRUE),
    ('AFTER_TODAY', 'Sau hom nay', 'DATE: phai sau hom nay', TRUE),
    ('NOT_FUTURE', 'Khong trong tuong lai', 'DATE: khong lon hon hom nay', TRUE),
    ('NOT_PAST', 'Khong trong qua khu', 'DATE: khong nho hon hom nay', TRUE),
    ('MIN_AGE', 'Tuoi toi thieu', 'DATE: tuoi toi thieu tinh theo nam da hoan thanh', TRUE),
    ('MAX_AGE', 'Tuoi toi da', 'DATE: tuoi toi da tinh theo nam da hoan thanh', TRUE),
    ('ALLOWED_WEEKDAYS', 'Thu duoc phep', 'DATE: thu Hai=1 den Chu Nhat=7', TRUE),
    ('ALLOWED_DATES', 'Ngay duoc phep', 'DATE: danh sach ngay duoc phep', TRUE),
    ('DISALLOWED_DATES', 'Ngay bi cam', 'DATE: danh sach ngay bi cam', TRUE),
    ('REQUIRE_PROVINCE', 'Bat buoc tinh/thanh', 'ADDRESS: bat buoc tinh/thanh', TRUE),
    ('REQUIRE_WARD', 'Bat buoc xa/phuong', 'ADDRESS: bat buoc xa/phuong', TRUE),
    ('REQUIRE_DETAIL', 'Bat buoc dia chi chi tiet', 'ADDRESS: bat buoc dia chi chi tiet', TRUE),
    ('FIXED_PROVINCE', 'Co dinh tinh/thanh', 'ADDRESS: khoa ten tinh/thanh', TRUE),
    ('FIXED_WARD', 'Co dinh xa/phuong', 'ADDRESS: khoa cap tinh/phuong', TRUE),
    ('ALLOWED_PROVINCES', 'Tinh/thanh duoc phep', 'ADDRESS: danh sach tinh/thanh duoc phep', TRUE),
    ('DISALLOWED_PROVINCES', 'Tinh/thanh bi cam', 'ADDRESS: danh sach tinh/thanh bi cam', TRUE),
    ('ALLOWED_WARDS', 'Xa/phuong duoc phep', 'ADDRESS: danh sach cap tinh/phuong duoc phep', TRUE),
    ('DISALLOWED_WARDS', 'Xa/phuong bi cam', 'ADDRESS: danh sach cap tinh/phuong bi cam', TRUE),
    ('DETAIL_MIN_LENGTH', 'Do dai dia chi toi thieu', 'ADDRESS: do dai chi tiet toi thieu', TRUE),
    ('DETAIL_MAX_LENGTH', 'Do dai dia chi toi da', 'ADDRESS: do dai chi tiet toi da', TRUE),
    ('DETAIL_REGEX', 'Regex dia chi chi tiet', 'ADDRESS: Java Pattern full match cho chi tiet', TRUE)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description;

INSERT INTO validation_rule_type_question_types (rule_code, question_type_code)
VALUES
    ('MIN', 'NUMBER'),
    ('MAX', 'NUMBER'),
    ('GREATER_THAN', 'NUMBER'),
    ('LESS_THAN', 'NUMBER'),
    ('STEP', 'NUMBER'),
    ('ALLOWED_VALUES', 'NUMBER'),
    ('DISALLOWED_VALUES', 'NUMBER'),
    ('MIN_LENGTH', 'TEXT'),
    ('MAX_LENGTH', 'TEXT'),
    ('REGEX', 'TEXT'),
    ('EMAIL', 'TEXT'),
    ('PHONE', 'TEXT'),
    ('URL', 'TEXT'),
    ('CHARACTER_SET', 'TEXT'),
    ('DIACRITICS', 'TEXT'),
    ('WHITESPACE', 'TEXT'),
    ('STARTS_WITH', 'TEXT'),
    ('ENDS_WITH', 'TEXT'),
    ('MUST_CONTAIN', 'TEXT'),
    ('NOT_CONTAIN', 'TEXT'),
    ('FIXED_OPTION', 'SINGLE_CHOICE'),
    ('ALLOWED_OPTIONS', 'SINGLE_CHOICE'),
    ('ALLOWED_OPTIONS', 'MULTIPLE_CHOICE'),
    ('DISALLOWED_OPTIONS', 'SINGLE_CHOICE'),
    ('DISALLOWED_OPTIONS', 'MULTIPLE_CHOICE'),
    ('MIN_SELECTIONS', 'MULTIPLE_CHOICE'),
    ('MAX_SELECTIONS', 'MULTIPLE_CHOICE'),
    ('REQUIRED_OPTIONS', 'MULTIPLE_CHOICE'),
    ('AT_MOST_ONE_OF', 'MULTIPLE_CHOICE'),
    ('EXCLUSIVE_OPTIONS', 'MULTIPLE_CHOICE'),
    ('MIN_DATE', 'DATE'),
    ('MAX_DATE', 'DATE'),
    ('BEFORE_TODAY', 'DATE'),
    ('AFTER_TODAY', 'DATE'),
    ('NOT_FUTURE', 'DATE'),
    ('NOT_PAST', 'DATE'),
    ('MIN_AGE', 'DATE'),
    ('MAX_AGE', 'DATE'),
    ('ALLOWED_WEEKDAYS', 'DATE'),
    ('ALLOWED_DATES', 'DATE'),
    ('DISALLOWED_DATES', 'DATE'),
    ('REQUIRE_PROVINCE', 'ADDRESS'),
    ('REQUIRE_WARD', 'ADDRESS'),
    ('REQUIRE_DETAIL', 'ADDRESS'),
    ('FIXED_PROVINCE', 'ADDRESS'),
    ('FIXED_WARD', 'ADDRESS'),
    ('ALLOWED_PROVINCES', 'ADDRESS'),
    ('DISALLOWED_PROVINCES', 'ADDRESS'),
    ('ALLOWED_WARDS', 'ADDRESS'),
    ('DISALLOWED_WARDS', 'ADDRESS'),
    ('DETAIL_MIN_LENGTH', 'ADDRESS'),
    ('DETAIL_MAX_LENGTH', 'ADDRESS'),
    ('DETAIL_REGEX', 'ADDRESS')
ON CONFLICT (rule_code, question_type_code) DO NOTHING;

COMMIT;
