CREATE TABLE objective8.invitations (
    _id           SERIAL PRIMARY KEY,
    _created_at   timestamp DEFAULT current_timestamp,
    invited_by_id integer REFERENCES objective8.users (_id) NOT NULL,
    objective_id  integer REFERENCES objective8.objectives (_id) NOT NULL,
    uuid          char(36) UNIQUE NOT NULL,
    invitation    json NOT NULL
);
