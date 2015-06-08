#!/bin/bash
# Halt running instance
[ -f /etc/init.d/objective8d ] && sudo service objective8d stop || true

# Set up static resources
mkdir -p /var/www/objective8
cp -r public /var/www/objective8

# Launch application
mkdir -p /usr/local/objective8
cp objective8-0.0.1-SNAPSHOT-standalone.jar /usr/local/objective8
cp -r migrations /usr/local/objective8
sudo cp init-script/objective8d /etc/init.d
sudo service objective8d start
