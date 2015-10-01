#!/bin/bash

npm install
lein do clean, uberjar
DIR=deploy_assets
TAR=deploy.tar.gz
mkdir -p $DIR
cp target/*-standalone.jar $DIR
cp -r resources/public $DIR
cp -r init-script $DIR
cp -r migrations $DIR
tar -cvzf $TAR $DIR

scp $TAR $REMOTE_USER@$SERVER_IP:/var/objective8
ssh $REMOTE_USER@$SERVER_IP <<EOF
   sudo docker stop objective8 || echo 'Failed to stop objective8 container'
   sudo docker rm objective8 || echo 'Failed to remove objective8 container'
   sudo docker run -d --name objective8 --env-file=/usr/local/objective8/objective8_config -p 8080:8080 -p 8081:8081 -v /var/objective8/$TAR:/$TAR java:8 bash -c 'tar -xvzf /$TAR; cd /$DIR; bash init-script/remote_start_objective8_docker.sh'
   rm /var/objective8/$TAR
EOF
rm -rf $DIR
lein do clean
