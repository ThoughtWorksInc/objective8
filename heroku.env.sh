#!/usr/bin/env bash

HEROKU_POSTGRES_URL=$DATABASE_URL

DB_URL=`echo $HEROKU_POSTGRES_URL | cut -d@ -f2`
DB_CONNECTION=`echo $HEROKU_POSTGRES_URL | cut -d@ -f1`

export BASE_URI="objective8.herokuapp.com"
export APP_PORT=$PORT
export DB_USER=`echo $DB_CONNECTION | cut -d: -f2 | cut -c 3-`
export DB_PASSWORD=`echo $DB_CONNECTION | cut -d: -f3`
export DB_HOST=`echo $DB_URL | cut -d: -f1`
export DB_PORT=`echo $DB_URL | cut -d/ -f1 | cut -d: -f2`
export DB_NAME=`echo $DB_URL | cut -d/ -f2`
export DB_JDBC_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}?user=${DB_USER}&password=${DB_PASSWORD}"

