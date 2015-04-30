CREATE TABLE objective8.admins (
    _id         SERIAL PRIMARY KEY,
    _created_at timestamp DEFAULT current_timestamp,   
    twitter_id  varchar NOT NULL
);
