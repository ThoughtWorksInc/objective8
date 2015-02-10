(ns objective8.api.comments-integration-tests
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [objective8.utils :as utils]
            [objective8.core :as core]
            [objective8.integration-helpers :as helpers]
            [objective8.comments :as comments]))

;; Testing from http request -> making correct calls within comments namespace
;; Mock or stub out 'comments' namespace

(def app (helpers/test-context))

(def the-comment {:comment "The comment"
                  :root-id 1
                  :parent-id 1234
                  :created-by-id 223})

(def stored-comment (assoc the-comment :_id 1))

(def the-comment-as-json "{\"comment\":\"The comment\",\"root-id\":1,\"parent-id\":1234,\"created-by-id\":223}")

(facts "about posting comments" :integration
       (fact "the posted comment is stored"
             (p/request app "/api/v1/comments"
                        :request-method :post
                        :content-type "application/json"
                        :body the-comment-as-json)

             => (helpers/check-json-body stored-comment)
             (provided
               (comments/store-comment! the-comment) => stored-comment))

       (fact "the http response indicates the location of the comment"

             (against-background
               (comments/store-comment! anything) => stored-comment)

             (let [result (p/request app "/api/v1/comments"
                                     :request-method :post
                                     :content-type "application/json"
                                     :body the-comment-as-json)
                   response (:response result)
                   headers (:headers response)]
               response => (contains {:status 201})
               headers => (contains {"Location" (contains "/api/v1/comments/1")}))))
