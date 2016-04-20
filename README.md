# Objective[8]

[![Build Status](https://snap-ci.com/d-cent/objective8/branch/master/build_image)](https://snap-ci.com/d-cent/objective8/branch/master)

A D-CENT project: an application for crowd sourcing and drafting policy.

## Development VM

You can develop and run the application in a VM to ensure that the correct versions of Objective[8]'s dependencies
are installed. You will need [VirtualBox][], [Vagrant][] and [Ansible][] installed.

First, clone the repository.

Navigate to the ops/ directory of the project and run:

    vagrant up development
    
The first time this is run, it will provision and configure a new VM.

When the VM has started, access the virtual machine by running:

    vagrant ssh

The source folder will be located at `/var/objective8`.

After initial setup, navigate to the source directory and apply the migrations:

    cd /var/objective8
    grunt build
    lein ragtime migrate

[Vagrant]: https://www.vagrantup.com
[Ansible]: http://docs.ansible.com/ansible/intro_installation.html
[VirtualBox]: https://www.virtualbox.org/


### Running the tests

To run all tests, use this command:

```
lein test
```

Commands and aliases can be found in the project.clj file.

### Designing in the browser

This allows you to rapidly design.  You can create jade, modify the sass and js.  These will all be live reloaded.

```
grunt design
```

Open your browser at http://192.168.50.50:2345/_routes to access it from outside the vagrant box.

#### Adding or updating a translation resource

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

### Running the app

Add any environment variables you want to use to the ```start_app_vm.sh``` script.

To start the app, run:

    lein start

And then inside the REPL run this for a fake twitter sign-in:

```
(reset :stub-twitter)
```

or this for the normal sign-in options:

```
(reset :default)
```

## Deployment

To deploy using Docker, see [here](docs/DOCKER.md).

To deploy to an Ubuntu server using Ansible, see [here](docs/UBUNTU.md).

To deploy to Heroku, see [here](docs/HEROKU.md).
