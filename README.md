# d-cent prototype

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

###### Running the app with credentials
create a task (for example `start_with_credentials.sh` with the following content:

```
API_BEARER_NAME=<choose a bearer name>
API_BEARER_TOKEN=<choose a secure bearer token>
TWITTER_CONSUMER_TOKEN=<obtain this from twitter when registering the application to allow sign-in via twitter> \
TWITTER_CONSUMER_SECRET_TOKEN=<as above> \
BASE_URI=<either localhost or VM ip address and :APP_PORT> \
APP_PORT= <> \
DB_PORT= <> \
lein repl $*
```
then run the task and start the app using:
```
(reset :default)
```

##Deployment

To deploy, you need to set some environment variables:
```
export APP_PORT=<port on which the applicaton will be served, defaults to 8080>
export BASE_URI=<the base uri at which the application is served, including the port, defaults to 'localhost:8080'>
export TWITTER_CONSUMER_TOKEN=<obtain this from twitter when registering the application to allow sign-in via twitter>
export TWITTER_CONSUMER_SECRET_TOKEN=<as above>
```
## Docker

With root privileges:
```
docker build -t objective8 .
docker run -it -p 8080:8080 --rm --name objective8-live objective8
```
