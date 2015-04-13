(ns objective8.storage.uris
  (:require [bidi.bidi :as bidi]))

(def uri-routes
  ["/" {["objectives/" [#"\d+" :objective-id]] {"" :objective
                                                ["/questions/" [#"\d+" :question-id] "/answers/" [#"\d+" :answer-id]] :answer
                                                ["/drafts/" [#"\d+" :draft-id]] :draft}
        ["users/" [#"\d+" :user-id]] :user
        ["comments/" [#"\d+" :comment-id]] :comment
        ["meta/stars/" [#"\d+" :star-id]] :star}])

(defn uri->query [uri]
  (if-let [{entity :handler route-params :route-params} (bidi/match-route uri-routes uri)]
    (case entity
      :objective {:entity :objective
                  :_id (Integer/parseInt (:objective-id route-params))}

      :draft {:entity :draft
              :_id (Integer/parseInt (:draft-id route-params))}

      :answer {:entity :answer
               :_id (Integer/parseInt (:answer-id route-params))}

      :comment {:entity :comment
                :_id (Integer/parseInt (:comment-id route-params))}

      :user {:entity :user
             :_id (Integer/parseInt (:user-id route-params))}

      :star {:entity :star
             :_id (Integer/parseInt (:star-id route-params))})))
