#!/usr/bin/env bash 
start-stop-daemon --start -b -x /usr/bin/Xvfb -- :1 -screen 0 1280x1024x16
API_BEARER_NAME=functionalTests \
API_BEARER_TOKEN=functionalTestsToken \
DISPLAY=:1 lein midje :autotest test/objective8/functional/ src/
start-stop-daemon --stop -x /usr/bin/Xvfb
