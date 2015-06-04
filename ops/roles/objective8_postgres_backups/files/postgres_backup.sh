#!/bin/bash

mkdir -p /usr/local/objective8/backups
pg_dump -U objective8 -h localhost > /usr/local/objective8/backups/backup_$(date +"%Y%m%d%H%M").bak
