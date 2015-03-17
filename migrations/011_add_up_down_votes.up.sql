CREATE TABLE objective8.up_down_votes (
    _id           SERIAL PRIMARY KEY,
    _created_at   timestamp DEFAULT current_timestamp,
    created_by_id integer REFERENCES objective8.users (_id) NOT NULL,
    global_id 	  integer REFERENCES objective8.global_identifiers (_id) NOT NULL,
    vote	  integer NOT NULL,
    CONSTRAINT valid_vote CHECK (vote <= 1 AND vote >= -1)
);
