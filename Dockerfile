FROM dcent/clojure-npm-grunt-gulp

COPY . /usr/src/app/
RUN lein with-profile production deps && \
    npm install && \
    npm rebuild node-sass && \
    grunt build && \
    lein uberjar

WORKDIR /usr/src/app/target

CMD java -jar objective8-0.0.1-SNAPSHOT-standalone.jar
