BEGIN;

SET LOCAL SCHEMA 'objective8';

/* Link objectives to global identifier */
ALTER TABLE objectives
  ADD COLUMN global_id integer;

ALTER TABLE global_identifiers
  ADD COLUMN temp_objective_id integer;

INSERT INTO global_identifiers (temp_objective_id)
  SELECT _id FROM objectives;

UPDATE objectives
  SET global_id = global_identifiers._id
  FROM global_identifiers
  WHERE global_identifiers.temp_objective_id = objectives._id;

ALTER TABLE objectives
  ALTER COLUMN global_id SET NOT NULL,
  ADD CONSTRAINT objectives_global_id_fkey
    FOREIGN KEY (global_id) REFERENCES global_identifiers (_id),
  ADD CONSTRAINT objectives_global_id_unique
    UNIQUE (global_id);

ALTER TABLE global_identifiers
  DROP COLUMN temp_objective_id;

COMMIT;
