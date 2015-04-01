BEGIN;

SET LOCAL SCHEMA 'objective8';

CREATE TYPE objective_status AS ENUM ('open', 'drafting', 'closed');

ALTER TABLE objectives
  ADD COLUMN status objective_status NOT NULL DEFAULT 'open'; 

COMMIT;
