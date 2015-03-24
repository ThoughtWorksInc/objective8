BEGIN;

SET LOCAL SCHEMA 'objective8';

/* Link each draft to a global identifier */
ALTER TABLE drafts
  ADD COLUMN global_id integer;

ALTER TABLE global_identifiers
  ADD COLUMN temp_draft_id integer;

INSERT INTO global_identifiers (temp_draft_id)
  SELECT _id FROM drafts;

UPDATE drafts
  SET global_id = global_identifiers._id
  FROM global_identifiers
  WHERE global_identifiers.temp_draft_id = drafts._id;

ALTER TABLE drafts
  ALTER COLUMN global_id SET NOT NULL,
  ADD CONSTRAINT drafts_global_id_fkey
    FOREIGN KEY (global_id) REFERENCES global_identifiers (_id),
  ADD CONSTRAINT drafts_global_id_unique
    UNIQUE (global_id);

ALTER TABLE global_identifiers
  DROP COLUMN temp_draft_id;

COMMIT;
