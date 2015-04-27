CREATE TABLE objective8.writer_notes (
	_id              SERIAL PRIMARY KEY,
       	_created_at      timestamp DEFAULT current_timestamp,
	created_by_id    integer REFERENCES objective8.users (_id) NOT NULL,
	objective_id     integer REFERENCES objective8.objectives (_id) NOT NULL,
	note_on_id       integer NOT NULL,
	global_id        integer REFERENCES objective8.global_identifiers (_id) UNIQUE NOT NULL,
	note             json NOT NULL
);
