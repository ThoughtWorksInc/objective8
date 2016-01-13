FROM dcent/clojure-with-npm:0

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

COPY project.clj /usr/src/app/
RUN lein with-profile production deps
COPY . /usr/src/app
RUN apt-get -y install build-essential
RUN npm install

RUN lein uberjar
WORKDIR /usr/src/app/target

CMD java -jar objective8-0.0.1-SNAPSHOT-standalone.jar
