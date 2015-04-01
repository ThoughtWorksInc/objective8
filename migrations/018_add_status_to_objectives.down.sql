BEGIN;
	
SET LOCAL SCHEMA 'objective8';
	
ALTER TABLE objectives
  DROP COLUMN status;

DROP TYPE objective_status;

COMMIT;
