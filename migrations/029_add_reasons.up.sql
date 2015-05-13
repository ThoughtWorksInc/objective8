BEGIN;

SET LOCAL SCHEMA 'objective8';

CREATE TYPE reason_type AS ENUM ('unclear', 'expand', 'suggestion', 'language', 'general');

CREATE TABLE reasons (
  _id         SERIAL PRIMARY KEY,
  _created_at timestamp DEFAULT current_timestamp,
  comment_id  integer REFERENCES comments (_id) NOT NULL,
  reason      reason_type NOT NULL DEFAULT 'general'
);

INSERT INTO reasons (comment_id)
  SELECT comments._id
  FROM comments
  JOIN sections
  ON comments.comment_on_id = sections.global_id;

COMMIT;

