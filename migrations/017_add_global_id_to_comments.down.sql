BEGIN;

SET LOCAL SCHEMA 'objective8';

/* Remove foreign key constraint from comments table */
ALTER TABLE comments
  DROP CONSTRAINT comments_global_id_fkey;

/* Remove global identifiers for comments */
DELETE FROM global_identifiers USING comments
  WHERE global_identifiers._id = comments.global_id;

/* Drop global_id column from comments */
ALTER TABLE comments
  DROP COLUMN global_id;

COMMIT;
