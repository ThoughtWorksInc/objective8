#!/usr/bin/env bash 
JAVA_OPTS="-Dlog4j.configuration=log4j.test" \
lein midje :autotest test/objective8/unit/ test/objective8/integration/ src/
