CREATE TABLE objective8.bearer_tokens (
    _id             SERIAL PRIMARY KEY,
    _created_at     timestamp DEFAULT current_timestamp,
    bearer_name     varchar NOT NULL UNIQUE,
    token_details   json NOT NULL
);
