(ns d-cent.api.comments-integration-tests
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [d-cent.utils :as utils]
            [d-cent.core :as core]
            [d-cent.integration-helpers :as helpers]
            [d-cent.comments :as comments]))

;; Testing from http request -> making correct calls within comments namespace
;; Mock or stub out 'comments' namespace

(def test-db (atom {}))
(def app (helpers/test-context test-db))

(def the-comment {:comment "The comment"
                  :discussing-id 1
                  :parent-id 1234
                  :created-by-id 223})

(def stored-comment (assoc the-comment :_id 1))

(def the-comment-as-json "{\"comment\":\"The comment\",\"discussing-id\":\"1\",\"parent-id\":\"1234\",\"created-by-id\":\"223\"}")

(def stored-comment-as-json "{\"_id\":1,\"created-by-id\":223,\"discussing-id\":1,\"parent-id\":1234,\"comment\":\"The comment\"}")


(facts "about posting comments"
       (fact "the posted comment is stored"
             (p/request app "/api/v1/comments"
                        :request-method :post
                        :content-type "application/json"
                        :body the-comment-as-json)

             => (contains {:response (contains {:body (contains stored-comment-as-json)})})
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
