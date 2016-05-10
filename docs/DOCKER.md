# Docker

First, install [Docker](https://www.docker.com/).

To run the application you'll need the following containers:

* Postgresql
* Objective8
* Nginx

First, open the *objective8_docker_config_template* found in the */ops* directory, enter your credentials and save the file. This will be used in both postgres and objective8. 
You can find more information about the configuration variables [here](./CONFIG.md).

## Postgres Container

To start a postgres docker container, using the relative path for your config file.

    docker run -d --env-file=<relative path to objective8 docker config> -v /data --name pg_objective8 postgres

## Objective8

To start the Objective[8] container without any customisations, run the following command:

    docker run -d --env-file=<relative path to objective8 docker config> -p 8080:8080 -p 8081:8081 --link pg_objective8:postgres --restart=on-failure --name objective8 dcent/objective8
    
### Customisations

You can optionally customise the colour scheme, favicon, the Stonecutter icon on the sign-in page and the translations. The following command will start the container with all these customisations:

    docker run -d --env-file=<relative path to objective8 docker config> \
    -v <absolute path to colour scheme>:/usr/src/app/assets/scss/root/_theme.scss \
    -v <absolute path to custom favicon>:/usr/src/app/resources/public/favicon.ico \
    -v <absolute path to stonecutter sign-in icon>:/usr/src/app/resources/public/stonecutter-sign-in-icon.png \
    -v <absolute path to English custom translations>:/usr/src/app/resources/translations/custom-en.csv \
    -v <absolute path to Spanish custom translations>:/usr/src/app/resources/translations/custom-es.csv \
    -p 8080:8080 -p 8081:8081 --link pg_objective8:postgres --restart=on-failure --name objective8 dcent/objective8

You can omit any of the lines starting with -v if you are happy with the default settings.

#### Colour scheme

To start the container with a custom colour scheme, you will need a file that redefines the three colours:

    $color1: #007E84;
    $color2: #9C0F83;
    $color3: #ffbf47;
    
#### Translations

To use your own translations within the app, you need to create .csv files for each language you wish to change. Find the existing translations [here](../resources/translations/en.csv). 
You will need to copy over the first two values for the translation and then enter your own text after another comma. For example, to change the subtitle on the learn more page, take the relevant line from the *en.csv* file:

    learn-more,sub-title,What are the basics?

and save this into your csv file:

    learn-more,sub-title,Objectivating explained!


    
## Nginx container

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
      server_name <web address for site>;
      return 301 https://$server_name$request_uri;
      }
      
      
      server {
        listen 443 ssl;
        server_name <web address for site>;
      
        ssl_certificate /etc/nginx/ssl/<file name for SSL certificate>;
        ssl_certificate_key /etc/nginx/ssl/<file name for SSL key>;
      
        ssl_session_cache shared:SSL:32m;
        ssl_session_timeout 10m;
      
        ssl_dhparam /etc/nginx/cert/dhparam.pem;
        ssl_protocols TLSv1.2 TLSv1.1 TLSv1;
      
        # trailing '/' after port means app doesn't know about '/as2/'
        location /as2/ {
          proxy_pass http://objective8:7000/;
        }
        # no trailing '/' after port means app knows about '/api/v1/'
        location /api/v1/ {
          proxy_pass http://objective8:8081;
        }
        location / {
          proxy_pass http://objective8:8080;
        }
      }
    }


Finally, run the following command:

    docker run -v <absolute path to SSL certificates and keys directory>:/etc/nginx/ssl -v <absolute path to conf file>/nginx.conf:/etc/nginx/nginx.conf -v <absolute path to dhparam file>/dhparam.pem:/etc/nginx/cert/dhparam.pem --link objective8:objective8 -p 443:443 -p 80:80 -d --name nginx nginx
        
