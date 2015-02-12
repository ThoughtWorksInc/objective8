(ns objective8.comments-test
  (:require [midje.sweet :refer :all]
            [cemerick.friend :as friend]
            [objective8.comments :as comments]
            [objective8.storage.storage :as storage]))

(def USER_ID 1)
(def OBJECTIVE_ID 234)

(fact "creates a comment from a request"
      (against-background
        (friend/current-authentication) => {:username USER_ID})
      (let [comment (comments/request->comment {:params {:comment "the comment"
                                                         :objective-id (str OBJECTIVE_ID)}})]
           comment => {:comment "the comment"
                       :objective-id OBJECTIVE_ID
                       :created-by-id USER_ID}))

(fact "By default, only the first 50 comments are retrieved"
      (comments/retrieve-comments OBJECTIVE_ID) => anything
      (provided
       (storage/pg-retrieve {:entity :comment
                             :objective-id OBJECTIVE_ID}
                            {:limit 50}) => []))
