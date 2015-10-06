CREATE TABLE objective8.activities (
    _id           SERIAL PRIMARY KEY,
    _created_at   timestamp DEFAULT current_timestamp,
    activity      json NOT NULL
);
