-- Upgrade script for Matrix questions, Question hierarchy (Parent-Child & Aggregation), and Page Condition

-- 1. Upgrade conditions to support target Page as well as Question
ALTER TABLE conditions 
    ADD COLUMN IF NOT EXISTS target_type VARCHAR(50) NOT NULL DEFAULT 'QUESTION',
    ALTER COLUMN target_question_id DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS target_page_id UUID REFERENCES pages(id) ON DELETE CASCADE;

-- 2. Upgrade questions to support hierarchy, aggregation, and matrix configuration
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS parent_id UUID REFERENCES questions(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS level INT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS path TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS is_leaf BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS aggregation_type VARCHAR(50) NOT NULL DEFAULT 'NONE',
    ADD COLUMN IF NOT EXISTS aggregation_formula TEXT,
    ADD COLUMN IF NOT EXISTS weight NUMERIC(10, 4) DEFAULT 1.0,
    ADD COLUMN IF NOT EXISTS matrix_config JSONB DEFAULT NULL;

-- 3. Upgrade answers to support matrix/table responses
ALTER TABLE answers
    ADD COLUMN IF NOT EXISTS matrix_data JSONB DEFAULT NULL;

-- 4. Seed question types for Matrix and Dynamic Table
INSERT INTO question_types (id, code, name) VALUES 
    (gen_random_uuid(), 'MATRIX_SINGLE', 'Ma trận chọn một đáp án'),
    (gen_random_uuid(), 'MATRIX_MULTI', 'Ma trận chọn nhiều đáp án'),
    (gen_random_uuid(), 'MATRIX_INPUT', 'Ma trận nhập số liệu'),
    (gen_random_uuid(), 'DYNAMIC_TABLE', 'Bảng danh sách động (thêm dòng)')
ON CONFLICT DO NOTHING;

