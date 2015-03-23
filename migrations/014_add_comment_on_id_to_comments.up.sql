BEGIN;

SET LOCAL SCHEMA 'objective8';

ALTER TABLE comments
  ADD COLUMN comment_on_id integer REFERENCES global_identifiers (_id);

UPDATE comments
  SET comment_on_id = objectives.global_id
  FROM objectives
  WHERE objectives._id = comments.objective_id;

ALTER TABLE comments
  ALTER COLUMN comment_on_id SET NOT NULL;

COMMIT;
