ALTER TABLE objective8.answers
  ALTER COLUMN global_id DROP NOT NULL,
  DROP CONSTRAINT answers_global_id_unique;
