#!/bin/bash

ssh $REMOTE_USER@$SERVER_IP <<EOF
   sudo docker stop objective8 || echo 'Failed to stop objective8 container'
   sudo docker rm objective8 || echo 'Failed to remove objective8 container'
   sudo docker rmi dcent/objective8 || echo 'Failed to remove objective8 image'
   sudo docker run -d --env-file=/usr/local/objective8/objective8_config -p 8080:8080 -p 8081:8081 --name objective8 --restart=on-failure dcent/objective8
EOF
