(ns objective8.api.writers-invite-integration-tests
  (:require [peridot.core :as p]
            [midje.sweet :refer :all]
            [objective8.integration-helpers :as helpers]
            [objective8.writers :as writers]
            [objective8.middleware :as m])) 


;; Testing from http request -> making correct calls within writers namespace
;; Mock or stub out 'writers' namespace

(def app (helpers/test-context))
(def OBJECTIVE_ID 1)
(def INVITED_BY_ID 2)
(def INVITED_WRITER_ID 3)
(def the-invited-writer {:writer-name "Mel"
                         :reason "She's cool"
                         :objective-id OBJECTIVE_ID
                         :invited-by-id INVITED_BY_ID })
(def stored-writer (assoc the-invited-writer :_id INVITED_WRITER_ID))
(def the-invited-writer-as-json (str "{\"writer-name\":\"Mel\",\"reason\":\"She's cool\",\"objective-id\":" OBJECTIVE_ID ",\"invited-by-id\":" INVITED_BY_ID "}"))

(facts "about inviting policy writers" :integration
       (against-background
         (m/valid-credentials? anything anything anything) => true)
       (fact "the invited writer is stored"
             (p/request app (str "/api/v1/objectives/" OBJECTIVE_ID "/writers/invited")
                        :request-method :post
                        :content-type "application/json"
                        :body the-invited-writer-as-json) => (helpers/check-json-body stored-writer)
             (provided
               (writers/store-invited-writer! the-invited-writer) => stored-writer))

       (fact "the http response indicates the location of the invited writer"
             (against-background
               (writers/store-invited-writer! anything) => stored-writer)

             (let [result (p/request app (str "/api/v1/objectives/" OBJECTIVE_ID 
                                              "/writers/invited")
                                     :request-method :post
                                     :content-type "application/json"
                                     :body the-invited-writer-as-json)
                   response (:response result)
                   headers (:headers response)]
               response => (contains {:status 201})
               headers => (contains {"Location" (contains 
                                                  (str "/api/v1/objectives/" OBJECTIVE_ID 
                                                       "/writers/invited/" INVITED_WRITER_ID))})))
       
       (fact "a 400 status is returned if a PSQLException is raised"
             (against-background
               (writers/store-invited-writer! anything) =throws=> (org.postgresql.util.PSQLException.
                                                                  (org.postgresql.util.ServerErrorMessage. "" 0)))
             (:response (p/request app (str "/api/v1/objectives/" OBJECTIVE_ID "/writers/invited")
                                   :request-method :post
                                   :content-type "application/json"
                                   :body the-invited-writer-as-json)) => (contains {:status 400})))
