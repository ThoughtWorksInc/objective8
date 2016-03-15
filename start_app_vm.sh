#!/usr/bin/env bash

grunt build
API_BEARER_NAME=abc \
API_BEARER_TOKEN=def \
BASE_URI=192.168.50.50:8080 \
TWITTER_CONSUMER_TOKEN="stub-token" \
TWITTER_CONSUMER_SECRET_TOKEN="stub-secret" \
lein repl