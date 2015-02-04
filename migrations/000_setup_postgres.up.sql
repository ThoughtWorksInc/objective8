CREATE SCHEMA policy_drafting;

CREATE TABLE policy_drafting.objectives (
    _id         SERIAL PRIMARY KEY,
    _created_at timestamp DEFAULT current_timestamp,
    created_by  varchar NOT NULL,
    end_date    timestamp NOT NULL,
    objective   json NOT NULL
);

