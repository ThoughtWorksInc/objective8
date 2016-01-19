FROM dcent/clojure-with-npm:0

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

COPY project.clj /usr/src/app/
RUN lein with-profile production deps
COPY . /usr/src/app
RUN apt-get -y install build-essential
RUN npm config set jobs 1
RUN npm install jade
RUN npm install grunt
RUN npm install jshint-stylish
RUN npm install load-grunt-tasks
RUN npm install node-sass
RUN npm install
RUN npm install -g grunt
RUN npm install -g grunt-cli
RUN npm rebuild node-sass
RUN grunt build

RUN lein uberjar
WORKDIR /usr/src/app/target

CMD java -jar objective8-0.0.1-SNAPSHOT-standalone.jar
