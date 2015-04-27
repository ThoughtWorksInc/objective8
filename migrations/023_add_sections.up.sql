CREATE TABLE objective8.sections (
	_id              SERIAL PRIMARY KEY,
       	_created_at      timestamp DEFAULT current_timestamp,
	draft_id         integer REFERENCES objective8.drafts (_id) NOT NULL,
	global_id        integer REFERENCES objective8.global_identifiers (_id) UNIQUE NOT NULL,
	section_label    char(8) NOT NULL
);
