FROM dcent/clojure-npm-grunt-gulp

COPY . /usr/src/app/
RUN lein with-profile production deps && \
    npm install && \
    npm rebuild node-sass

CMD grunt build && lein uberjar && \
    java -jar /usr/src/app/target/objective8-0.0.1-SNAPSHOT-standalone.jar
