(defproject d-cent "0.0.1-SNAPSHOT"
  :description "Cool new project to do things and stuff"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [bidi "1.12.0"]
                 [ring "1.3.2"]
                 [http-kit "2.1.16"]
                 [selmer "0.7.9"]]
  :main d-cent.core
  :profiles {:dev {:dependencies [[midje "1.5.1"]
                                  [javax.servlet/servlet-api "2.5"]]
                   :plugins [[lein-midje "3.1.3"]]}})
