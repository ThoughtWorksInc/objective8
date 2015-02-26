CREATE TABLE objective8.candidates (
    _id           SERIAL PRIMARY KEY,
    _created_at   timestamp DEFAULT current_timestamp,
    user_id       integer REFERENCES objective8.users (_id) NOT NULL,
    objective_id  integer REFERENCES objective8.objectives (_id) NOT NULL,
    invitation_id integer REFERENCES objective8.invitations (_id) NOT NULL,
    candidate     json NOT NULL
);
