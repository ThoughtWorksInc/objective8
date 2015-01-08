(defproject d-cent "0.0.1-SNAPSHOT"
  :description "Cool new project to do things and stuff"
  :dependencies [[org.clojure/clojure "1.5.0"]
                 [bidi "1.12.0"]
                 [ring "1.3.2"]
                 [http-kit "2.1.16"]]
  :main main
  :profiles {:dev {:dependencies [[midje "1.5.1"]
                                  [javax.servlet/servlet-api "2.5"]]
                   :plugins [[lein-midje "3.1.3"]]}})
