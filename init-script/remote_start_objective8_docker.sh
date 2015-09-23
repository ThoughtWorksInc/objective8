#!/bin/bash
# Set up static resources
mkdir -p /var/www/objective8
cp -r public /var/www/objective8

# Launch application
mkdir -p /usr/local/objective8
cp objective8-0.0.1-SNAPSHOT-standalone.jar /usr/local/objective8
cp -r migrations /usr/local/objective8
java -Dlog4j.configuration=log4j.debug -server -Xms256M -Xmx512M -XX:MaxPermSize=128M -jar /usr/local/objective8/*standalone.jar