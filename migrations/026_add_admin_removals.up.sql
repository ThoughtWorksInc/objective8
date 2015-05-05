CREATE TABLE objective8.admin_removals (
    _id           SERIAL PRIMARY KEY,
    _created_at   timestamp DEFAULT current_timestamp,   
    removal_uri   varchar NOT NULL,
    removed_by_id integer REFERENCES objective8.users (_id) NOT NULL
);
