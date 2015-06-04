#!/bin/bash
[ -f /etc/init.d/objective8d ] && sudo service objective8d stop || true
mkdir -p /usr/local/objective8
cp objective8-0.0.1-SNAPSHOT-standalone.jar /usr/local/objective8
cp -r migrations /usr/local/objective8
cp objective8_config /etc/default
sudo cp init-script/objective8d /etc/init.d
sudo service objective8d start
