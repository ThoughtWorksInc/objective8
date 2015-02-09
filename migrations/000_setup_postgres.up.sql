CREATE SCHEMA policy_drafting;

CREATE TABLE policy_drafting.users (
    _id         SERIAL PRIMARY KEY,
    _created_at timestamp DEFAULT current_timestamp,
    twitter_id  varchar NOT NULL,
    user_data   json NOT NULL
);

CREATE TABLE policy_drafting.objectives (
    _id            SERIAL PRIMARY KEY,
    _created_at    timestamp DEFAULT current_timestamp,
    created_by_id  integer REFERENCES policy_drafting.users (_id) NOT NULL,
    end_date       timestamp NOT NULL,
    objective      json NOT NULL
);

CREATE TABLE policy_drafting.comments (
    _id           SERIAL PRIMARY KEY,
    _created_at   timestamp DEFAULT current_timestamp,
    created_by_id integer REFERENCES policy_drafting.users (_id) NOT NULL,
    root_id       integer NOT NULL,
    parent_id     integer NOT NULL,
    comment       json NOT NULL
);
