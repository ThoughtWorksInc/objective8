#!/bin/bash -e

PGPASSWORD=$DB_PASSWORD \
psql -U objective8 -h localhost -w -c \
"TRUNCATE TABLE objective8.bearer_tokens;
 INSERT INTO objective8.bearer_tokens (bearer_name, token_details)
 VALUES ('$API_BEARER_NAME', '{\"bearer-name\": \"$API_BEARER_NAME\", \"bearer-token\": \"$API_BEARER_TOKEN\"}');"
