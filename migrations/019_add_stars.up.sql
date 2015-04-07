CREATE TABLE objective8.stars (
	_id SERIAL PRIMARY KEY,
	_created_at timestamp DEFAULT current_timestamp,
	objective_id integer REFERENCES objective8.objectives (_id) NOT NULL,
	created_by_id integer REFERENCES objective8.users (_id) NOT NULL,
	active boolean NOT NULL DEFAULT true);
