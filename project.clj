(def database-connection-url
  (or (System/getenv "DB_JDBC_URL") 
      "jdbc:postgresql://localhost/objective8?user=objective8&password=development"))

(defproject objective8 "0.0.1-SNAPSHOT"
  :description "Cool new project to do things and stuff"
  :min-lein-version "2.0.0"
  :test-paths ["test"] 
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [http-kit "2.1.16"]
                 [org.clojure/tools.logging "0.3.1"]
                 [bidi "1.12.0"]
                 [ring "1.3.2"]
                 [ring/ring-json "0.3.1"]
                 [ring/ring-anti-forgery "1.0.0"]
                 [ring/ring-headers "0.1.2"]
                 [org.apache.httpcomponents/httpclient "4.3.5"]
                 [xml-apis "1.4.01"]
                 [com.cemerick/friend "0.2.1" :exclusions [robert/hooke xml-apis]]
                 [de.ubercode.clostache/clostache "1.4.0"]
                 [enlive "1.1.5"]
                 [com.taoensso/tower "3.0.2"]
                 [clj-oauth "1.5.1"]
                 [cheshire "5.4.0"]
                 [clj-time "0.9.0"]
                 [korma "0.3.0"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [postgresql "9.3-1102.jdbc41"]
                 [ragtime "0.3.8"]
                 [environ "1.0.0"]
                 [endophile "0.1.2"]
                 [org.pegdown/pegdown "1.2.0"]
                 [clojure-csv/clojure-csv "2.0.1"]]
  :main objective8.core
  :plugins [[ragtime/ragtime.lein "0.3.8"]
            [lein-environ "1.0.0"]]
  :profiles {:dev {:source-paths ["dev"]
                   :repl-options {:init-ns user
                                  :timeout 120000}
                   :dependencies [[midje "1.6.3"]
                                  [ring/ring-mock "0.2.0"]
                                  [clj-webdriver "0.6.1" :exclusions [org.seleniumhq.selenium/selenium-java 
                                                                      org.seleniumhq.selenium/selenium-server
                                                                      org.seleniumhq.selenium/selenium-remote-driver
                                                                      xml-apis]] 
                                  [org.seleniumhq.selenium/selenium-server "2.45.0"]
                                  [org.seleniumhq.selenium/selenium-java "2.45.0"]
                                  [org.seleniumhq.selenium/selenium-remote-driver "2.45.0"]
                                  [http-kit.fake "0.2.1"]
                                  [javax.servlet/servlet-api "2.5"]
                                  [peridot "0.3.1"]
                                  [clj-logging-config "1.9.12"]]
                   :plugins [[lein-midje "3.1.3"]
                             [jonase/eastwood "0.2.1"]]
                   :ragtime {:migrations ragtime.sql.files/migrations
                             :database ~database-connection-url}
                   :aliases {"translation-template" ["run" "-m" "dev-helpers.translation/main"]}}
             :uberjar {:aot [objective8.core]}
             :build {:plugins [[org.clojars.strongh/lein-init-script "1.3.1"]]
                     :lis-opts {:name "objective8"
                                :redirect-output-to "/var/log/objective8d-init.log"
                                :jvm-opts ["-server"
                                           "-Xms256M"
                                           "-Xmx512M"
                                           "-XX:MaxPermSize=128M"]}}
             :heroku {:ragtime {:migrations ragtime.sql.files/migrations
                                :database ~database-connection-url}}})
