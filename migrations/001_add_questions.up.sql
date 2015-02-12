CREATE TABLE objective8.questions (
    _id           SERIAL PRIMARY KEY,
    _created_at   timestamp DEFAULT current_timestamp,
    created_by_id integer REFERENCES objective8.users (_id) NOT NULL,
    objective_id  integer REFERENCES objective8.objectives (_id) NOT NULL,
    question      json NOT NULL
);
