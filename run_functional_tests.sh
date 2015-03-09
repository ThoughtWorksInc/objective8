#!/usr/bin/env bash 
start-stop-daemon --start -b -x /usr/bin/Xvfb -- :1 -screen 0 1024x768x24
DISPLAY=:1 lein midje :filter functional
start-stop-daemon --stop -x /usr/bin/Xvfb
