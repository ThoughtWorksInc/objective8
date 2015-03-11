CREATE TABLE objective8.drafts (
    _id           SERIAL PRIMARY KEY,
    _created_at   timestamp DEFAULT current_timestamp,
    submitter_id  integer REFERENCES objective8.users (_id) NOT NULL,
    objective_id  integer REFERENCES objective8.objectives (_id) NOT NULL,
    draft         json NOT NULL
);
