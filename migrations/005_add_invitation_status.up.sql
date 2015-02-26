BEGIN;

SET LOCAL SCHEMA 'objective8';

CREATE TYPE invitation_status AS ENUM ('active', 'accepted', 'rejected', 'expired');

ALTER TABLE invitations
  ADD COLUMN status invitation_status NOT NULL default 'active';

COMMIT;
