(ns objective8.storage.uris
  (:require [bidi.bidi :as bidi]))

(def uri-routes
  ["/" {["objectives/" [#"\d+" :objective-id]] {"" :objective
                                                ["/questions/" [#"\d+" :question-id] "/answers/" [#"\d+" :answer-id]] :answer
                                                ["/drafts/" [#"\d+" :draft-id]] :draft}
        ["comments/" [#"\d+" :comment-id]] :comment}])

(defn uri->query [uri]
  (if-let [{entity :handler route-params :route-params} (bidi/match-route uri-routes uri)]
    (cond
      (= entity :objective) {:entity :objective
                             :_id (Integer/parseInt (:objective-id route-params))}

      (= entity :draft) {:entity :draft
                         :_id (Integer/parseInt (:draft-id route-params))}

      (= entity :answer) {:entity :answer
                          :_id (Integer/parseInt (:answer-id route-params))}

      (= entity :comment) {:entity :comment
                           :_id (Integer/parseInt (:comment-id route-params))})))
