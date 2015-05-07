BEGIN;

SET LOCAL SCHEMA 'objective8';

ALTER TABLE sections
  ADD COLUMN objective_id integer;

UPDATE sections
  SET objective_id = drafts.objective_id
  FROM drafts
  WHERE drafts._id = sections.draft_id;

ALTER TABLE sections
  ALTER COLUMN objective_id SET NOT NULL,
  ADD CONSTRAINT sections_objective_id_fkey
    FOREIGN KEY (objective_id) REFERENCES objectives (_id);

COMMIT;
