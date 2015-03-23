#!/usr/bin/env bash 
lein midje objective8.unit.* objective8.integration.* :autotest :filter -functional
