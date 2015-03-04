BEGIN;

SET LOCAL SCHEMA 'objective8';

ALTER TYPE invitation_status RENAME TO temp_invitation_status;

CREATE TYPE invitation_status AS ENUM ('active', 'accepted', 'rejected', 'expired');
ALTER TABLE invitations RENAME COLUMN status TO _status;
ALTER TABLE invitations ADD COLUMN status invitation_status NOT NULL DEFAULT 'active';
UPDATE invitations SET status = 'rejected' WHERE _status = 'declined';
UPDATE invitations SET status = _status::text::invitation_status WHERE _status IN ('active', 'accepted', 'expired');
ALTER TABLE invitations DROP COLUMN _status;
DROP TYPE temp_invitation_status;

COMMIT;
