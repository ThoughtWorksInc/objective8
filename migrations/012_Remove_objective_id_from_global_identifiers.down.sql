BEGIN;

SET LOCAL SCHEMA 'objective8';

ALTER TABLE global_identifiers
    ADD COLUMN objective_id integer;

UPDATE global_identifiers
  SET objective_id = answers.objective_id
  FROM answers
  WHERE answers.global_id = global_identifiers._id;

ALTER TABLE global_identifiers
  ADD CONSTRAINT global_identifiers_objective_id_not_null CHECK(objective_id IS NOT NULL);

COMMIT;
