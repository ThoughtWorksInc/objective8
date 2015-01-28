#!/bin/bash -e

trap 'killall' INT

killall() {
    trap '' INT TERM
    echo
    echo "shutting down..."
    kill -TERM 0
    wait
    echo "done!"
}


echo "[1] watching the scss  changes..."
grunt dev &

echo "[2] automatically running the server tests on changes..."
lein midje :autotest &

echo "[3] running the server..."
lein run &

wait

# Proper clean up taken from this unix stackexchange post:
# http://unix.stackexchange.com/questions/55558/how-can-i-kill-and-wait-for-background-processes-to-finish-in-a-shell-script-whe