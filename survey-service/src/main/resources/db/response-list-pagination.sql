-- Apply after schema-uuid.sql. Safe to rerun on existing PostgreSQL databases.
BEGIN;

CREATE INDEX IF NOT EXISTS idx_answers_response_question
  ON answers (response_id, question_id);

CREATE INDEX IF NOT EXISTS idx_answer_options_answer_option
  ON answer_options (answer_id, option_id);

COMMIT;
