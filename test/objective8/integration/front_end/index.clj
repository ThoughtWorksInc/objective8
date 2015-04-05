(ns objective8.integration.front-end.index 
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [objective8.utils :as utils]
            [objective8.integration.integration-helpers :as helpers]))

(def untranslated-string-regex #"(?!!DOCTYPE|!IEMobile)!\w+")

(facts "about rendering index page"
       (future-fact "there are no untranslated strings"
             (let [user-session (helpers/test-context)
                   peridot-response-body (-> user-session
                                             (p/request (utils/path-for :fe/index))
                                             :response
                                             :body)]
               (prn peridot-response-body)
               (re-seq untranslated-string-regex peridot-response-body) => empty?)))

(facts "about rendering learn-more page"
       (future-fact "there are no untranslated strings"
             (let [user-session (helpers/test-context)
                   peridot-response-body (-> user-session
                                             (p/request (utils/path-for :fe/learn-more))
                                             :response
                                             :body)]
               (prn peridot-response-body)
               (re-seq untranslated-string-regex peridot-response-body) => empty?)))  
