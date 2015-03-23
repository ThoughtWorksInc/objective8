BEGIN;

SET LOCAL SCHEMA 'objective8';

/* Remove foreign key constraint from objectives table */
ALTER TABLE objectives
  DROP CONSTRAINT objectives_global_id_fkey;

/* Remove global identifiers for objectives */
DELETE FROM global_identifiers USING objectives
  WHERE global_identifiers._id = objectives.global_id;

/* Drop global_id column from objectives */
ALTER TABLE objectives
  DROP COLUMN global_id;

COMMIT;
