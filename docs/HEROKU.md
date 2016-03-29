# Deployment to Heroku

### Heroku account setup:
- Create a heroku account
- Create new heroku app
- Install heroku tool-belt

### Setup Twitter API for app:
- create new app on www.dev.twitter.com
- get consumer API key

### Local heroku configuration:
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