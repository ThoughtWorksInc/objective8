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

(def OBJECTIVE_ID 234)
(def USER_ID 1)

(def the-comment {:comment "The comment"
                  :objective-id OBJECTIVE_ID
                  :created-by-id USER_ID})

(def stored-comment (assoc the-comment :_id 1))
(def stored-comments (map #(assoc the-comment :_id %) (range 5)))

(def the-comment-as-json (str "{\"comment\":\"The comment\",\"objective-id\":" OBJECTIVE_ID ",\"created-by-id\":" USER_ID "}"))

(facts "about posting comments" :integration
       (fact "the posted comment is stored"
             (p/request app "/api/v1/comments"
                        :request-method :post
                        :content-type "application/json"
                        :body the-comment-as-json)

             => (helpers/check-json-body stored-comment)
             (provided
               (comments/store-comment! the-comment) => stored-comment))

       (fact "a 400 status is returned if a PSQLException returned"
          (against-background
               (comments/store-comment! anything) =throws=> (org.postgresql.util.PSQLException. (org.postgresql.util.ServerErrorMessage. "" 0)) )
               (:response (p/request app "/api/v1/comments"
                                     :request-method :post
                                     :content-type "application/json"
                                     :body the-comment-as-json)) => (contains {:status 400}))

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


(facts "about retrieving comments"
  (fact "for an objective ID"
      (p/request app "/api/v1/objectives/1/comments")
      => (helpers/check-json-body stored-comments)
      (provided
        (comments/retrieve-comments 1) => stored-comments)))