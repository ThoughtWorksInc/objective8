#!/usr/bin/env bash 
start-stop-daemon --start -b -x /usr/bin/Xvfb -- :1 -screen 0 1024x768x24
API_BEARER_NAME=functionalTests \
API_BEARER_TOKEN=functionalTestsToken \
DISPLAY=:1 lein midje :config midje/functional_tests.clj
start-stop-daemon --stop -x /usr/bin/Xvfb
