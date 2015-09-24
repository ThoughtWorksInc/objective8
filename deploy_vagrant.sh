#!/bin/bash

REMOTE_USER=vagrant
SERVER_IP=192.168.50.52

npm install
lein do clean, uberjar
DIR=deploy_assets
TAR=deploy.tar.gz
mkdir -p $DIR
cp target/objective8-0.0.1-SNAPSHOT-standalone.jar $DIR
cp -r resources/public $DIR
cp -r init-script $DIR
cp -r migrations $DIR
tar -cvzf $TAR $DIR
ssh $REMOTE_USER@$SERVER_IP <<EOF
   sudo docker stop objective8 || echo 'Failed to stop objective8 container'
   sudo docker rm objective8 || echo 'Failed to remove objective8 container'
   sudo docker run -d --name objective8 --env-file=/usr/local/objective8/objective8_config -p 8080:8080 -v /var/objective8/$TAR:/$TAR java:8 bash -c 'tar -xvzf $TAR; cd $DIR; bash init-script/remote_start_objective8_docker.sh'
EOF
rm -rf $DIR
lein do clean
