#!/bin/bash

dir=/usr/local/objective8
cd $dir
mkdir -p $dir/backups
backup_path=$dir/backups/backup_$(date +"%Y%m%d%H%M").bak
PGPASSFILE=$dir/.pgpass pg_dump -U objective8 -h localhost > $backup_path

bucket_name=objective8_pg_db_backups
s3cmd mb s3://$bucket_name -c $dir/.s3cfg
s3cmd put $backup_path s3://$bucket_name -c $dir/.s3cfg
