BEGIN;

SET LOCAL SCHEMA 'objective8';

ALTER TABLE invitations
  DROP COLUMN status;

DROP TYPE invitation_status;

COMMIT;
