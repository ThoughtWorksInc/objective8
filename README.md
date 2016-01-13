# Objective8

[![Build Status](https://snap-ci.com/d-cent/objective8/branch/master/build_image)](https://snap-ci.com/d-cent/objective8/branch/master)

## Getting Started

###Local setup
To start the development VM you will need to install
- Vagrant + Virtualbox (see https://www.vagrantup.com/downloads.html, https://www.virtualbox.org/wiki/Downloads)
- Ansible (see http://docs.ansible.com/intro_installation.html)

```
git clone git@github.com:ThoughtWorksInc/objective8.git
cd objective8/ops/
```

### Working on the VM
####To get started:

```
# use the provided template to create an objective8_config file
cp objective8_config_template objective8_config
vagrant up
# type 'vagrant' when asked for a sudoers password
vagrant ssh
cd /var/objective8
npm install
lein ragtime migrate
```

####Running the tests

######To run all tests:
```
test/run_all_tests.sh
```
######To run only unit tests:
```
lein midje objective8.unit.*
```
######To run only integration tests:
```
lein midje objective8.integration.*
```
######To run only functional tests:
```
test/run_functional_tests.sh
```

#####Use `:autotest` to make tests rerun automatically when files are changed:

*Autotest watches directories rather than namespaces*

######Autotest all tests:
```
test/run_all_tests.sh :autotest
```

######Autotest unit and integration tests:
```
test/autotest_unit_and_integration_tests.sh
```

######Autotest functional tests:
```
test/autotest_functional_tests.sh
```

######Extra configuration options:

You can also pass extra configuration to the tests using the :config
option.  For example: to run just the unit tests with autotest and without any logging, use:
```
lein midje :autotest test/objective8/unit/ :config config/tests/no-logging.clj
```


####Adding or updating a translation resource

Resources for translating the site into different languages are located under:
/resources/translations/<locale-identifier>.csv

To start translating into a new locale, or to update translations for
an existing locale, there is a helper leiningen task that copies keys
from the default en.csv template into a new or existing templates for
other locales.  This can be run using:
```
lein translation-template <locale-identifier> [& <locale-identifier>]
```
For example:
```
lein translation-template es el
```
will generate or update `/resources/translations/es.csv` and
`/resources/translations/el.csv`.

####Running the app

######Build front-end assets (html and css) using:
```
npm install
```

######Designing in the browser
This allows you to rapidly design.  You can create jade, modify the sass and js.  These will all be live reloaded.
```
grunt design
```

######Running the app with a fake twitter (used for Sign-in) 
```
lein repl
(reset :stub-twitter)
```

######Running the app with credentials
create a task (for example `start_with_credentials.sh` with the following content:

```
API_BEARER_NAME=<choose a bearer name>
API_BEARER_TOKEN=<choose a secure bearer token>
TWITTER_CONSUMER_TOKEN=<obtain this from twitter when registering the application to allow sign-in via twitter> \
TWITTER_CONSUMER_SECRET_TOKEN=<as above> \
STONECUTTER_CLIENT_ID=<this is from registering the application with a D-cent Stonecutter Oauth instance>  \
STONECUTTER_CLIENT_SECRET=<as above> \
STONECUTTER_AUTH_URL=<the full url of the Stonecutter Oauth instance that the application is registered with> \
BASE_URI=<either localhost or VM ip address and :APP_PORT> \
APP_PORT= <> \
DB_PORT= <> \
lein repl $*
```
then run the task and start the app using:
```
(reset :default)
```

##Deployment to Heroku

######Heroku account setup:
- Create a heroku account
- Create new heroku app
- Install heroku tool-belt

######Setup Twitter API for app:
- create new app on www.dev.twitter.com
- get consumer API key

######Local heroku configuration:
- add heroku remote: `heroku git:remote -a [APP_NAME]`
- Add heroku postgres add-on: `heroku addons:create heroku-postgresql:hobby-dev`

- Set Config Vars on Heroku
	- ADMIN = twitter-[twitter id of admin user]
  - BUILDPACK_URL = https://www.github.com/ddollar/heroku-buildpack-multi.git
  - BASE_URI = [app uri]
  - HTTPS_ONLY = true
  - SCHEDULER_INTERVAL_MINUTES = 10
  - TWITTER_CONSUMER_SECRET_TOKEN = [twitter consumer api secret]
  - TWITTER_CONSUMER_TOKEN = [twitter consumer api key]
  - API_BEARER_NAME = [anything]
  - API_BEARER_TOKEN = [anything]

- Push to Heroku


##Deployment to ubuntu server (e.g. through digital ocean)

All of the following steps are essential.

####Provision
- Provision an ubuntu server machine (can be a cloud server such as digital ocean)
- A machine with 1gb RAM and 30gb hard disk has been sufficient for early tests
- Enable connection to the box via ssh - [how to](https://www.digitalocean.com/community/tutorials/how-to-set-up-ssh-keys--2) 

####Configure with ansible
- Install Ansible
- In file *ops/digital_ocean_box.inventory* replace the IP address with the IP address of your ubuntu server machine
- Create a Twitter developer account and "app" for user authentication in the app
- Create an AWS (Amazon Web Services) account which will be used to store database backups in S3
- Use the *objective8_config_template* found in the */ops* directory and replace the empty strings with your credentials and save it for use in the next step. Take note of the file path.
- Create a */ops/roles/nginx/files/secure/* directory, and copy your SSL certificate and key files there, with the names *objective8.key* and *objective.crt*.

Run Ansible playbook:

  The following command will install necessary packages and configure them (it will take a few minutes).
  It will require choosing a database password and supplying your Amazon S3 credentials for automatically backing up (encrypted) the database to an Amazon S3 bucket: 
  ```
  ansible-playbook ops/digital_ocean_box_playbook.yml -i ops/digital_ocean_box.inventory --extra-vars "CONFIG_FILE_PATH={config file path from the previous step without the curly braces}"
  ```
  
####Deploy application to the server
Run:

  The following will copy the application to the server and start it running as a service on a docker container.
  Once complete you should be able to access the app at your IP address.

  ```
  chmod +x deploy_prod.sh
  REMOTE_USER={username on server} SERVER_IP={IP address of server} ./deploy_prod.sh
  ```

## Docker

To run the application you'll need the following containers:

* Postgresql
* Nginx
* Objective8

First, open the *objective8_docker_config_template* found in the */ops* directory, enter your credentials and save the file. This will be used in both postgres and objective8.

#### Postgres Container

To start a postgres docker container, using the relative path for your config file.

    docker run -d --env-file=<docker config relative file path> -v /data --name pg_objective8 postgres

#### Nginx container

Nginx is used to add SSL protection and act as a reverse proxy. to use it you need:

* an SSL certificate and key
* a dhparam.pem file
* an nginx.conf file

You can acquire an SSL certificate and key online inexpensively. You should receive a pair of files, for instance objective8.crt and objective8.key. Store them in their own directory somewhere safe.

You can generate a dhparam.pem file by running: 
    
    openssl dhparam -rand – 2048 > dhparam.pem
 
You can create an nginx.conf file by copying the following into a new file and replacing the <> appropriately:

    events {
    }
    http {
      server {
      listen 80;
      return 301 $request_uri;
      }
      
      
      server {
        listen 443 ssl;
      
        ssl_certificate /etc/nginx/ssl/<file name for SSL certificate>;
        ssl_certificate_key /etc/nginx/ssl/<file name for SSL key>;
      
        ssl_session_cache shared:SSL:32m;
        ssl_session_timeout 10m;
      
        ssl_dhparam /etc/nginx/cert/dhparam.pem;
        ssl_protocols TLSv1.2 TLSv1.1 TLSv1;
      
        # trailing '/' after port means app doesn't know about '/as2/'
        location /as2/ {
          proxy_pass http://<docker ip>:7000/;
        }
        # no trailing '/' after port means app knows about '/api/v1/'
        location /api/v1/ {
          proxy_pass http://<docker ip>:8081;
        }
        location / {
          proxy_pass http://<docker ip>:8080;
        }
      }
    }


Finally, run the following command:

    docker run -v <absolute path to SSL certificates and keys directory>:/etc/nginx/ssl -v <absolute path to conf file>/nginx.conf:/etc/nginx/nginx.conf -v <absolute path to dhparam file>/dhparam.pem:/etc/nginx/cert/dhparam.pem -P -d --name nginx-conainer nginx
        
#### Objective8

Then, build and start the objective8 docker container.

```
docker build -t objective8 .
docker run -d --env-file=<relative path to objective8 docker config> -v <absolute path to project location>/objective8/migrations:/usr/src/app/target/migrations -p 8080:8080 -p 8081:8081 --link pg_objective8:postgres --name objective8 objective8
```
