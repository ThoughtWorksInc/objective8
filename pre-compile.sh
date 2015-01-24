#!/bin/bash -e

npm install
grunt build

lein do clean, midje