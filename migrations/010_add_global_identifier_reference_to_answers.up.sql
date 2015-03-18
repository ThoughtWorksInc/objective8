BEGIN;

SET LOCAL SCHEMA 'objective8';

/* Add objective-id to answers table */
ALTER TABLE answers
  ADD COLUMN objective_id integer;

UPDATE answers
  SET objective_id = questions.objective_id
  FROM questions
  WHERE answers.question_id = questions._id;

ALTER TABLE answers
  ADD CONSTRAINT answers_objective_id_fkey
  FOREIGN KEY (objective_id) REFERENCES objectives (_id);

ALTER TABLE answers
  ADD CONSTRAINT objective_id_not_null CHECK(objective_id IS NOT NULL);

/* Link answer to global identifier */
ALTER TABLE answers
  ADD COLUMN global_id integer;

ALTER TABLE global_identifiers
  ADD COLUMN temp_answer_id integer;

INSERT INTO global_identifiers (temp_answer_id, objective_id)
  SELECT _id, objective_id FROM answers;

UPDATE answers
  SET global_id = global_identifiers._id
  FROM global_identifiers
  WHERE global_identifiers.temp_answer_id = answers._id;

ALTER TABLE answers
  ADD CONSTRAINT answers_global_id_fkey
  FOREIGN KEY (global_id) REFERENCES global_identifiers (_id);
  
ALTER TABLE global_identifiers
  DROP COLUMN temp_answer_id;

COMMIT;
