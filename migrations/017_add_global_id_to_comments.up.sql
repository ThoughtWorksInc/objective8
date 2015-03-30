BEGIN;

SET LOCAL SCHEMA 'objective8';

/* Link each comment to a global identifier */
ALTER TABLE comments
  ADD COLUMN global_id integer;

ALTER TABLE global_identifiers
  ADD COLUMN temp_comment_id integer;

INSERT INTO global_identifiers (temp_comment_id)
  SELECT _id FROM comments;

UPDATE comments
  SET global_id = global_identifiers._id
  FROM global_identifiers
  WHERE global_identifiers.temp_comment_id = comments._id;

ALTER TABLE comments
  ALTER COLUMN global_id SET NOT NULL,
  ADD CONSTRAINT comments_global_id_fkey
    FOREIGN KEY (global_id) REFERENCES global_identifiers (_id),
  ADD CONSTRAINT comments_global_id_unique
    UNIQUE (global_id);

ALTER TABLE global_identifiers
  DROP COLUMN temp_comment_id;

COMMIT;
