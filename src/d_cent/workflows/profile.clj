(ns d-cent.workflows.profile 
  (:require [cemerick.friend.workflows :as workflows]
            [cemerick.friend :as friend]
            [clojure.pprint :as pprint]
            [bidi.ring :refer [make-handler]]))

(def capture-profile-routes
  ["/" {"create-profile" :create-profile}])

(defn capture-profile-handler [request]
  (let [username (get-in request [:session :d-cent-user-id] )]
    (workflows/make-auth {:username username :roles #{:signed-in}}
                         {::friend/workflow :d-cent.workflows.profile/capture-profile-workflow})))

(def capture-profile-handlers
  {:create-profile capture-profile-handler})

(def capture-profile-workflow
  (make-handler capture-profile-routes capture-profile-handlers))
