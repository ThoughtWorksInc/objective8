FROM dcent/clojure-npm-grunt-gulp

COPY . ./
RUN lein with-profile production deps && \
    npm install && \
    npm rebuild node-sass

CMD grunt build && \
    lein uberjar && \
    cp target/objective8-0.0.1-SNAPSHOT-standalone.jar ./ && \
    java -jar objective8-0.0.1-SNAPSHOT-standalone.jar
