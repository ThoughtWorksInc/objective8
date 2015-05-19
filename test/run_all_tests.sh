#!/usr/bin/env bash 
start-stop-daemon --start -b -x /usr/bin/Xvfb -- :1 -screen 0 1280x1024x16
JAVA_OPTS="-Dlog4j.configuration=log4j.test" \
API_BEARER_NAME=functionalTests \
API_BEARER_TOKEN=functionalTestsToken \
GA_TRACKING_ID="" \
DISPLAY=:1 lein do clean, midje $*
start-stop-daemon --stop -x /usr/bin/Xvfb
