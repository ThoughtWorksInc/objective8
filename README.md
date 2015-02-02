# d-cent prototype

## Getting Started
There is setup for a local development environment in the `ops` directory.
To start the development VM you will need to install

- Vagrant + Virtualbox (see https://www.vagrantup.com/downloads.html)
- Ansible (see http://docs.ansible.com/intro_installation.html)

To get started:

```
git clone git@github.com:ThoughtWorksInc/d-cent.git
cd d-cent/ops/
vagrant up
# type 'vagrant' when asked for a sudoers password
vagrant ssh
```

## Deployment

To deploy, you need to set some environment variables:

> export PORT=<port on which the applicaton will be served, defaults to 8080>
> export BASE_URI=<the base uri at which the application is served, defaults to 'localhost'>
> export TWITTER_CONSUMER_TOKEN=<obtain this from twitter when registering the application to allow sign-in via twitter>
> export TWITTER_CONSUMER_SECRET_TOKEN=<as above>

## Docker

With root privileges:
> docker build -t d-cent .
> docker run -it -p 8080:8080 --rm --name d-cent-live d-cent
