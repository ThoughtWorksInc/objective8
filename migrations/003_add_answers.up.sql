CREATE TABLE objective8.answers (
    _id           SERIAL PRIMARY KEY,
    _created_at   timestamp DEFAULT current_timestamp,
    created_by_id integer REFERENCES objective8.users (_id) NOT NULL,
    question_id   integer REFERENCES objective8.questions (_id) NOT NULL,
    answer        json NOT NULL
);
