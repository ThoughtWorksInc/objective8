# Configuration

The following environment variables can be passed to the application.

## Required:

- **API_BEARER_NAME** and **API_BEARER_TOKEN** - set these variables to any strings. They are used to connect to the API securely and should be kept secret.
 
## Optional:

- **BASE_URI** - your application URI or IP address. Defaults to localhost:8080
- **APP_PORT** - defaults to 8080
- **POSTGRES_DB** and **POSTGRES_USER** and **POSTGRES_PASSWORD** - set these variables to any strings. They are used to set up and connect to the postgres database. Defaults to objective8, objective8 and development


### Enter these credentials to add [Stonecutter](https://github.com/d-cent/stonecutter) as a login option

- **STONECUTTER_AUTH_URL** - the URL of the deployment
- **STONECUTTER_CLIENT_ID** and **STONECUTTER_CLIENT_SECRET** - the client ID and secret from the clients.yml file
- **STONECUTTER_NAME** - defaults to Stonecutter


### Enter the credentials from [here](https://apps.twitter.com/) to add Twitter as a login option

- **TWITTER_CONSUMER_TOKEN** - the Twitter App Consumer Key
- **TWITTER_CONSUMER_SECRET_TOKEN** - the Twitter App Consumer Secret


### Enter these credentials from [here](https://developers.facebook.com/apps/) to add Facebook as a login option

- **FB_CLIENT_ID** - the Facebook App ID
- **FB_CLIENT_SECRET** - the Facebook App Secret


### Enter these credentials to store all activities in an instance of [Coracle](https://github.com/d-cent/coracle) 
- **CORACLE_URI** - the URL of the deployment 
- **CORACLE_BEARER_TOKEN** - the bearer token of the deployment


- **ADMINS** - The auth IDs of the admins, separated by spaces. The format is twitter-<ID of Twitter account> or facebook-<ID of Facebook account>. e.g. "facebook-123456789 twitter-54321"
- **AWS_ACCESS_KEY** and **AWS_SECRET_KEY** - the Access Key ID and Secret Access Key from your AWS account. They are used to backup the database daily.
- **AWS_GPG_PASSPHRASE** - a passphrase for encrypting the database backups before they reach AWS S3. You will need this if you ever want to restore backups from a different machine so don't lose/forget it.
- **APP_NAME** - defaults to Objective[8]
- **SHOW_ALPHA_WARNINGS** - set to true if your deployment is a test version
- **GA_TRACKING_ID** - your Google Analytics tracking ID. Use this to monitor your deployment at https://analytics.google.com/
- **COOKIE_MESSAGE_ENABLED** - set to true to show a warning about the use of cookies. You will need this if your deployment is based in the EU or aimed at EU citizens and you have set the variable **GA_TRACKING_ID**.
