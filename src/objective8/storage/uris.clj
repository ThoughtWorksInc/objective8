(ns objective8.storage.uris
  (:require [bidi.bidi :as bidi]))

(def uri-routes
  ["/" {["objectives/" [#"\d+" :objective-id]] {"" :objective
                                                ["/questions/" [#"\d+" :question-id]] {"" :question
                                                                                       ["/answers/" [#"\d+" :answer-id]] :answer}
                                                ["/drafts/" [#"\d+" :draft-id]] {"" :draft
                                                                                 "/sections" {"" :sections 
                                                                                              ["/" [#"[0-9a-f]{8}" :section-label]] :section}}}
        ["users/" [#"\d+" :user-id]] :user
        ["comments/" [#"\d+" :comment-id]] :comment
        ["meta/stars/" [#"\d+" :star-id]] :star}])

(defn uri->query [uri]
  (when (string? uri)
    (if-let [{entity :handler route-params :route-params} (bidi/match-route uri-routes uri)]
      (case entity
        :objective {:entity :objective
                    :_id (Integer/parseInt (:objective-id route-params))}

        :draft {:entity :draft
                :_id (Integer/parseInt (:draft-id route-params))
                :objective-id (Integer/parseInt (:objective-id route-params))}

        :section {:entity :section
                  :draft-id (Integer/parseInt (:draft-id route-params))
                  :section-label (:section-label route-params)}

        :sections {:entity :section
                   :draft-id (Integer/parseInt (:draft-id route-params))
                   :objective-id (Integer/parseInt (:objective-id route-params))}

        :question {:entity :question
                   :objective-id (Integer/parseInt (:objective-id route-params))
                   :_id (Integer/parseInt (:question-id route-params))}

        :answer {:entity :answer
                 :_id (Integer/parseInt (:answer-id route-params))}

        :comment {:entity :comment
                  :_id (Integer/parseInt (:comment-id route-params))}

        :user {:entity :user
               :_id (Integer/parseInt (:user-id route-params))}

        :star {:entity :star
               :_id (Integer/parseInt (:star-id route-params))}))))

(defn question->uri [question]
  (bidi/path-for uri-routes :question
                 :question-id (:_id question)
                 :objective-id (:objective-id question)))

(defn user-id->uri [user-id]
  (bidi/path-for uri-routes :user
                 :user-id user-id))

(defn uri->section-data [uri]
  (if-let [{entity :handler route-params :route-params} (bidi/match-route uri-routes uri)]
   (when (= entity :section)
     {:entity :section
      :objective-id (Integer/parseInt (:objective-id route-params))
      :draft-id (Integer/parseInt (:draft-id route-params))
      :section-label (:section-label route-params)})))
