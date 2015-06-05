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
scp $TAR $REMOTE_USER@$SERVER_IP:~
ssh $REMOTE_USER@$SERVER_IP "tar -xvzf $TAR; cd $DIR; sudo bash init-script/remote_start_objective8.sh"
