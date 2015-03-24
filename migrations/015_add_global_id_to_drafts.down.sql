BEGIN;

SET LOCAL SCHEMA 'objective8';

/* Remove foreign key constraint from drafts table */
ALTER TABLE drafts
  DROP CONSTRAINT drafts_global_id_fkey;

/* Remove global identifiers for objectives */
DELETE FROM global_identifiers USING drafts
  WHERE global_identifiers._id = drafts.global_id;

/* Drop global_id column from objectives */
ALTER TABLE drafts
  DROP COLUMN global_id;

COMMIT;
