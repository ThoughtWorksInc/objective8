(defproject d-cent "0.0.1-SNAPSHOT"
  :description "Cool new project to do things and stuff"
  :min-lein-version "2.0.0"
  :test-paths ["test" "integration"]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [http-kit "2.1.16"]
                 [org.clojure/tools.logging "0.3.1"]
                 [bidi "1.12.0"]
                 [ring "1.3.2"]
                 [org.apache.httpcomponents/httpclient "4.3.5"]
                 [com.cemerick/friend "0.2.1"]
                 [de.ubercode.clostache/clostache "1.4.0"]
                 [enlive "1.1.5"]
                 [com.taoensso/tower "3.0.2"]
                 [clj-oauth "1.5.1"]]
  :main d-cent.core
  :profiles {:dev {:dependencies [[midje "1.6.3"]
                                  [ring/ring-mock "0.2.0"]
                                  [javax.servlet/servlet-api "2.5"]
                                  [peridot "0.3.1"]]
                   :plugins [[lein-midje "3.1.3"]
                             [jonase/eastwood "0.2.1"]]}})
