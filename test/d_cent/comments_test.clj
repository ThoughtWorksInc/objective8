(ns d-cent.comments-test
  (:require [midje.sweet :refer :all]
            [cemerick.friend :as friend]
            [d-cent.storage.storage :as storage]
            [d-cent.utils :as utils]
            [d-cent.comments :refer :all]))


(def test-comment {:comment "the comment"
                   :objective-id "OBJECTIVE_GUID"})


(fact "creates a comment from a request"
      (against-background
        (friend/current-authentication) => {:username "username"})
        (let [comment (request->comment test-comment)]
          (:user-id comment) => "username"))
