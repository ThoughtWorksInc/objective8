CREATE SCHEMA policy_drafting;

CREATE TABLE policy_drafting.objectives (
    _id         SERIAL PRIMARY KEY,
    created_by  varchar NOT NULL,
    objective   json NOT NULL
);

