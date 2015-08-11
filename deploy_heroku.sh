#!/bin/bash

set -e

heroku git:remote --app objective8
heroku maintenance:on
heroku buildpacks:clear
heroku buildpacks:set https://github.com/heroku/heroku-buildpack-clojure
heroku buildpacks:add --index 1 https://github.com/heroku/heroku-buildpack-nodejs
git push heroku master
heroku maintenance:off

