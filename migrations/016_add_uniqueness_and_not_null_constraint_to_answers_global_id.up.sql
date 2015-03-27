ALTER TABLE objective8.answers
  ALTER COLUMN global_id SET NOT NULL,
  ADD CONSTRAINT answers_global_id_unique
    UNIQUE (global_id);
