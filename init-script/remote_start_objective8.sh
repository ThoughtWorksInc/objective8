#!/bin/bash
[ -f /etc/init.d/objective8d ] && sudo service objective8d stop || true
mkdir -p /usr/local/objective8
mv objective8-0.0.1-SNAPSHOT-standalone.jar /usr/local/objective8
mv migrations /usr/local/objective8
mv objective8_config /etc/default
sudo mv objective8d /etc/init.d
sudo service objective8d start
